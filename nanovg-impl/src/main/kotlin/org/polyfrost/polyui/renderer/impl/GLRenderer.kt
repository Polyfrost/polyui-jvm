package org.polyfrost.polyui.renderer.impl

import org.lwjgl.BufferUtils
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.opengl.ARBDrawInstanced.glDrawArraysInstancedARB
import org.lwjgl.opengl.ARBInstancedArrays.glVertexAttribDivisorARB
import org.lwjgl.opengl.GL21C.*
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.stb.STBImageWrite.stbi_write_png
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTPackContext
import org.lwjgl.stb.STBTTPackRange
import org.lwjgl.stb.STBTTPackedchar
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryUtil
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.Vec4
import org.polyfrost.polyui.utils.toDirectByteBuffer
import org.polyfrost.polyui.utils.toDirectByteBufferNT
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object GLRenderer : Renderer {
    private const val SCISSOR_MAX_DEPTH = 8

    private const val ATLAS_SIZE = 1024

    private var quadiVbo = 0
    private var atlas = 0

    // quad GL objects
    private const val QUAD_STRIDE = 4 + 4 + 4 + 4 // xywh, radii, rgba, uv
    private const val MAX_BATCH_QUAD = 256
    private var quadProgram = 0
    private var quadVbo = 0
    private val quadBuffer = BufferUtils.createFloatBuffer(MAX_BATCH_QUAD * QUAD_STRIDE)
    private var quadCount = 0

    private var uWindowQuad = 0
    private var uTransformQuad = 0
    private var aLocalQuad = 0
    private var iRectQuad = 0
    private var iRadiiQuad = 0
    private var iColorQuad = 0
    private var iUVRectQuad = 0

    // font GL objects
    private const val FONT_STRIDE = 4 + 4 + 4 // xywh, rgba, uv
    private const val MAX_BATCH_GLYPH = 512
    private var fontProgram = 0
    private var fontVbo = 0
    private var fontiVbo = 0
    private val glyphBuffer = BufferUtils.createFloatBuffer(MAX_BATCH_GLYPH * FONT_STRIDE)
    var glyphCount = 0
    private var uWindowFont = 0
    private var uTransformFont = 0
    private var aLocalFont = 0
    private var iRectFont = 0
    private var iColorFont = 0
    private var iUVRectFont = 0


    // Current batch state
    private val scissorBuffer = IntArray(SCISSOR_MAX_DEPTH * 4)
    private var curScissor = -1

    private var currentTex = 0
    private var currentFontTex = 0
    private var transform = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
    private val transformStack = ArrayDeque<FloatArray>()

    private var viewportWidth = 0f
    private var viewportHeight = 0f
    private var pixelRatio = 1f
    private var alphaCap = 1f
    private var flushNeeded = false

    val QUAD_FRAG = """
        #version 120
        
        uniform sampler2D uTex;
        
        varying vec2 vUV;
        varying vec2 vPos;      // pixel coords in rect space
        varying vec4 vRect;     // rect x, y, w, h
        varying vec4 vRadii;    // per-corner radii
        varying vec4 vColor;    // RGBA
        
        // Signed distance function for rounded box
        float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
            vec2 q = abs(p) - b;
        
            // Compute masks for quadrant selection
            float px = step(0.0, p.x); // 0 if p.x<0, 1 if p.x>=0
            float py = step(0.0, p.y); // 0 if p.y<0, 1 if p.y>=0
        
            // Select radius based on quadrant using mix
            float r0 = mix(r.w, r.x, py); // left side: bottom/ top
            float r1 = mix(r.z, r.y, py); // right side: bottom/ top
            float rx = mix(r0, r1, px);
        
            q += vec2(rx);
            return length(max(q, 0.0)) - rx;
        }
        
        void main() {
            vec2 center = vRect.xy + 0.5 * vRect.zw;
            vec2 halfSize = 0.5 * vRect.zw;
            vec2 p = vPos - center;
        
            float d = roundedBoxSDF(p, halfSize, vRadii);
        
            vec4 col = vColor;
            if (vUV.x >= 0.0) {     // textured if UV.x >= 0
                vec4 texColor = texture2D(uTex, vUV);
                col *= texColor;
            }
            float f = fwidth(d);
            gl_FragColor = vec4(col.rgb, col.a * (1.0 - smoothstep(-f, f, d)));
        }
    """.trimIndent()

    val QUAD_VERT = """
        #version 120
        
        attribute vec2 aLocal;
        attribute vec4 iRect;
        attribute vec4 iRadii;
        attribute vec4 iColor;
        attribute vec4 iUVRect;
        
        uniform mat3 uTransform = mat3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        );
        uniform vec2 uWindow;
        
        varying vec2 vPos;
        varying vec4 vRect;
        varying vec4 vRadii;
        varying vec4 vColor;
        varying vec2 vUV;
        
        void main() {
            // Position inside rect
            vec2 pos = iRect.xy + aLocal * iRect.zw;
            vec2 uv  = iUVRect.xy + aLocal * iUVRect.zw;
        
            vec3 transformed = uTransform * vec3(pos, 1.0);
        
            vec2 ndc = (transformed.xy / uWindow) * 2.0 - 1.0;
            ndc.y = -ndc.y;
        
            gl_Position = vec4(ndc, 0.0, 1.0);
        
            vPos   = pos;
            vRect  = iRect;
            vRadii = iRadii;
            vColor = iColor;
            vUV    = uv;
        }
    """.trimIndent()

    val FONT_VERT = """
        #version 120
        
        attribute vec2 aLocal;
        attribute vec4 iRect;
        attribute vec4 iUVRect;
        attribute vec4 iColor;
        uniform mat3 uTransform = mat3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        );
        uniform vec2 uWindow;
        varying vec2 vUV;
        varying vec4 vColor;
        
        void main() {
            vec2 pos = iRect.xy + vec2(aLocal.x * iRect.z, aLocal.y * iRect.w);
            vec3 transformed = uTransform * vec3(pos, 1.0);
        
            vec2 ndc = (transformed.xy / uWindow) * 2.0 - 1.0;
            ndc.y = -ndc.y;
        
            gl_Position = vec4(ndc, 0.0, 1.0);
            vUV = iUVRect.xy + aLocal * iUVRect.zw;
            vColor = iColor;
        }
    """.trimIndent()

    val FONT_FRAG = """
        uniform sampler2D uFontTex;
        varying vec2 vUV;
        varying vec4 vColor;

        void main() {
            float a = texture2D(uFontTex, vUV).a;
            gl_FragColor = vec4(vColor.rgb, vColor.a * a);
        }
    """.trimIndent()

    fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        if (shader == 0) throw RuntimeException("Failed to create shader")

        glShaderSource(shader, source)
        glCompileShader(shader)

        val status = glGetShaderi(shader, GL_COMPILE_STATUS)
        if (status == GL_FALSE) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("Shader compile failed!\n$log")
        }
        return shader
    }

    fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = glCreateProgram()
        if (program == 0) throw RuntimeException("Failed to create program")

        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)

        val status = glGetProgrami(program, GL_LINK_STATUS)
        if (status == GL_FALSE) {
            throw RuntimeException("Program link failed:\n" + glGetProgramInfoLog(program))
        }

        glValidateProgram(program)
        val valid = glGetProgrami(program, GL_VALIDATE_STATUS)
        if (valid == GL_FALSE) {
            throw RuntimeException("Program validation failed:\n" + glGetProgramInfoLog(program))
        }

        glDetachShader(program, vertexShader)
        glDetachShader(program, fragmentShader)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)

        return program
    }

    fun createProgram(vertexSource: String, fragmentSource: String) =
        linkProgram(compileShader(GL_VERTEX_SHADER, vertexSource), compileShader(GL_FRAGMENT_SHADER, fragmentSource))

    override fun init() {
        quadProgram = createProgram(QUAD_VERT, QUAD_FRAG)
        fontProgram = createProgram(FONT_VERT, FONT_FRAG)

//         check if instancing extension is available
        if (glGetString(GL_EXTENSIONS)?.contains("GL_ARB_instanced_arrays") != true) {
            throw RuntimeException("GL_ARB_instanced_arrays not supported")
        }

        val quadData = floatArrayOf(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )
        quadVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
        glBufferData(GL_ARRAY_BUFFER, quadData, GL_STATIC_DRAW)
        quadiVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, quadiVbo)
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH_QUAD * QUAD_STRIDE * 4L, GL_STREAM_DRAW)
        glBufferSubData(GL_ARRAY_BUFFER, 0, quadBuffer)

        uWindowQuad = glGetUniformLocation(quadProgram, "uWindow")
        uTransformQuad = glGetUniformLocation(quadProgram, "uTransform")
        aLocalQuad = glGetAttribLocation(quadProgram, "aLocal")
        iRectQuad = glGetAttribLocation(quadProgram, "iRect")
        iRadiiQuad = glGetAttribLocation(quadProgram, "iRadii")
        iColorQuad = glGetAttribLocation(quadProgram, "iColor")
        iUVRectQuad = glGetAttribLocation(quadProgram, "iUVRect")


        fontVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, fontVbo)
        glBufferData(GL_ARRAY_BUFFER, quadData, GL_STATIC_DRAW)
        fontiVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, fontiVbo)
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH_GLYPH * FONT_STRIDE * 4L, GL_STREAM_DRAW)
        glBufferSubData(GL_ARRAY_BUFFER, 0, glyphBuffer)

        uWindowFont = glGetUniformLocation(fontProgram, "uWindow")
        uTransformFont = glGetUniformLocation(fontProgram, "uTransform")
        iRectFont = glGetAttribLocation(fontProgram, "iRect")
        iColorFont = glGetAttribLocation(fontProgram, "iColor")
        iUVRectFont = glGetAttribLocation(fontProgram, "iUVRect")



        atlas = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, atlas)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        quadCount = 0
        quadBuffer.clear()
        glUseProgram(quadProgram)
        glUniform2f(uWindowQuad, width, height)
        glUseProgram(fontProgram)
        glUniform2f(uWindowFont, width, height)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        viewportWidth = width * pixelRatio
        viewportHeight = height * pixelRatio
        this.pixelRatio = pixelRatio
    }

    // Example rect wrapper for your Renderer interface
    override fun rect(
        x: Float, y: Float, width: Float, height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        if (quadCount > MAX_BATCH_QUAD) flushQuads()
        quadBuffer.put(x).put(y).put(width).put(height)
        quadBuffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        quadBuffer.put(color.r / 255f).put(color.g / 255f).put(color.b / 255f).put(color.alpha.coerceAtMost(alphaCap))
        quadBuffer.put(-1f).put(-1f).put(1f).put(1f) // -1f to indicate no texture
        quadCount++
    }

    override fun hollowRect(x: Float, y: Float, width: Float, height: Float, color: Color, lineWidth: Float, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {

    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {

    }

    override fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) {

    }

    override fun transformsWithPoint() = false

    // image: supply texture ID and uv transform if needed
    override fun image(
        image: PolyImage, x: Float, y: Float, width: Float, height: Float,
        colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float
    ) {
        if (quadCount > MAX_BATCH_QUAD) flushQuads()
        if (quadCount > 0 && currentTex != atlas) flushQuads()
        currentTex = atlas

        quadBuffer.put(x).put(y).put(width).put(height)
        quadBuffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        quadBuffer.put((colorMask shr 16 and 0xFF) / 255f)
            .put((colorMask shr 8 and 0xFF) / 255f)
            .put((colorMask and 0xFF) / 255f)
            .put(1f)
        // default full UV rect
        quadBuffer.put(image.uv.x).put(image.uv.y).put(image.uv.w).put(image.uv.h)
        quadCount++
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        return
//        flushQuads()
        val nx = (x * pixelRatio).toInt()
        val ny = (viewportHeight - ((y + height) * pixelRatio)).toInt()
        val nw = (width * pixelRatio).toInt()
        val nh = (height * pixelRatio).toInt()
        scissorBuffer[++curScissor] = nx
        scissorBuffer[++curScissor] = ny
        scissorBuffer[++curScissor] = nw
        scissorBuffer[++curScissor] = nh
//        glEnable(GL_SCISSOR_TEST)
        glScissor(nx, ny, nw, nh)
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
        return
        if (curScissor < 4) {
            pushScissor(x, y, width, height)
            return
        }
//        flushQuads()
        val px = scissorBuffer[curScissor - 4]
        val py = scissorBuffer[curScissor - 3]
        val pw = scissorBuffer[curScissor - 2]
        val ph = scissorBuffer[curScissor - 1]

        val nx = maxOf((x * pixelRatio).toInt(), px)
        val ny = maxOf((viewportHeight - ((y + height) * pixelRatio)).toInt(), py)
        val nright = minOf(((x + width) * pixelRatio).toInt(), px + pw)
        val nbottom = minOf(((y + height) * pixelRatio).toInt(), py + ph)
        val nwidth = maxOf(0, nright - nx)
        val nheight = maxOf(0, nbottom - ny)
//        glEnable(GL_SCISSOR_TEST)
        glScissor(nx, ny, nwidth, nheight)
    }

    override fun popScissor() {
        return
        if (curScissor < 4) {
            curScissor = -1
            flushQuads()
            glDisable(GL_SCISSOR_TEST)
            return
        }
        flushQuads()
        curScissor -= 4
        val x = scissorBuffer[curScissor]
        val y = scissorBuffer[curScissor + 1]
        val width = scissorBuffer[curScissor + 2]
        val height = scissorBuffer[curScissor + 3]
        glScissor(x, y, width, height)

    }

    fun flushQuads() {
        if (quadCount == 0) return
        quadBuffer.flip()
        glBindBuffer(GL_ARRAY_BUFFER, quadiVbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, quadBuffer)

        glUseProgram(quadProgram)
        glBindTexture(GL_TEXTURE_2D, currentTex)

        // Quad attrib
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
        glEnableVertexAttribArray(aLocalQuad)
        glVertexAttribPointer(aLocalQuad, 2, GL_FLOAT, false, 0, 0L)

        // Instance attribs
        glBindBuffer(GL_ARRAY_BUFFER, quadiVbo)

        var qOffset = 0L
        qOffset = enableAttrib(iRectQuad, 4, QUAD_STRIDE, qOffset)
        qOffset = enableAttrib(iRadiiQuad, 4, QUAD_STRIDE, qOffset)
        qOffset = enableAttrib(iColorQuad, 4, QUAD_STRIDE, qOffset)
        enableAttrib(iUVRectQuad, 4, QUAD_STRIDE, qOffset)

        // Draw all instances
        glDrawArraysInstancedARB(GL_TRIANGLE_FAN, 0, 4, quadCount)

        quadCount = 0
        quadBuffer.clear()
    }

    fun flushGlyphs() {
        if (glyphCount == 0) return
        glyphBuffer.flip()
        glBindBuffer(GL_ARRAY_BUFFER, fontiVbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, glyphBuffer)
        glUseProgram(fontProgram)
        glBindTexture(GL_TEXTURE_2D, currentFontTex)
        glBindBuffer(GL_ARRAY_BUFFER, fontVbo)
        glEnableVertexAttribArray(aLocalFont)
        glVertexAttribPointer(aLocalFont, 2, GL_FLOAT, false, 0, 0L)

        glBindBuffer(GL_ARRAY_BUFFER, fontiVbo)
        var fOffset = 0L
        fOffset = enableAttrib(iRectFont, 4, FONT_STRIDE, fOffset)
        fOffset = enableAttrib(iColorFont, 4, FONT_STRIDE, fOffset)
        enableAttrib(iUVRectFont, 4, FONT_STRIDE, fOffset)
        glDrawArraysInstancedARB(GL_TRIANGLE_FAN, 0, 4, glyphCount)
        glyphCount = 0
        glyphBuffer.clear()
    }

    fun enableAttrib(loc: Int, size: Int, stride: Int, offset: Long): Long {
        glEnableVertexAttribArray(loc)
        glVertexAttribPointer(loc, size, GL_FLOAT, false, stride * 4, offset)
        glVertexAttribDivisorARB(loc, 1)
        return offset + size * 4L
    }

    override fun endFrame() {
        flushQuads()
        flushGlyphs()
//        glBindTexture(GL_TEXTURE_2D, 0)
//        glUseProgram(0)
    }

    override fun cleanup() {
        if (quadProgram != 0) glDeleteProgram(quadProgram)
        if (quadVbo != 0) glDeleteBuffers(quadVbo)
        if (quadiVbo != 0) glDeleteBuffers(quadiVbo)
        if (fontProgram != 0) glDeleteProgram(fontProgram)
        if (fontVbo != 0) glDeleteBuffers(fontVbo)
        if (fontiVbo != 0) glDeleteBuffers(fontiVbo)
        if (atlas != 0) glDeleteTextures(atlas)
        // todo cleanup fonts
        quadCount = 0
        quadBuffer.clear()
    }

    // --- stubbed Renderer methods ---
    override fun globalAlpha(alpha: Float) {
        alphaCap = alpha
    }

    override fun resetGlobalAlpha() = globalAlpha(1f)
    override fun translate(x: Float, y: Float) {
        flushQuads()
        transform[6] += transform[0] * x + transform[3] * y
        transform[7] += transform[1] * x + transform[4] * y
        flushNeeded = true
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        flushQuads()
        transform[0] *= sx; transform[1] *= sx
        transform[3] *= sy; transform[4] *= sy
        flushNeeded = true
    }

    override fun rotate(angleRadians: Double, px: Float, py: Float) {
        flushQuads()
        val c = cos(angleRadians).toFloat()
        val s = sin(angleRadians).toFloat()
        val a00 = transform[0]
        val a01 = transform[1]
        val a10 = transform[3]
        val a11 = transform[4]
        transform[0] = a00 * c + a10 * s
        transform[1] = a01 * c + a11 * s
        transform[3] = a00 * -s + a10 * c
        transform[4] = a01 * -s + a11 * c
        flushNeeded = true
    }

    override fun skewX(angleRadians: Double, px: Float, py: Float) {
        flushQuads()
        val t = tan(angleRadians).toFloat()
        val a00 = transform[0]
        val a01 = transform[1]
        val a10 = transform[3]
        val a11 = transform[4]
        transform[0] = a00 + a10 * t
        transform[1] = a01 + a11 * t
        flushNeeded = true

    }

    override fun skewY(angleRadians: Double, px: Float, py: Float) {
        flushQuads()
        val t = tan(angleRadians).toFloat()
        val a00 = transform[0]
        val a01 = transform[1]
        val a10 = transform[3]
        val a11 = transform[4]
        transform[3] = a10 + a00 * t
        transform[4] = a11 + a01 * t
        flushNeeded = true
    }

    private fun uploadTransform() {
        glUniformMatrix3fv(uTransformQuad, false, transform)
        glUniformMatrix3fv(uTransformFont, false, transform)
    }

    fun FloatArray.isIdentity(): Boolean {
        return this[0] == 1f && this[1] == 0f && this[2] == 0f &&
                this[3] == 0f && this[4] == 1f && this[5] == 0f &&
                this[6] == 0f && this[7] == 0f && this[8] == 1f
    }

    fun identity() {
        val transform = transform
        transform[0] = 1f; transform[1] = 0f; transform[2] = 0f
        transform[3] = 0f; transform[4] = 1f; transform[5] = 0f
        transform[6] = 0f; transform[7] = 0f; transform[8] = 1f
        this.transform = transform
    }

    override fun pop() {
        if (flushNeeded) {
            uploadTransform()
            flushQuads()
            flushNeeded = false
        }
        if (transform.isIdentity()) return
        if (transformStack.isEmpty()) {
            identity()
        } else transform = transformStack.removeLast()
        uploadTransform()
    }

    override fun push() {
        if (transform.isIdentity()) return
        transformStack.addLast(transform.copyOf())
    }

    private val fonts = HashMap<Font, HashMap<Float, FontAtlas>>()

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {
        val atlas = getFontAtlas(font, fontSize)
        if (currentFontTex != atlas.texture) {
            flushGlyphs()
            currentFontTex = atlas.texture
        }
        var penX = x
        val y = y + atlas.ascent + atlas.descent
        val r = (color.r / 255f)
        val g = (color.g / 255f)
        val b = (color.b / 255f)
        val a = (color.alpha.coerceAtMost(alphaCap))
        for (c in text) {
            if (glyphCount >= MAX_BATCH_GLYPH) {
                flushGlyphs()
            }
            val glyph = atlas.glyphs[c] ?: continue
            glyphBuffer.put(penX + glyph.xoff).put(y + glyph.yoff).put(glyph.width).put(glyph.height)
            glyphBuffer.put(r).put(g).put(b).put(a)
            glyphBuffer.put(glyph.u).put(glyph.v).put(glyph.uw).put(glyph.vh)
            penX += glyph.xadvance
            glyphCount++
        }
    }

    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        return getFontAtlas(font, fontSize).measure(text)
    }

    fun getFontAtlas(font: Font, fontSize: Float): FontAtlas {
        return fonts.getOrPut(font) { HashMap() }.getOrPut(fontSize) {
            val data = font.load().toDirectByteBuffer()
            FontAtlas(data, fontSize)
        }
    }

    private var slotX = 0
    private var slotY = 0

    override fun initImage(image: PolyImage, size: Vec2) {

        val w = IntArray(1)
        val h = IntArray(1)
        val d = initImage(image, w, h)

        glBindTexture(GL_TEXTURE_2D, atlas)
        glTexSubImage2D(GL_TEXTURE_2D, 0, slotX, slotY, w[0], h[0], GL_RGBA, GL_UNSIGNED_BYTE, d)
        glBindTexture(GL_TEXTURE_2D, 0)

        // Store UV rect for this image
        image.uv = Vec4.of(
            slotX / ATLAS_SIZE.toFloat(),
            slotY / ATLAS_SIZE.toFloat(),
            w[0] / ATLAS_SIZE.toFloat(),
            h[0] / ATLAS_SIZE.toFloat()
        )
        slotX += w[0]
        if (slotX + w[0] >= ATLAS_SIZE) {
            slotX = 0
            slotY += h[0]
            if (slotY >= ATLAS_SIZE) {
                throw IllegalStateException("Texture atlas full!")
            }
        }
    }

    private val PIXELS: ByteBuffer = MemoryUtil.memAlloc(3).put(112).put(120).put(0).flip() as ByteBuffer

    fun initImage(image: PolyImage, w: IntArray, h: IntArray): ByteBuffer {
        if (image.type == PolyImage.Type.Vector) {
            val nsvg = nsvgCreateRasterizer()
            val svg = nsvgParse(image.load().toDirectByteBufferNT(), PIXELS, 96f)
                ?: throw IllegalStateException("Could not parse SVG image ${image.resourcePath}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(svg.width(), svg.height()))
            w[0] = svg.width().toInt()
            h[0] = svg.height().toInt()
            val dst = BufferUtils.createByteBuffer(w[0] * h[0] * 4)
            nsvgRasterize(nsvg, svg, 0f, 0f, 1f, dst, w[0], h[0], w[0] * 4)
            nsvgDeleteRasterizer(nsvg)
            nsvgDelete(svg)
            return dst
        } else {
            val data = image.load().toDirectByteBuffer()
            val d = stbi_load_from_memory(data, w, h, IntArray(1), 4) ?: throw IllegalStateException("Failed to load image ${image.resourcePath}: ${stbi_failure_reason()}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(w[0].toFloat(), h[0].toFloat()))
            return d
        }
    }

    fun debugWriteAtlas(texId: Int) {
        val buf = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4)
        glBindTexture(GL_TEXTURE_2D, texId)
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)
        glBindTexture(GL_TEXTURE_2D, 0)
        stbi_write_png("debug_atlas$texId.png", ATLAS_SIZE, ATLAS_SIZE, 4, buf, ATLAS_SIZE * 4)
    }

    override fun delete(font: Font?) {}
    override fun delete(image: PolyImage?) {}

    override fun close() {
        cleanup()
    }

    class FontAtlas(data: ByteBuffer, val fontSize: Float) {
        val texture: Int
        val glyphs = HashMap<Char, Glyph>()
        val ascent: Float
        val descent: Float
        val lineGap: Float

        init {
            val stbFont = STBTTFontinfo.create()
            if (!stbtt_InitFont(stbFont, data)) {
                throw IllegalStateException("Failed to initialize font")
            }
            val scale = stbtt_ScaleForMappingEmToPixels(stbFont, fontSize)
            val asc = IntArray(1)
            val des = IntArray(1)
            val gap = IntArray(1)
            stbtt_GetFontVMetrics(stbFont, asc, des, gap)
            val pixelHeight = (asc[0] - des[0]) * scale
            ascent = asc[0] * scale
            descent = des[0] * scale
            lineGap = gap[0] * scale

            val range = STBTTPackRange.create()
            range.font_size(fontSize)
            range.first_unicode_codepoint_in_range(32)
            range.num_chars(95)
            val packed = STBTTPackedchar.create(range.num_chars())
            range.chardata_for_range(packed)

            val atlasSize = (pixelHeight.toInt() * pixelHeight.toInt()) / 2
            val atlasSizef = atlasSize.toFloat()

            val bitMap = BufferUtils.createByteBuffer(atlasSize * atlasSize)
            val pack = STBTTPackContext.create()
            if(!stbtt_PackBegin(pack, bitMap, atlasSize, atlasSize, 0, 1, 0L)) {
                throw IllegalStateException("Failed to initialize font packer")
            }

            if(!stbtt_PackFontRange(pack, data, 0, pixelHeight, range.first_unicode_codepoint_in_range(), packed)) {
                throw IllegalStateException("Failed to pack font range")
            }
            stbtt_PackEnd(pack)

            for (i in 0..<range.num_chars()) {
                val c = (i + 32).toChar()
                val g = packed.get(i)

                glyphs[c] = Glyph(
                    g.x0() / atlasSizef,
                    g.y0() / atlasSizef,
                    (g.x1() - g.x0()) / atlasSizef,
                    (g.y1() - g.y0()) / atlasSizef,
                    g.xoff(),
                    g.yoff(),
                    (g.x1() - g.x0()).toFloat(),
                    (g.y1() - g.y0()).toFloat(),
                    g.xadvance()
                )
            }

            texture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texture)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, atlasSize, atlasSize, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitMap)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        }

        fun measure(text: String): Vec2 {
            var width = 0f
//            var height = 0f
            for (c in text) {
                val g = glyphs[c] ?: continue
                width += g.xadvance
//                height = maxOf(height, g.height + g.offsetY)
            }
            return Vec2.of(width, fontSize)
        }
    }

    @JvmInline
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    value class Glyph(val data: FloatArray) {
        @kotlin.internal.InlineOnly
        inline val u get() = data[0]

        @kotlin.internal.InlineOnly
        inline val v get() = data[1]

        @kotlin.internal.InlineOnly
        inline val uw get() = data[2]

        @kotlin.internal.InlineOnly
        inline val vh get() = data[3]

        @kotlin.internal.InlineOnly
        inline val xoff get() = data[4]

        @kotlin.internal.InlineOnly
        inline val yoff get() = data[5]

        @kotlin.internal.InlineOnly
        inline val width get() = data[6]

        @kotlin.internal.InlineOnly
        inline val height get() = data[7]

        @kotlin.internal.InlineOnly
        inline val xadvance get() = data[8]

        constructor(
            uvX: Float, uvY: Float, uvW: Float, uvH: Float,
            offsetX: Float, offsetY: Float,
            width: Float, height: Float,
            advanceX: Float
        ) : this(floatArrayOf(uvX, uvY, uvW, uvH, offsetX, offsetY, width, height, advanceX))
    }
}
