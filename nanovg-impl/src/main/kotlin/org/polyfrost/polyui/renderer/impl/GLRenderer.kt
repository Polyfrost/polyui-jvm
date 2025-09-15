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
    private const val FONT_MAX_BITMAP_SIZE = 512
    private const val ATLAS_SIZE = 2048
    private const val ATLAS_SIZE_F = ATLAS_SIZE.toFloat()
    private const val ATLAS_UPSCALE_FACTOR = 2f
    private const val STRIDE = 4 + 4 + 4 + 4 + 1 // xywh, radii, rgba, uvwh, thick
    private const val MAX_BATCH = 1024
    private val PIXELS: ByteBuffer = MemoryUtil.memAlloc(3).put(112).put(120).put(0).flip() as ByteBuffer


    private val buffer = BufferUtils.createFloatBuffer(MAX_BATCH * STRIDE)
    private val scissorStack = IntArray(SCISSOR_MAX_DEPTH * 4)
    private val transformStack = ArrayDeque<FloatArray>()
    private val fonts = HashMap<Font, HashMap<Float, FontAtlas>>()

    // GL objects
    private var instancedVbo = 0
    private var atlas = 0
    private var program = 0
    private var vbo = 0


    private var uWindow = 0
    private var uTransform = 0
    private var aLocal = 0
    private var iRect = 0
    private var iRadii = 0
    private var iColor = 0
    private var iUVRect = 0
    private var iThickness = 0
    private var currentTex = 0


    // Current batch state
    private var curScissor = -1
    private var count = 0
    private var transform = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    private var pixelRatio = 1f
    private var alphaCap = 1f
    private var flushNeeded = false

    private var slotX = 0
    private var slotY = 0
    private var atlasRowHeight = 0

    val FRAG = """
        #version 120
        
        uniform sampler2D uTex;
        
        varying vec2 vUV;
        varying vec2 vPos;      // pixel coords in rect space
        varying vec4 vRect;     // rect x, y, w, h
        varying vec4 vRadii;    // per-corner radii
        varying vec4 vColor;    // RGBA
        varying float vThickness; // -1 for text, >0 for hollow rect
        
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
        
        float hollowRoundedBoxSDF(vec2 p, vec2 b, vec4 r, float thickness) {
            float dist = roundedBoxSDF(p, b, r);
            return max(dist, -(dist + thickness));
        }
        
        void main() {
            vec2 center = vRect.xy + 0.5 * vRect.zw;
            vec2 halfSize = 0.5 * vRect.zw;
            vec2 p = vPos - center;
        
            float d;
            if (vThickness > 0.0) {
                // Hollow outline: signed distance around the band
                d = hollowRoundedBoxSDF(p, halfSize, vRadii, vThickness);
            } else {
                // Filled rounded rect
                d = roundedBoxSDF(p, halfSize, vRadii);
            }
        
            vec4 col = vColor;
            if (vUV.x >= 0.0) {     // textured if UV.x >= 0
                vec4 texColor = texture2D(uTex, vUV);
                if (vThickness == -1.0) {
                    col = vec4(col.rgb, col.a * texColor.a);
                } else {
                    col *= texColor;
                }
            }
        
            // Proper antialiasing based on distance field
            float f = fwidth(d);
            float alpha = 1.0 - smoothstep(-f, f, d);
            if (alpha <= 0.0) discard;
        
            gl_FragColor = vec4(col.rgb, col.a * alpha);
        }
    """.trimIndent()

    val VERT = """
        #version 120
        
        attribute vec2 aLocal;
        attribute vec4 iRect;
        attribute vec4 iRadii;
        attribute vec4 iColor;
        attribute vec4 iUVRect;
        attribute float iThickness;
        
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
        varying float vThickness;
        
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
            vThickness = iThickness;
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
        program = createProgram(VERT, FRAG)

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
        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, quadData, GL_STATIC_DRAW)
        instancedVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH * STRIDE * 4L, GL_STREAM_DRAW)
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer)

        uWindow = glGetUniformLocation(program, "uWindow")
        uTransform = glGetUniformLocation(program, "uTransform")
        aLocal = glGetAttribLocation(program, "aLocal")
        iRect = glGetAttribLocation(program, "iRect")
        iRadii = glGetAttribLocation(program, "iRadii")
        iColor = glGetAttribLocation(program, "iColor")
        iUVRect = glGetAttribLocation(program, "iUVRect")
        iThickness = glGetAttribLocation(program, "iThickness")


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
        count = 0
        buffer.clear()
        glUseProgram(program)
        glUniform2f(uWindow, width, height)
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
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(color.r / 255f).put(color.g / 255f).put(color.b / 255f).put(color.alpha.coerceAtMost(alphaCap))
        buffer.put(-1f).put(-1f).put(1f).put(1f) // -1f to indicate no texture
        buffer.put(0f) // thickness = 0 for filled rect
        count++
    }

    override fun hollowRect(x: Float, y: Float, width: Float, height: Float, color: Color, lineWidth: Float, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(color.r / 255f).put(color.g / 255f).put(color.b / 255f).put(color.alpha.coerceAtMost(alphaCap))
        buffer.put(-1f).put(-1f).put(1f).put(1f) // -1f to indicate no texture
        buffer.put(lineWidth)
        count++
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {

    }

    override fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) {

    }

    // image: supply texture ID and uv transform if needed
    override fun image(
        image: PolyImage, x: Float, y: Float, width: Float, height: Float,
        colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float
    ) {
        if (count >= MAX_BATCH) flush()
        if (count > 0 && currentTex != atlas) flush()
        currentTex = atlas

        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put((colorMask shr 16 and 0xFF) / 255f)
            .put((colorMask shr 8 and 0xFF) / 255f)
            .put((colorMask and 0xFF) / 255f)
            .put(1f)
        // default full UV rect
        buffer.put(image.uv.x).put(image.uv.y).put(image.uv.w).put(image.uv.h)
        buffer.put(0f) // thickness = 0 for filled rect
        count++
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        return
//        flushQuads()
        val nx = (x * pixelRatio).toInt()
        val ny = (viewportHeight - ((y + height) * pixelRatio)).toInt()
        val nw = (width * pixelRatio).toInt()
        val nh = (height * pixelRatio).toInt()
        scissorStack[++curScissor] = nx
        scissorStack[++curScissor] = ny
        scissorStack[++curScissor] = nw
        scissorStack[++curScissor] = nh
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
        val px = scissorStack[curScissor - 4]
        val py = scissorStack[curScissor - 3]
        val pw = scissorStack[curScissor - 2]
        val ph = scissorStack[curScissor - 1]

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
            flush()
            glDisable(GL_SCISSOR_TEST)
            return
        }
        flush()
        curScissor -= 4
        val x = scissorStack[curScissor]
        val y = scissorStack[curScissor + 1]
        val width = scissorStack[curScissor + 2]
        val height = scissorStack[curScissor + 3]
        glScissor(x, y, width, height)

    }

    fun flush() {
        if (count == 0) return
        buffer.flip()
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer)

        glUseProgram(program)
        glBindTexture(GL_TEXTURE_2D, currentTex)

        // Quad attrib
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glEnableVertexAttribArray(aLocal)
        glVertexAttribPointer(aLocal, 2, GL_FLOAT, false, 0, 0L)

        // Instance attribs
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)

        var offset = 0L
        offset = enableAttrib(iRect, 4, STRIDE, offset)
        offset = enableAttrib(iRadii, 4, STRIDE, offset)
        offset = enableAttrib(iColor, 4, STRIDE, offset)
        offset = enableAttrib(iUVRect, 4, STRIDE, offset)
        enableAttrib(iThickness, 1, STRIDE, offset)

        // Draw all instances
        glDrawArraysInstancedARB(GL_TRIANGLE_FAN, 0, 4, count)

        count = 0
        buffer.clear()
    }

    fun enableAttrib(loc: Int, size: Int, stride: Int, offset: Long): Long {
        glEnableVertexAttribArray(loc)
        glVertexAttribPointer(loc, size, GL_FLOAT, false, stride * 4, offset)
        glVertexAttribDivisorARB(loc, 1)
        return offset + size * 4L
    }

    override fun transformsWithPoint() = false

    override fun endFrame() {
        flush()
//        glBindTexture(GL_TEXTURE_2D, 0)
//        glUseProgram(0)
    }

    override fun cleanup() {
        if (program != 0) glDeleteProgram(program)
        if (vbo != 0) glDeleteBuffers(vbo)
        if (instancedVbo != 0) glDeleteBuffers(instancedVbo)
        if (atlas != 0) glDeleteTextures(atlas)
        count = 0
        buffer.clear()
    }

    // --- stubbed Renderer methods ---
    override fun globalAlpha(alpha: Float) {
        alphaCap = alpha
    }

    override fun resetGlobalAlpha() = globalAlpha(1f)
    override fun translate(x: Float, y: Float) {
        flush()
        transform[6] += transform[0] * x + transform[3] * y
        transform[7] += transform[1] * x + transform[4] * y
        flushNeeded = true
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        flush()
        transform[0] *= sx; transform[1] *= sx
        transform[3] *= sy; transform[4] *= sy
        flushNeeded = true
    }

    override fun rotate(angleRadians: Double, px: Float, py: Float) {
        flush()
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
        flush()
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
        flush()
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
        glUniformMatrix3fv(uTransform, false, transform)
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
            flush()
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

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {
        val fAtlas = getFontAtlas(font, fontSize)
        if (count >= MAX_BATCH) flush()
        if (count > 0 && currentTex != atlas) flush()
        currentTex = atlas

        var penX = x
        val penY = y + fAtlas.ascent + fAtlas.descent
        val r = (color.r / 255f)
        val g = (color.g / 255f)
        val b = (color.b / 255f)
        val a = (color.alpha.coerceAtMost(alphaCap))
        for (c in text) {
            if (count >= MAX_BATCH) {
                flush()
            }
            val glyph = fAtlas.glyphs[c] ?: continue
            buffer.put(penX + glyph.xoff).put(penY + glyph.yoff).put(glyph.width).put(glyph.height)
            buffer.put(0f).put(0f).put(0f).put(0f) // zero radii
            buffer.put(r).put(g).put(b).put(a)
            buffer.put(glyph.u).put(glyph.v).put(glyph.uw).put(glyph.vh)
            buffer.put(-1f) // thickness = -1 for text
            penX += glyph.xadvance
            count++
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

    override fun initImage(image: PolyImage, size: Vec2) {
        val w = IntArray(1)
        val h = IntArray(1)
        val d = initImage(image, w, h)

        if (slotX + w[0] >= ATLAS_SIZE) {
            slotX = 0
            slotY += atlasRowHeight
            atlasRowHeight = 0
            if (slotY + h[0] >= ATLAS_SIZE) {
                throw IllegalStateException("Texture atlas full!")
            }
        }

        // Store UV rect for this image
        image.uv = Vec4.of(
            slotX / ATLAS_SIZE.toFloat(),
            slotY / ATLAS_SIZE.toFloat(),
            w[0] / ATLAS_SIZE.toFloat(),
            h[0] / ATLAS_SIZE.toFloat()
        )

        glBindTexture(GL_TEXTURE_2D, atlas)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexSubImage2D(GL_TEXTURE_2D, 0, slotX, slotY, w[0], h[0], GL_RGBA, GL_UNSIGNED_BYTE, d)
        glBindTexture(GL_TEXTURE_2D, 0)

        slotX += w[0]
        if (h[0] > atlasRowHeight) atlasRowHeight = h[0]
    }

    var rasterizer = 0L

    fun initImage(image: PolyImage, w: IntArray, h: IntArray): ByteBuffer {
        if (image.type == PolyImage.Type.Vector) {
            if (rasterizer == 0L) {
                rasterizer = nsvgCreateRasterizer()
                if (rasterizer == 0L) throw IllegalStateException("Could not create SVG rasterizer")
            }
            val nsvg = rasterizer
            val svg = nsvgParse(image.load().toDirectByteBufferNT(), PIXELS, 96f)
                ?: throw IllegalStateException("Could not parse SVG image ${image.resourcePath}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(svg.width(), svg.height()))
            w[0] = (svg.width() * ATLAS_UPSCALE_FACTOR).toInt()
            h[0] = (svg.height() * ATLAS_UPSCALE_FACTOR).toInt()
            val dst = BufferUtils.createByteBuffer(w[0] * h[0] * 4)
            nsvgRasterize(nsvg, svg, 0f, 0f, ATLAS_UPSCALE_FACTOR, dst, w[0], h[0], w[0] * 4)
            nsvgDelete(svg)
            return dst
        } else {
            val data = image.load().toDirectByteBuffer()
            val d = stbi_load_from_memory(data, w, h, IntArray(1), 4) ?: throw IllegalStateException("Failed to load image ${image.resourcePath}: ${stbi_failure_reason()}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(w[0].toFloat(), h[0].toFloat()))
            return d
        }
    }

    fun debugWriteAtlas(texId: Int = atlas) {
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
            val pixelHeight = (asc[0] - des[0]) * scale * ATLAS_UPSCALE_FACTOR
            ascent = asc[0] * scale
            descent = des[0] * scale
            lineGap = gap[0] * scale

            val range = STBTTPackRange.malloc()
            range.font_size(fontSize)
            range.first_unicode_codepoint_in_range(32)
            range.num_chars(95)
            val packed = STBTTPackedchar.malloc(range.num_chars())
            range.chardata_for_range(packed)

            val bitMap = BufferUtils.createByteBuffer(FONT_MAX_BITMAP_SIZE * FONT_MAX_BITMAP_SIZE)
            val pack = STBTTPackContext.malloc()
            if (!stbtt_PackBegin(pack, bitMap, FONT_MAX_BITMAP_SIZE, FONT_MAX_BITMAP_SIZE, 0, 1, 0L)) {
                throw IllegalStateException("Failed to initialize font packer")
            }

            if (!stbtt_PackFontRange(pack, data, 0, pixelHeight, range.first_unicode_codepoint_in_range(), packed)) {
                throw IllegalStateException("Failed to pack font range")
            }
            stbtt_PackEnd(pack)
            pack.free()

            var minX = Short.MAX_VALUE
            var minY = Short.MAX_VALUE
            var maxX = Short.MIN_VALUE
            var maxY = Short.MIN_VALUE
            for (i in 0..<range.num_chars()) {
                val g = packed.get(i)
                if (g.x0() < minX) minX = g.x0()
                if (g.y0() < minY) minY = g.y0()
                if (g.x1() > maxX) maxX = g.x1()
                if (g.y1() > maxY) maxY = g.y1()
            }
            val totalSizeX = maxX - minX
            val totalSizeY = maxY - minY

            if (slotX + totalSizeX > ATLAS_SIZE) {
                slotX = 0
                slotY += atlasRowHeight
                atlasRowHeight = 0
            }
            val sx = slotX
            val sy = slotY


            for (i in 0..<range.num_chars()) {
                val c = (i + 32).toChar()
                val g = packed.get(i)

                val glyph = Glyph(
                    (sx + g.x0()) / ATLAS_SIZE_F,
                    (sy + g.y0()) / ATLAS_SIZE_F,
                    (g.x1() - g.x0()) / ATLAS_SIZE_F,
                    (g.y1() - g.y0()) / ATLAS_SIZE_F,
                    g.xoff() / ATLAS_UPSCALE_FACTOR,
                    g.yoff() / ATLAS_UPSCALE_FACTOR,
                    (g.x1() - g.x0()).toFloat() / ATLAS_UPSCALE_FACTOR,
                    (g.y1() - g.y0()).toFloat() / ATLAS_UPSCALE_FACTOR,
                    g.xadvance() / ATLAS_UPSCALE_FACTOR
                )
                glyphs[c] = glyph
            }
            packed.free()
            range.free()

            glBindTexture(GL_TEXTURE_2D, atlas)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            glTexSubImage2D(GL_TEXTURE_2D, 0, sx, sy, FONT_MAX_BITMAP_SIZE, FONT_MAX_BITMAP_SIZE, GL_ALPHA, GL_UNSIGNED_BYTE, bitMap)
            slotX += totalSizeX
            atlasRowHeight = maxOf(atlasRowHeight, totalSizeY)
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
