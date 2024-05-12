/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_DISABLED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.PolyUI.Companion.LOGGER
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.countChildren
import org.polyfrost.polyui.component.impl.PopupMenu
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.seconds
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

/**
 * the debug utilities for PolyUI.
 *
 * Contains various methods for performance monitoring, debugging component, printing state and more.
 * @since 1.1.7
 */
class Debugger(private val polyUI: PolyUI) {
    // telemetry
    var fps: Int = 1
        private set
    private var frames = 0
    internal var nframes = 0
    var longestFrame = 0.0
        private set
    var shortestFrame = 100.0
        private set
    private var timeInFrames = 0.0
    var avgFrame = 0.0
        private set
    private var perf: String = ""
    private val formatter = DecimalFormat("#.###")

    private val telemtryExecutor = Clock.FixedTimeExecutor(1.seconds) {
        val sb = StringBuilder(64)
        sb.append("fps: ").append(fps)
            .append(", avg/max/min: ")
            .append(formatter.format(avgFrame)).append("ms; ")
            .append(formatter.format(longestFrame)).append("ms; ")
            .append(formatter.format(shortestFrame)).append("ms")
        if (polyUI.canPauseRendering) {
            val skipPercent = (1.0 - (frames.toDouble() / nframes)) * 100.0
            sb.append(", skip=").append(formatter.format(skipPercent)).append('%')
        }
        perf = sb.toString()
        longestFrame = 0.0
        shortestFrame = 100.0
        avgFrame = timeInFrames / fps
        timeInFrames = 0.0
        fps = frames
        frames = 0
        nframes = 0
        polyUI.master.needsRedraw = true
//        if (polyUI.drew) LOGGER.info(perf)
    }

    private val printBind = KeyBinder.Bind('P', mods = mods(KeyModifiers.LCONTROL)) {
        LOGGER.info(debugString())
        true
    }

    private val inspectBind = KeyBinder.Bind(chars = null, mouse = intArrayOf(0), mods = mods(KeyModifiers.LSHIFT), durationNanos = 0.4.seconds) {
        openDebugWindow(polyUI.inputManager.rayCheckUnsafe(polyUI.master, polyUI.mouseX, polyUI.mouseY))
        true
    }


    init {
        if (polyUI.settings.debug) {
            polyUI.addExecutor(telemtryExecutor)
            val keyBinder = polyUI.keyBinder
            if (keyBinder == null) {
                LOGGER.warn("PolyUI instance was created without a keybinder. Some debug features may not work.")
            } else {
                keyBinder.add(printBind)
                keyBinder.add(inspectBind)
            }
        }
        if (polyUI.settings.enableDebugKeybind) {
            polyUI.keyBinder?.add(
                KeyBinder.Bind('I', mods = mods(KeyModifiers.LCONTROL, KeyModifiers.LSHIFT)) {
                    polyUI.settings.debug = !polyUI.settings.debug
                    polyUI.master.needsRedraw = true
                    val s = if (polyUI.settings.debug) {
                        polyUI.addExecutor(telemtryExecutor)
                        polyUI.keyBinder?.add(printBind, inspectBind)
                        "enabled"
                    } else {
                        polyUI.removeExecutor(telemtryExecutor)
                        polyUI.keyBinder?.remove(printBind)
                        polyUI.keyBinder?.remove(inspectBind)
                        "disabled"
                    }
                    LOGGER.info("Debug mode $s")
                    true
                },
            )

        }
    }

    @ApiStatus.Internal
    fun takeReadings() {
        val frameTime = (Clock.peek()) / 1_000_000.0
        timeInFrames += frameTime
        if (frameTime > longestFrame) longestFrame = frameTime
        else if (frameTime < shortestFrame) shortestFrame = frameTime
        frames++
    }

    fun render() {
        polyUI.apply {
            renderer.text(
                monospaceFont,
                0f, size.y - 11f,
                text = perf,
                color = colors.text.primary.normal,
                fontSize = 10f,
            )
            master.debugDraw()
            if (inputManager.focused == null) {
                val mods = inputManager.keyModifiers
                if (mods.hasControl) {
                    val obj = inputManager.mouseOver
                    if (obj != null) {
                        val os = obj.toString()
                        val w = renderer.textBounds(monospaceFont, os, 10f).x
                        val pos = min(max(0f, mouseX - w / 2f), this.size.x - w - 10f)
                        renderer.rect(pos, mouseY - 14f, w + 10f, 14f, colors.component.bg.hovered)
                        renderer.text(monospaceFont, pos + 5f, mouseY - 10f, text = os, colors.text.primary.normal, 10f)
                        master.needsRedraw = true
                    }
                }
                if (mods.hasShift) {
                    val s = "${inputManager.mouseX}x${inputManager.mouseY}"
                    val ww = renderer.textBounds(monospaceFont, s, 10f).x
                    val ppos = min(max(0f, mouseX + 10f), this.size.x - ww - 10f)
                    val pposy = min(max(0f, mouseY), this.size.y - 14f)
                    renderer.rect(ppos, pposy, ww + 10f, 14f, colors.component.bg.hovered)
                    renderer.text(monospaceFont, ppos + 5f, pposy + 4f, text = s, colors.text.primary.normal, 10f)
                    master.needsRedraw = true
                }
            }
        }
    }


    /**
     * return a string of this PolyUI instance's components and children in a list, like this:
     * ```
     * PolyUI(800.0 x 800.0) with 0 components and 2 layouts:
     *   PixelLayout@5bcab519 [Draggable](20.0x570.0, 540.0 x 150.0)
     * 	 Text@6eebc39e(40.0x570.0, 520.0x32.0)
     * 	 Block@2f686d1f(40.0x600.0, 120.0x120.0)
     * 	 Block@7085bdee(220.0x600.0, 120.0x120.0)
     * 	 Image@6fd02e5(400.0x600.0, 120.0x120.0 (auto))
     * 	 ... 2 more
     *   FlexLayout@73846619 [Scrollable](20.0x30.0, 693.73267 x 409.47168, buffered, needsRedraw)
     * 	 Block@32e6e9c3(20.0x30.0, 61.111263x42.21167)
     * 	 Block@5056dfcb(86.11127x30.0, 40.909004x76.32132)
     * 	 Block@6574b225(132.02026x30.0, 52.75415x52.59597)
     * 	 Block@2669b199(189.77441x30.0, 76.59671x45.275665)
     * ```
     */
    fun debugString(): String {
        val sb = StringBuilder(1024).append(polyUI.toString())
        val children = polyUI.master.children ?: run {
            sb.append(" with 0 children, totalling 0 components.")
            return sb.toString()
        }
        sb.append(" with ${children.size} children, totalling ${polyUI.master.countChildren()} components:")
        children.fastEach {
            sb.append("\n\t").append(it.toString())
            _debugString(it.children ?: return@fastEach, 1, sb)
        }
        return sb.toString()
    }

    private fun _debugString(list: LinkedList<Drawable>, depth: Int, sb: StringBuilder) {
        var i = 0
        var ii = 0
        val ndepth = depth + 1
        list.fastEach {
            if (it.initialized) {
                sb.append('\n').append('\t', ndepth).append(it.toString())
                val children = it.children
                if (children != null) _debugString(children, ndepth, sb)
                else i++
            } else ii += it.countChildren() + 1
            if (i >= 5) {
                sb.append('\n').append('\t', ndepth).append("... ").append(list.size - i).append(" more")
                return
            }
        }
        if (ii != 0) sb.append('\n').append('\t', ndepth).append("... (").append(ii).append(" uninitialized)")
    }

    fun openDebugWindow(d: Drawable?) {
        if (d == null) return
        val f = PolyUI.monospaceFont
        PopupMenu(
            Text(d.simpleName, font = f, fontSize = 16f),
            Text(
                ("""
parent: ${d._parent?.simpleName}   siblings: ${d._parent?.countChildren()?.dec()}
children: ${d.countChildren()};  fbo: ${d.framebuffer}, renders: ${d.renders}
at: ${d.x}x${d.y}
size: ${d.size} visible: ${d._visibleSize}
rgba: ${d.color.r}, ${d.color.g}, ${d.color.b}, ${d.color.a}
scale: ${d.scaleX}x${d.scaleY};  skew: ${d.skewX}x${d.skewY}
rotation: ${d.rotation};  alpha: ${d.alpha}
redraw: ${d.needsRedraw};  ops=${d.operating};  scroll: ${d.scrolling}
state: ${getInputStateString(d.inputState)}
            """ + (d.debugString()?.let { "\n\n$it" } ?: "")).translated().dont(),
                font = f
            ),
            align = Align(maxRowSize = 1),
            polyUI = polyUI,
            position = Point.Below
        )
    }

    private fun getInputStateString(state: Byte) = when (state) {
        INPUT_DISABLED -> "disabled"
        INPUT_NONE -> "none"
        INPUT_HOVERED -> "hovered"
        INPUT_PRESSED -> "pressed"
        else -> "??"
    }

}
