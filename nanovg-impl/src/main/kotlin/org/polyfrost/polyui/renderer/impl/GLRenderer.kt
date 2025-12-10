package org.polyfrost.polyui.renderer.impl

import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.opengl.GL20C.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTPackContext
import org.lwjgl.stb.STBTTPackRange
import org.lwjgl.stb.STBTTPackedchar
import org.lwjgl.stb.STBTruetype.*
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.Vec4
import org.polyfrost.polyui.utils.toDirectByteBuffer
import org.polyfrost.polyui.utils.toDirectByteBufferNT
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
object GLRenderer : Renderer {
    private val LOGGER = LogManager.getLogger("PolyUI/GLRenderer")

    private const val MAX_UI_DEPTH = 16
    private const val FONT_MAX_BITMAP_W = 1024
    private const val FONT_MAX_BITMAP_H = 512
    private const val ATLAS_SIZE = 2048
    private const val ATLAS_SVG_UPSCALE_FACTOR = 4f
    private const val STRIDE = 4 + 4 + 1 + 1 + 4 + 1 // bounds, radii, color0, color1, UV, thick
    private const val MAX_BATCH = 1024

    private val PIXELS: ByteBuffer = BufferUtils.createByteBuffer(3).put(112).put(120).put(0).flip() as ByteBuffer
    private val EMPTY_ROW = floatArrayOf(0f, 0f, 0f, 0f)
    private val NO_UV = floatArrayOf(-1f, -1f, 1f, 1f)
    private val IDENTITY = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )


    private val buffer = BufferUtils.createFloatBuffer(MAX_BATCH * STRIDE)
    private val scissorStack = IntArray(MAX_UI_DEPTH * 4)
    private val transformStack = Array(MAX_UI_DEPTH) { FloatArray(9) }
    private val fonts = HashMap<Int, FontAtlas>()
    private val init get() = program != 0

    // GL objects
    private var instancedVbo = 0
    private var atlas = 0
    private var program = 0
    private var vao = 0 // GL3+ only
    private var quadVbo = 0
    private var nSvgRaster = 0L


    private var uWindow = 0
    private var uTransform = 0
    private var aLocal = 0
    private var iRect = 0
    private var iRadii = 0
    private var iColor0 = 0
    private var iColor1 = 0
    private var iUVRect = 0
    private var iThickness = 0


    // Current batch state
    private var count = 0
    private var scissorDepth = 0
    private var transformDepth = 0
    private var transform = FloatArray(9).set(IDENTITY)
    private val VIEWPORT = IntArray(4)
    private var pixelRatio = 1f
    private var alphaCap = 255
    private var popFlushNeeded = false

    // atlas data
    private var slotX = 0
    private var slotY = 0
    private var atlasRowHeight = 0

    private val FRAG = """
        #version $$$ // replaced by compileShader
        #if __VERSION__ >= 130
            #define VARYING in
            #define TEXTURE texture
            out vec4 fragColor;
        #else
            #define VARYING varying
            #define TEXTURE texture2D
            #define fragColor gl_FragColor
        #endif

        uniform sampler2D uTex;

        VARYING vec2 vUV;
        VARYING vec2 vUV2;      // used for gradients
        VARYING vec2 vPos;      // pixel coords in rect space
        VARYING vec4 vRect;     // rect x, y, w, h
        VARYING vec4 vRadii;    // per-corner radii
        VARYING vec4 vColor0;    // RGBA
        VARYING vec4 vColor1;    // RGBA (for gradients)
        VARYING float vThickness; // -1 for text, -2 for linear gradient, -3 for radial, -4 for box,  >0 for hollow rect

        // Signed distance function for rounded box
        float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
            // px = 1.0 if p.x > 0, else 0.0
            float px = step(0.0, p.x);
            float py = step(0.0, p.y);

            // Select radius per quadrant
            float rLeft  = mix(r.x, r.w, py); // top-left / bottom-left
            float rRight = mix(r.y, r.z, py); // top-right / bottom-right
            float radius = mix(rLeft, rRight, px); // left vs right

            vec2 q = abs(p) - (b - radius);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }

        float hollowRoundedBoxSDF(vec2 p, vec2 b, vec4 r, float thickness) {
            float outer = roundedBoxSDF(p, b + 0.3, r + 0.3);
            float inner = roundedBoxSDF(p, b - thickness, max(r - thickness, 0.0)); 
            return max(outer, -inner);
        }

        float roundBoxSDF(vec2 p, vec2 halfSize, float radius) {
            vec2 q = abs(p) - (halfSize - radius);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }

        void main() {
            vec2 halfSize = 0.5 * vRect.zw;
            vec2 center = vRect.xy + halfSize;
            vec2 p = vPos - center;

            float d = (vThickness > 0.0) ? hollowRoundedBoxSDF(p, halfSize, vRadii, vThickness) : roundedBoxSDF(p, halfSize, vRadii);

            vec4 col = vColor0;
            if (vUV.x >= 0.0 && vThickness >= -1.0) {     // textured if UV.x >= 0
                vec4 texColor = TEXTURE(uTex, vUV);
                // text check: use red channel as alpha
                col = (vThickness == -1.0) ? vec4(col.rgb, col.a * texColor.r) : col * texColor;
            }
            else if (vThickness == -2.0) { // linear gradient, vUV as start and vUV2 as end
                vec2 dir = vUV2 - vUV;
                float len = length(dir);
                float t = clamp(dot((p + halfSize) - vUV, dir / len) / len, 0.0, 1.0);
                col = mix(vColor0, vColor1, t);
            }
            else if (vThickness == -3.0) { // radial gradient, vUV as center and vUV2.x as radius
                float dist = length(p + halfSize - vUV);
                float t = clamp((dist - vUV2.x) / (vUV2.y - vUV2.x), 0.0, 1.0);
                col = mix(vColor0, vColor1, t);
            }
            else if (vThickness == -4.0) { // box gradient, vUV.x as radius and vUV.y as feather
                float dist = roundBoxSDF(p, halfSize, vUV.x);
                float t = clamp((dist + vUV.y * 0.5) / vUV.y, 0.0, 1.0);
                col = mix(vColor0, vColor1, t);
            }
            else if (vThickness == -5.0) { // drop shadow, vUV.x as spread and vUV.y as blur
                float dShadow = roundBoxSDF(p, halfSize + vUV.x, vRadii.x);
                col = vec4(vColor0.rgb, vColor0.a * (1.0 - smoothstep(-vUV.y, vUV.y, dShadow)));
            }

            // Proper antialiasing based on distance field
            float f = fwidth(d);
            float alpha = col.a * (1.0 - smoothstep(-f, f, d));

            fragColor = vec4(col.rgb * alpha, alpha);
        }
    """.trimIndent()

    private val VERT = """
        #version $$$ // replaced by compileShader
        #extension GL_EXT_gpu_shader4 : enable
        #if __VERSION__ >= 130
            #define ATTRIBUTE in
            #define VARYING out
            #define U_INT uint
        #else
            #define ATTRIBUTE attribute
            #define VARYING varying
            #define U_INT unsigned int
        #endif

        ATTRIBUTE vec2 aLocal;
        ATTRIBUTE vec4 iRect;
        ATTRIBUTE vec4 iRadii;
        ATTRIBUTE U_INT iColor0;
        ATTRIBUTE U_INT iColor1;
        ATTRIBUTE vec4 iUVRect;
        ATTRIBUTE float iThickness;

        uniform mat3 uTransform = mat3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        );
        uniform vec2 uWindow;

        VARYING vec2 vPos;
        VARYING vec4 vRect;
        VARYING vec4 vRadii;
        VARYING vec4 vColor0;
        VARYING vec4 vColor1;
        VARYING vec2 vUV;
        VARYING vec2 vUV2;
        VARYING float vThickness;
        
        vec4 unpackColor(U_INT c) {
            float a = float((c >> 24) & 0xFFu) / 255.0;
            float r = float((c >> 16) & 0xFFu) / 255.0;
            float g = float((c >>  8) & 0xFFu) / 255.0;
            float b = float((c      ) & 0xFFu) / 255.0;

            return vec4(r, g, b, a);
        }

        void main() {
            // Position inside rect
            vec2 pos = iRect.xy + aLocal * iRect.zw;
            vec2 uv  = (iThickness > -2.0) ? iUVRect.xy + aLocal * iUVRect.zw : iUVRect.xy; // for gradients, just pass through the first two param to frag

            vec3 transformed = uTransform * vec3(pos, 1.0);

            vec2 ndc = (transformed.xy / uWindow) * 2.0 - 1.0;
            ndc.y = -ndc.y;

            gl_Position = vec4(ndc, 0.0, 1.0);

            vPos    = pos;
            vRect   = iRect;
            vRadii  = iRadii;
            vColor0 = unpackColor(iColor0);
            vColor1 = unpackColor(iColor1);
            vUV     = uv;
            // pass through for gradients.
            vUV2    = iUVRect.zw;
            vThickness = iThickness;
        }
    """.trimIndent()

    private fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        if (shader == 0) throw RuntimeException("Failed to create shader")

        glShaderSource(shader, source.replaceFirst("$$$", if (caps().OpenGL30) "150" else "120"))
        glCompileShader(shader)

        val status = glGetShaderi(shader, GL_COMPILE_STATUS)
        if (status == GL_FALSE) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("${if (type == GL_FRAGMENT_SHADER) "Fragment" else "Vertex"} shader compile failed!\n$log")
        }
        return shader
    }

    private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
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

    private fun caps() = org.lwjgl.opengl.GL.getCapabilities()

    override fun init() {
        if (init) return
        // check if instancing extension is available
        require(caps().OpenGL20) { "At least OpenGL 2.0 is required" }
        require(caps().OpenGL30 || caps().GL_EXT_gpu_shader4) { "GL_EXT_gpu_shader4 is not supported and is required" }
        require(caps().OpenGL33 || caps().GL_ARB_instanced_arrays) { "GL_ARB_instanced_arrays is not supported and is required" }
        require(caps().OpenGL31 || caps().GL_ARB_draw_instanced) { "GL_ARB_draw_instanced is not supported and is required" }

        if (caps().OpenGL30) {
            // ...ok i guess this is needed
            vao = org.lwjgl.opengl.GL30C.glGenVertexArrays()
            org.lwjgl.opengl.GL30C.glBindVertexArray(vao)
        }

        program = linkProgram(compileShader(GL_VERTEX_SHADER, VERT), compileShader(GL_FRAGMENT_SHADER, FRAG))

        val quadData = floatArrayOf(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )
        quadVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
        glBufferData(GL_ARRAY_BUFFER, quadData, GL_STATIC_DRAW)
        instancedVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH * STRIDE * 4L, GL_STREAM_DRAW)

        uWindow = glGetUniformLocation(program, "uWindow")
        uTransform = glGetUniformLocation(program, "uTransform")
        aLocal = glGetAttribLocation(program, "aLocal")
        iRect = glGetAttribLocation(program, "iRect")
        iRadii = glGetAttribLocation(program, "iRadii")
        iColor0 = glGetAttribLocation(program, "iColor0")
        iColor1 = glGetAttribLocation(program, "iColor1")
        iUVRect = glGetAttribLocation(program, "iUVRect")
        iThickness = glGetAttribLocation(program, "iThickness")

        if (caps().OpenGL30) {
            var offset = 0L
            offset = enableAttrib(iRect, 4, offset)
            offset = enableAttrib(iRadii, 4, offset)
            offset = enableAttrib(iColor0, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iColor1, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iUVRect, 4, offset)
            enableAttrib(iThickness, 1, offset)
            org.lwjgl.opengl.GL30C.glBindVertexArray(0)
        }


        atlas = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, atlas)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            ATLAS_SIZE,
            ATLAS_SIZE,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null as ByteBuffer?
        )
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        scissorDepth = 0
        alphaCap = 255
        count = 0
        buffer.clear()
        transform.set(IDENTITY)
        popFlushNeeded = false
        transformDepth = 0
        glUseProgram(program)
        if (caps().OpenGL30) org.lwjgl.opengl.GL30C.glBindVertexArray(vao)
        glUniform2f(uWindow, width, height)
        glUniformMatrix3fv(uTransform, false, transform)
        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_SCISSOR_TEST)
        glGetIntegerv(GL_VIEWPORT, VIEWPORT)
        this.pixelRatio = pixelRatio
    }

    override fun endFrame() {
        flush()
        glDisable(GL_SCISSOR_TEST)
        glDisable(GL_BLEND)
        glUseProgram(0)
    }

    @kotlin.internal.InlineOnly
    private inline fun Int.capAlpha(): Int {
        val a = (this ushr 24) and 0xFF
        val capped = if (a > alphaCap) alphaCap else a
        return (capped shl 24) or (this and 0x00FFFFFF)
    }

    private fun flush() {
        if (count == 0) return
        buffer.flip()
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)
        // 'orphan' the buffer - give a hint to the driver that we don't need the old data so we don't stall waiting for it
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH * STRIDE * 4L, GL_STREAM_DRAW)
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer)

        // upload transform if needed
        if (popFlushNeeded) {
            glUniformMatrix3fv(uTransform, false, transform)
            popFlushNeeded = false
        }

        glBindTexture(GL_TEXTURE_2D, atlas)

        // Quad attrib
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
        glEnableVertexAttribArray(aLocal)
        glVertexAttribPointer(aLocal, 2, GL_FLOAT, false, 0, 0L)

        // Instance attribs
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)

        // asm: on VAO the state is stored, so we only need to set it up once
        if (!caps().OpenGL30) {
            var offset = 0L
            offset = enableAttrib(iRect, 4, offset)
            offset = enableAttrib(iRadii, 4, offset)
            offset = enableAttrib(iColor0, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iColor1, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iUVRect, 4, offset)
            enableAttrib(iThickness, 1, offset)
        }

        // Draw all instances
        if (caps().OpenGL31) org.lwjgl.opengl.GL31C.glDrawArraysInstanced(GL_TRIANGLE_FAN, 0, 4, count)
        else org.lwjgl.opengl.ARBDrawInstanced.glDrawArraysInstancedARB(GL_TRIANGLE_FAN, 0, 4, count)

        count = 0
        buffer.clear()
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun enableAttrib(loc: Int, size: Int, offset: Long, type: Int = GL_FLOAT): Long {
        glEnableVertexAttribArray(loc)
        glVertexAttribPointer(loc, size, type, false, STRIDE * 4, offset)
        // i don't know why core disables the extension functions... but ok!
        if (caps().OpenGL33) org.lwjgl.opengl.GL33C.glVertexAttribDivisor(loc, 1)
        else org.lwjgl.opengl.ARBInstancedArrays.glVertexAttribDivisorARB(loc, 1)
        return offset + size * 4L
    }

    override fun rect(
        x: Float, y: Float, width: Float, height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        val buffer = buffer
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(color.argb.capAlpha()))
        if (color is PolyColor.Gradient) {
            buffer.put(java.lang.Float.intBitsToFloat(color.color2.argb.capAlpha()))
            when (val type = color.type) {
                is PolyColor.Gradient.Type.LeftToRight -> {
                    buffer.put(0f).put(height / 2f).put(width).put(height / 2f)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.TopToBottom -> {
                    buffer.put(width / 2f).put(0f).put(width / 2f).put(height)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.BottomLeftToTopRight -> {
                    buffer.put(0f).put(height).put(width).put(0f)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.TopLeftToBottomRight -> {
                    buffer.put(0f).put(0f).put(width).put(height)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.Radial -> {
                    buffer.put(if (type.centerX == -1f) width / 2f else type.centerX)
                        .put(if (type.centerY == -1f) height / 2f else type.centerY).put(type.innerRadius)
                        .put(type.outerRadius)
                    buffer.put(-3f)
                }

                is PolyColor.Gradient.Type.Box -> {
                    buffer.put(type.radius).put(type.feather).put(0f).put(0f)
                    buffer.put(-4f)
                }
            }
        } else {
            buffer.put(0f) // color1 unused
            buffer.put(NO_UV) // -1f UVs to indicate no texture
            buffer.put(0f)
        }

        count += 1
    }

    override fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(color.argb.capAlpha()))
        buffer.put(0f) // color1 unused
        buffer.put(NO_UV) // -1f UVs to indicate no texture
        buffer.put(lineWidth)
        count += 1
    }

    override fun image(
        image: PolyImage, x: Float, y: Float, width: Float, height: Float,
        colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float
    ) {
        if (count >= MAX_BATCH) flush()

        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(colorMask.capAlpha()))
        buffer.put(0f) // color1 unused
        buffer.put(image.uv.x).put(image.uv.y).put(image.uv.w).put(image.uv.h)
        buffer.put(0f) // thickness = 0 for filled rect
        count += 1
    }

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {
        val fAtlas = getFontAtlas(font, fontSize)
        if (count >= MAX_BATCH) flush()

        var penX = x
        val scaleFactor = fontSize / fAtlas.renderedSize
        val penY = y + (fAtlas.ascent + fAtlas.descent) * scaleFactor
        val col = java.lang.Float.intBitsToFloat(color.argb.capAlpha())
        val buffer = buffer

        for (c in text) {
            if (count >= MAX_BATCH) flush()
            val glyph = fAtlas.get(c)
            buffer.put(penX + glyph.xOff * scaleFactor).put(penY + glyph.yOff * scaleFactor)
                .put(glyph.width * scaleFactor).put(glyph.height * scaleFactor)
            buffer.put(EMPTY_ROW) // zero radii
            buffer.put(col)
            buffer.put(0f) // color1 unused
            buffer.put(glyph, 0, 4) // UVs
            buffer.put(-1f) // thickness = -1 for text
            penX += glyph.xAdvance * scaleFactor
            count += 1
        }
    }

    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        return getFontAtlas(font, fontSize).measure(text, fontSize)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (y1 == y2) rect(x1, y1, x2 - x1, width, color, 0f, 0f, 0f, 0f)
        else rect(x1, y1, width, y2 - y1, color, 0f, 0f, 0f, 0f)
    }

    override fun dropShadow(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blur: Float,
        spread: Float,
        radius: Float
    ) {
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(EMPTY_ROW) // zero radii
        buffer.put(java.lang.Float.intBitsToFloat(alphaCap shl 24)) // black, alpha to alphaCap
        buffer.put(0f) // color1 unused
        buffer.put(spread).put(blur).put(0f).put(0f)
        buffer.put(-5f) // thickness = -5 for drop shadow
        count += 1
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        flush()
        val nx = (x * pixelRatio).roundToInt()
        val ny = ((VIEWPORT[3] + VIEWPORT[1]) - (y + height) * pixelRatio).roundToInt()
        val nw = (width * pixelRatio).roundToInt()
        val nh = (height * pixelRatio).roundToInt()
        scissorStack[scissorDepth++] = nx
        scissorStack[scissorDepth++] = ny
        scissorStack[scissorDepth++] = nw
        scissorStack[scissorDepth++] = nh
        glEnable(GL_SCISSOR_TEST)
        glScissor(nx, ny, nw, nh)
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
        if (scissorDepth < 4) {
            pushScissor(x, y, width, height)
            return
        }
        flush()
        val px = scissorStack[scissorDepth - 4]
        val py = scissorStack[scissorDepth - 3]
        val pw = scissorStack[scissorDepth - 2]
        val ph = scissorStack[scissorDepth - 1]
        val nx = (x * pixelRatio).roundToInt()
        val ny = ((VIEWPORT[3] + VIEWPORT[1]) - (y + height) * pixelRatio).roundToInt()

        val ix = maxOf(nx, px)
        val iy = maxOf(ny, py)
        val iw = maxOf(0, minOf(nx + (width * pixelRatio).roundToInt(), px + pw) - ix)
        val ih = maxOf(0, minOf(ny + (height * pixelRatio).roundToInt(), py + ph) - iy)

        scissorStack[scissorDepth++] = ix
        scissorStack[scissorDepth++] = iy
        scissorStack[scissorDepth++] = iw
        scissorStack[scissorDepth++] = ih

        glEnable(GL_SCISSOR_TEST)
        glScissor(ix, iy, iw, ih)
    }

    override fun popScissor() {
        flush()
        if (scissorDepth <= 4) {
            scissorDepth = 0
            glDisable(GL_SCISSOR_TEST)
            return
        }
        scissorDepth -= 4
        val x = scissorStack[scissorDepth - 4]
        val y = scissorStack[scissorDepth - 3]
        val width = scissorStack[scissorDepth - 2]
        val height = scissorStack[scissorDepth - 1]
        glEnable(GL_SCISSOR_TEST)
        glScissor(x, y, width, height)
    }

    override fun globalAlpha(alpha: Float) {
        alphaCap = (alpha * 255f).toInt()
    }

    override fun resetGlobalAlpha() = globalAlpha(1f)

    override fun transformsWithPoint() = false

    override fun push() {
        if (transform.isIdentity()) return
        transformStack[transformDepth++].set(transform)
    }

    override fun pop() {
        if (transform.isIdentity()) return
        // asm: flush out any pending draws before changing transform state back to previous
        flush()
        if (transformDepth == 0) {
            transform.set(IDENTITY)
        } else {
            transform.setThenClear(transformStack[--transformDepth])
        }
        popFlushNeeded = true
    }

    override fun translate(x: Float, y: Float) {
        flush()
        transform[6] += transform[0] * x + transform[3] * y
        transform[7] += transform[1] * x + transform[4] * y
        popFlushNeeded = true
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        flush()
        transform[0] *= sx; transform[1] *= sx
        transform[3] *= sy; transform[4] *= sy
        popFlushNeeded = true
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
        popFlushNeeded = true
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
        popFlushNeeded = true

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
        popFlushNeeded = true
    }

    @JvmStatic
    private fun FloatArray.isIdentity(): Boolean {
        return this[0] == 1f && this[1] == 0f && this[2] == 0f &&
                this[3] == 0f && this[4] == 1f && this[5] == 0f &&
                this[6] == 0f && this[7] == 0f && this[8] == 1f
    }

    @JvmStatic
    private fun FloatArray.set(other: FloatArray): FloatArray {
        System.arraycopy(other, 0, this, 0, 9)
        return this
    }

    @JvmStatic
    private fun FloatArray.setThenClear(other: FloatArray) {
        System.arraycopy(other, 0, this, 0, 9)
        System.arraycopy(IDENTITY, 0, other, 0, 9)
    }

    override fun initImage(image: PolyImage, size: Vec2) {
        if (image.initialized) return
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
        if (image.type == PolyImage.Type.Raster) stbi_image_free(d)

        slotX += w[0] + 1
        if (h[0] + 1 > atlasRowHeight) atlasRowHeight = h[0] + 1
        image.reportInit()
    }

    private fun initImage(image: PolyImage, w: IntArray, h: IntArray): ByteBuffer {
        if (image.type == PolyImage.Type.Vector) {
            if (nSvgRaster == 0L) {
                nSvgRaster = nsvgCreateRasterizer()
                if (nSvgRaster == 0L) throw IllegalStateException("Could not create SVG rasterizer")
            }
            val svg = nsvgParse(image.load().toDirectByteBufferNT(), PIXELS, 96f)
                ?: throw IllegalStateException("Could not parse SVG image ${image.resourcePath}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(svg.width(), svg.height()))
            w[0] = (svg.width() * ATLAS_SVG_UPSCALE_FACTOR).toInt()
            h[0] = (svg.height() * ATLAS_SVG_UPSCALE_FACTOR).toInt()
            val dst = BufferUtils.createByteBuffer(w[0] * h[0] * 4)
            nsvgRasterize(nSvgRaster, svg, 0f, 0f, ATLAS_SVG_UPSCALE_FACTOR, dst, w[0], h[0], w[0] * 4)
            nsvgDelete(svg)
            return dst
        } else {
            val data = image.load().toDirectByteBuffer()
            val d = stbi_load_from_memory(data, w, h, IntArray(1), 4)
                ?: throw IllegalStateException("Failed to load image ${image.resourcePath}: ${stbi_failure_reason()}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(w[0].toFloat(), h[0].toFloat()))
            return d
        }
    }

    private fun getFontAtlas(font: Font, fontSize: Float): FontAtlas {
        val p = when (fontSize) {
            in 0f..48f -> 48f
            else -> 96f
        }
        return fonts.getOrPut(font.resourcePath.hashCode() + p.toInt()) {
            val data = font.load {
                LOGGER.error("Failed to load font: $font", it)
                return@getOrPut fonts[PolyUI.defaultFonts.regular.resourcePath.hashCode() + p.toInt()]
                    ?: throw IllegalStateException("Default font couldn't be loaded")
            }.toDirectByteBuffer()
            FontAtlas(data, p)
        }
    }

    fun dumpAtlas() {
        val buf = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4)
        glBindTexture(GL_TEXTURE_2D, atlas)
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)
        glBindTexture(GL_TEXTURE_2D, 0)
        org.lwjgl.stb.STBImageWrite.stbi_write_png(
            "debug_atlas.png",
            ATLAS_SIZE,
            ATLAS_SIZE,
            4,
            buf,
            ATLAS_SIZE * 4
        )
    }

    override fun cleanup() {
//        dumpAtlas()
        if (program != 0) glDeleteProgram(program)
        if (quadVbo != 0) glDeleteBuffers(quadVbo)
        if (instancedVbo != 0) glDeleteBuffers(instancedVbo)
        if (atlas != 0) glDeleteTextures(atlas)
        if (nSvgRaster != 0L) nsvgDeleteRasterizer(nSvgRaster)
        fonts.clear()
        buffer.clear()
    }

    override fun delete(font: Font?) {}
    override fun delete(image: PolyImage?) {}

    override fun close() {
        cleanup()
    }

    private class FontAtlas(data: ByteBuffer, val renderedSize: Float) {
        private val glyphs: Array<FloatArray>
        val ascent: Float
        val descent: Float
        val lineGap: Float

        init {
            val stbFont = STBTTFontinfo.malloc()
            if (!stbtt_InitFont(stbFont, data)) {
                throw IllegalStateException("Failed to initialize font")
            }
            val scale = stbtt_ScaleForMappingEmToPixels(stbFont, renderedSize)
            val asc = IntArray(1)
            val des = IntArray(1)
            val gap = IntArray(1)
            stbtt_GetFontVMetrics(stbFont, asc, des, gap)
            stbFont.free()
            val pixelHeight = (asc[0] - des[0]) * scale
            ascent = asc[0] * scale
            descent = des[0] * scale
            lineGap = gap[0] * scale

            val range = STBTTPackRange.malloc()
            range.font_size(renderedSize)
            range.first_unicode_codepoint_in_range(32)
            range.num_chars(95)
            val packed = STBTTPackedchar.malloc(range.num_chars())
            range.chardata_for_range(packed)

            val bitMap = BufferUtils.createByteBuffer(FONT_MAX_BITMAP_W * FONT_MAX_BITMAP_H)
            val pack = STBTTPackContext.malloc()
            if (!stbtt_PackBegin(pack, bitMap, FONT_MAX_BITMAP_W, FONT_MAX_BITMAP_H, 0, 1, 0L)) {
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

            glyphs = Array(range.num_chars()) {
                val g = packed.get(it)
                floatArrayOf(
                    (sx + g.x0()) / ATLAS_SIZE.toFloat(),
                    (sy + g.y0()) / ATLAS_SIZE.toFloat(),
                    (g.x1() - g.x0()) / ATLAS_SIZE.toFloat(),
                    (g.y1() - g.y0()) / ATLAS_SIZE.toFloat(),
                    g.xoff(),
                    g.yoff(),
                    (g.x1() - g.x0()).toFloat(),
                    (g.y1() - g.y0()).toFloat(),
                    g.xadvance()
                )
            }
            packed.free()
            range.free()

            glBindTexture(GL_TEXTURE_2D, atlas)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            // can't write to the alpha channel in GL3 core! lol hahaahahah
            glTexSubImage2D(
                GL_TEXTURE_2D,
                0,
                sx,
                sy,
                FONT_MAX_BITMAP_W,
                FONT_MAX_BITMAP_H,
                GL_RED,
                GL_UNSIGNED_BYTE,
                bitMap
            )
            glBindTexture(GL_TEXTURE_2D, 0)
            slotX += totalSizeX
            atlasRowHeight = maxOf(atlasRowHeight, totalSizeY)
        }

        fun measure(text: String, fontSize: Float): Vec2 {
            var width = 0f
//            var height = 0f
            val scaleFactor = fontSize / this.renderedSize
            for (c in text) {
                val g = get(c)
                width += g.xAdvance * scaleFactor
//                height = maxOf(height, g.height + g.offsetY)
            }
            return Vec2.of(width, fontSize)
        }

        @Suppress("DEPRECATION")
        @kotlin.internal.InlineOnly
        inline fun get(char: Char) = glyphs[(char.toInt() - 32) /* .coerceIn(0, glyphs.size - 1) */]

    }

    @kotlin.internal.InlineOnly
    inline val FloatArray.u get() = this[0]

    @kotlin.internal.InlineOnly
    inline val FloatArray.v get() = this[1]

    @kotlin.internal.InlineOnly
    inline val FloatArray.uw get() = this[2]

    @kotlin.internal.InlineOnly
    inline val FloatArray.vh get() = this[3]

    @kotlin.internal.InlineOnly
    inline val FloatArray.xOff get() = this[4]

    @kotlin.internal.InlineOnly
    inline val FloatArray.yOff get() = this[5]

    @kotlin.internal.InlineOnly
    inline val FloatArray.width get() = this[6]

    @kotlin.internal.InlineOnly
    inline val FloatArray.height get() = this[7]

    @kotlin.internal.InlineOnly
    inline val FloatArray.xAdvance get() = this[8]
}
