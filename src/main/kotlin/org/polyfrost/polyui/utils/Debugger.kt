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
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.PolyUI.Companion.LOGGER
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.asMutable
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.Scrollable
import org.polyfrost.polyui.component.extensions.countChildren
import org.polyfrost.polyui.component.extensions.disable
import org.polyfrost.polyui.component.extensions.events
import org.polyfrost.polyui.component.extensions.fix
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.input.*
import org.polyfrost.polyui.operations.ShakeOp
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.SpawnPos
import org.polyfrost.polyui.unit.seconds
import java.lang.reflect.Method
import java.text.DecimalFormat

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

    private val telemetryExecutor = Clock.FixedTimeExecutor(1.seconds) {
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
        if (polyUI.settings.printPerfInfo && polyUI.drew) LOGGER.info(perf)
    }

    private val printBind = PolyBind(key = Keys.P, mods = Modifiers(KeyModifiers.PRIMARY)) {
        if (it) LOGGER.info(debugString())
        true
    }

    private val inspectBind = PolyBind(mouse = Mouse.LEFT_MOUSE, mods = Modifiers(KeyModifiers.SHIFT), durationNanos = 0.4.seconds) {
        if (it) openDebugWindow(polyUI.inputManager.rayCheckUnsafe(polyUI.master, polyUI.mouseX, polyUI.mouseY))
        true
    }

    private var forEval: Component? = null

    private val evalWindow by lazy {
        Block(
            TextInput(placeholder = "evaluate expression...", font = polyUI.monospaceFont).events {
                Event.Focused.Companion.KeyTyped then {
                    parent.recalculate()
                }
                Event.Focused.Companion.KeyPressed then {
                    parent.recalculate()
                    if (it.key == Keys.ENTER) {
                        if (!processEval(forEval, text)) {
                            ShakeOp(parent, 0.2.seconds, oscillations = 2) {
                                this@Debugger.polyUI.inputManager.unfocus(this@then)
                            }.add()
                        } else {
                            if (!it.mods.hasShift) text = ""
                            this@Debugger.polyUI.inputManager.unfocus(this)
                        }
                    }
                }
                Event.Focused.Lost then {
                    parent.remove(recalculate = false)
                }
            },
        ).disable()
    }

    private val evalBind = PolyBind(key = Keys.TAB, mods = Modifiers(KeyModifiers.SHIFT)) {
        if (!it) return@PolyBind false
        val forEval = polyUI.inputManager.rayCheckUnsafe(polyUI.master, polyUI.mouseX, polyUI.mouseY) ?: return@PolyBind false
        this.forEval = forEval
        val evalWindow = evalWindow
        (evalWindow[0] as TextInput).placeholder = "evaluate ${forEval.name}..."
        if (evalWindow.initialized) evalWindow.recalculate()
        if (!evalWindow.isEnabled) polyUI.master.addChild(evalWindow, recalculate = false)
        evalWindow.x = polyUI.mouseX - evalWindow.width / 2f
        evalWindow.y = polyUI.mouseY - 30f
        polyUI.focus(evalWindow[0])
        polyUI.master.needsRedraw = true
        true
    }

    private fun processEval(target: Component?, eval: String): Boolean {
        if (eval.isEmpty()) return false
        if (target == null) return false
        if (eval.startsWith("colors", true)) {
            val newColors = PolyUI.registeredThemes.get(eval.substringAfterLast('=').trim().lowercase())
            return if (newColors != null) {
                polyUI.colors = newColors
                true
            } else {
                LOGGER.warn("unknown color theme: $eval")
                false
            }
        }
        if (eval.startsWith("fonts ", true)) {
            val newFonts = PolyUI.registeredFonts.get(eval.substringAfterLast('=').trim().lowercase())
            return if (newFonts != null) {
                polyUI.fonts = newFonts
                true
            } else {
                LOGGER.warn("unknown font pack: $eval")
                false
            }
        }

//        if (eval.startsWith("settings.")) { TODO
//
//        }

        if (eval[0] == '[') {
            val idx = eval.indexOf(']')
            if (idx == -1 || idx + 2 > eval.length) return false
            val child = eval.substring(1, idx).toIntOrNull() ?: return false
            return processEval(target[child], eval.substring(if (eval[idx + 1] == '.') idx + 2 else idx + 1))
        }
        val set = eval.indexOf('=')
        if (set == -1) {
            val fe = eval.indexOf(')')
            if (fe == -1) return false
            val fs = eval.indexOf('(')
            if (fs < 1) return false
            val fn = eval.substring(0, fs)
            if (fn == "fix") {
                target.fix()
                return true
            }
            val methods = target::class.java.allMethods(fn)
            // no args specified
            if (fs + 1 == fe) {
                for (method in methods) {
                    if (method.parameterCount == 0) {
                        method.invoke(target)
                        return true
                    }
                }
            }
//            val args = eval.substring(fs + 1, eval.length - 1).split(',')
//            args.forEach { it.trim() }
//            if (args.isEmpty()) {
//                if (methods.size != 1) return false
//                methods[0].invoke(target)
//                return true
//            }
            LOGGER.warn("no methods matching $fn() on $target")
            return false
        }
        if (set == 0 || set == eval.length) return false
        if (target is Drawable && eval == "color = picker()") {
            target.color = target.color.asMutable()
            ColorPicker(State(target.color.asMutable()), polyUI)
            return true
        }
        @Suppress("DEPRECATION")
        val sn = "set${eval.substring(0, set).trim().capitalize()}"
        val arg = eval.substring(set + 1).trim().removeSurrounding('"')
        val methods = target::class.java.allMethods(sn)
        for (method in methods) {
            if (method.parameterCount != 1) continue
            val typ = method.parameterTypes[0]
            if (typ.isEnum) {
                val const = try {
                    @Suppress("UNCHECKED_CAST")
                    java.lang.Enum.valueOf(typ as Class<out Enum<*>>, arg)
                } catch (_: Exception) {
                    null
                }
                return if (const != null) {
                    method.invoke(target, const)
                    true
                } else false
            }
            val p: Any? = when (typ) {
                String::class.java -> arg
                Float::class.java -> arg.toFloatOrNull()
                Int::class.java -> arg.toIntOrNull()
                Long::class.java -> arg.toLongOrNull()
                Double::class.java -> arg.toDoubleOrNull()
                Byte::class.java -> arg.toByteOrNull()
                Boolean::class.java -> arg.toBoolean()
                Font::class.java -> Font.of(arg)
                PolyImage::class.java -> PolyImage.of(arg)
                PolyColor::class.java -> {
                    val split = arg.split(',').mapToArray { it.trim().toInt() }
                    if (split.size < 3) null
                    else if (split.size == 3) rgba(split[0], split[1], split[2])
                    else rgba(split[0], split[1], split[2], split[3] / 255f)
                }

                else -> null
            }
            if (p == null) {
                LOGGER.warn("couldn't convert argument $arg to type $typ")
                return false
            }
            try {
                method.invoke(target, p)
                return true
            } catch (e: Exception) {
                LOGGER.warn("failed to invoke $sn($arg) on $target", e)
                return false
            }
        }
        LOGGER.warn("no methods matching $sn($arg) on $target")
        return false
    }

    private fun Class<*>.allMethods(name: String, ls: MutableList<Method>? = null): MutableList<Method> {
        val list = ls ?: mutableListOf()
        for (method in this.declaredMethods) {
            if (method.name == name) list.add(method)
        }
        this.superclass?.allMethods(name, list)
        for (inter in this.interfaces) {
            inter.allMethods(name, list)
        }
        return list
    }


    init {
        if (polyUI.settings.debug) {
            polyUI.addExecutor(telemetryExecutor)
            val keyBinder = polyUI.keyBinder
            if (keyBinder == null) {
                LOGGER.warn("PolyUI instance was created without a keybinder. Some debug features will not work.")
            } else {
                keyBinder.add(printBind, inspectBind, evalBind)
            }
        }
        if (polyUI.settings.enableDebugKeybind) {
            polyUI.keyBinder?.add(
                PolyBind(key = Keys.I, mods = Modifiers(KeyModifiers.PRIMARY, KeyModifiers.SHIFT)) {
                    if (!it) return@PolyBind false
                    polyUI.settings.debug = !polyUI.settings.debug
                    polyUI.master.needsRedraw = true
                    val s = if (polyUI.settings.debug) {
                        polyUI.addExecutor(telemetryExecutor)
                        polyUI.keyBinder?.add(printBind, inspectBind, evalBind)
                        "enabled"
                    } else {
                        polyUI.removeExecutor(telemetryExecutor)
                        polyUI.keyBinder?.remove(printBind, inspectBind, evalBind)
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
        val frameTime = (polyUI.clock.peek()) / 1_000_000.0
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
            val mods = inputManager.keyModifiers
            if (mods.hasPrimary) {
                val obj = inputManager.mouseOver
                if (obj != null) {
                    val os = obj.toString()
                    val w = renderer.textBounds(monospaceFont, os, 10f).x
                    val pos = (mouseX - w / 2f).coerceWithin(0f, this.size.x - w - 10f)
                    renderer.rect(pos, mouseY - 14f, w + 10f, 14f, colors.component.bg.hovered)
                    renderer.text(monospaceFont, pos + 5f, mouseY - 10f, text = os, colors.text.primary.normal, 10f)
                    master.needsRedraw = true
                }
            }
            if (mods.hasShift) {
                master.needsRedraw = true
                val s = "${inputManager.mouseX}x${inputManager.mouseY}"
                val ww = renderer.textBounds(monospaceFont, s, 10f).x
                val ppos = (mouseX + 10f).coerceWithin(0f, this.size.x - ww - 10f)
                val pposy = mouseY.coerceWithin(0f, this.size.y - 14f)
                renderer.rect(ppos, pposy, ww + 10f, 14f, colors.component.bg.hovered)
                renderer.text(monospaceFont, ppos + 5f, pposy + 4f, text = s, colors.text.primary.normal, 10f)
            }
            if (mods.hasAlt) {
                val hovered = inputManager.rayCheckUnsafe(polyUI.master, mouseX, mouseY) ?: return@apply
                val parent = hovered._parent ?: polyUI.master

                var closestLeft: Component? = null
                var closestRight: Component? = null
                var closestTop: Component? = null
                var closestBottom: Component? = null

                val hoveredSize = hovered.visibleSize
                val hoveredCenterX = hovered.x + hoveredSize.x / 2f
                val hoveredCenterY = hovered.y + hoveredSize.y / 2f

                parent.children?.fastEach {
                    if (it === hovered) return@fastEach

                    val ivs = it.visibleSize
                    val right = it.x + ivs.x
                    val bottom = it.y + ivs.y

                    if (right <= hovered.x) {
                        if (closestLeft == null || right > (closestLeft.x + closestLeft.visibleSize.x)) {
                            closestLeft = it
                        }
                    }

                    if (it.x >= hovered.x + hoveredSize.x) {
                        if (closestRight == null || it.x < closestRight.x) {
                            closestRight = it
                        }
                    }

                    if (bottom <= hovered.y) {
                        if (closestTop == null || bottom > (closestTop.y + closestTop.visibleSize.y)) {
                            closestTop = it
                        }
                    }

                    if (it.y >= hovered.y + hoveredSize.y) {
                        if (closestBottom == null || it.y < closestBottom.y) {
                            closestBottom = it
                        }
                    }
                }

                val color = colors.text.primary.normal

                closestLeft?.let {
                    val endX = it.x + it.visibleSize.x
                    val distance = hovered.x - endX
                    if (distance == 0f) return@let
                    renderer.line(hovered.x, hoveredCenterY, endX, hoveredCenterY, color, 1f)
                    renderer.text(monospaceFont, (hovered.x + endX) / 2f, hoveredCenterY - 5f, distance.toInt().toString(), color, 10f)
                } ?: run {
                    val distance = hovered.x - parent.x
                    if (distance == 0f) return@run
                    renderer.line(hovered.x, hoveredCenterY, parent.x, hoveredCenterY, color, 1f)
                    renderer.text(monospaceFont, parent.x + distance / 2f, hoveredCenterY - 5f, distance.toInt().toString(), color, 10f)
                }

                closestRight?.let {
                    val startX = it.x
                    val distance = startX - (hovered.x + hoveredSize.x)
                    if (distance == 0f) return@let
                    renderer.line(hovered.x + hoveredSize.x, hoveredCenterY, startX, hoveredCenterY, color, 1f)
                    renderer.text(monospaceFont, hovered.x + hoveredSize.x + distance / 2f, hovered.y - 5f, distance.toInt().toString(), color, 10f)
                } ?: run {
                    val pvs = parent.visibleSize
                    val distance = parent.x + pvs.x - (hovered.x + hoveredSize.x)
                    if (distance == 0f) return@run
                    renderer.line(hovered.x + hoveredSize.x, hoveredCenterY, parent.x + pvs.x, hoveredCenterY, color, 1f)
                    renderer.text(monospaceFont, hovered.x + hoveredSize.x + distance / 2f, hovered.y - 5f, distance.toInt().toString(), color, 10f)
                }

                closestTop?.let {
                    val endY = it.y + it.visibleSize.y
                    val distance = hovered.y - endY
                    if (distance == 0f) return@let
                    renderer.line(hoveredCenterX, hovered.y, hoveredCenterX, endY, color, 1f)
                    renderer.text(monospaceFont, hovered.x, endY + distance / 2f - 5f, distance.toInt().toString(), color, 10f)
                } ?: run {
                    val distance = hovered.y - parent.y
                    if (distance == 0f) return@run
                    renderer.line(hoveredCenterX, hovered.y, hoveredCenterX, parent.y, color, 1f)
                    renderer.text(monospaceFont, hovered.x, parent.y + distance / 2f - 5f, distance.toInt().toString(), color, 10f)
                }

                closestBottom?.let {
                    val startY = it.y
                    val distance = startY - (hovered.y + hoveredSize.y)
                    if (distance == 0f) return@let
                    renderer.line(hoveredCenterX, hovered.y + hoveredSize.y, hoveredCenterX, startY, color, 1f)
                    renderer.text(monospaceFont, hovered.x, hovered.y + hoveredSize.y + distance / 2f - 5f, distance.toInt().toString(), color, 10f)
                } ?: run {
                    val pvs = parent.visibleSize
                    val distance = parent.y + pvs.y - (hovered.y + hoveredSize.y)
                    if (distance == 0f) return@run
                    renderer.line(hoveredCenterX, hovered.y + hoveredSize.y, hoveredCenterX, parent.y + pvs.y, color, 1f)
                    renderer.text(monospaceFont, hovered.x, hovered.y + hoveredSize.y + distance / 2f - 5f, distance.toInt().toString(), color, 10f)
                }
                master.needsRedraw = true
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
        val children = polyUI.master.children ?: return sb.append(" with 0 children, totalling 0 components.").toString()
        sb.append(" with ${children.size} children, totalling ${polyUI.master.countChildren()} components:")
        children.fastEach {
            sb.append('\n').append(it.toString())
            _debugString(it.children ?: return@fastEach, 0, sb)
        }
        return sb.toString()
    }

    private fun _debugString(list: ArrayList<Component>, depth: Int, sb: StringBuilder) {
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

    fun openDebugWindow(d: Component?) {
        if (d == null) return
        val f = PolyUI.monospaceFont
        PopupMenu(
            Text(d.name, font = f, fontSize = 16f),
            Text(
                ("""
parent: ${d._parent?.name}   siblings: ${d._parent?.countChildren()?.dec()}
children: ${d.countChildren()};  renders: ${d.renders}, ${if (d is Scrollable) "scrolling: ${if (!d.shouldScroll) "disabled" else if (d.scrollingX && d.scrollingY) "x+y" else if (d.scrollingX) "x" else if (d.scrollingY) "y" else "off"}" else ""}
at: ${d.x}x${d.y}, pad: ${d.padding}, flags: ${decodeFlags(d)}
size: ${d.size}, visible: ${if (d.visibleSize != d.size) d.visibleSize.toString() else "null"}
${
                    if (d is Drawable) """rgba: ${d.color.r}, ${d.color.g}, ${d.color.b}, ${d.color.a}
scale: ${d.scaleX}x${d.scaleY};  skew: ${d.skewX}x${d.skewY}
rotation: ${d.rotation};  alpha: ${d.alpha}
redraw: ${d.needsRedraw};  ops=${d.isOperating};  fbo: ${d.framebuffer}""" else ""
                }
${if (d is Inputtable) "state: ${if (d.acceptsInput) getInputStateString(d.inputState) else "never"}, focused: ${if (!d.focusable) "disabled" else if (d.focused) "yes" else "no"}" else ""}
${d.alignment}
            """ + (d.debugString()?.let { "\n\n$it" } ?: "")).translated().dont(),
                font = f
            ),
            align = Align(wrap = Align.Wrap.ALWAYS),
            polyUI = polyUI,
            spawnPos = SpawnPos.BelowMouse
        ).events {
            Event.Focused.Companion.KeyTyped then {
//                if (it.codepoint == 'c' && it.mods.hasControl) {
//                    LOGGER.info("copied ID ${d.name} to clipboard")
//                    polyUI.clipboard = d.name
//                }
            }
        }
    }

    private fun decodeFlags(component: Component): String {
        val sb = StringBuilder(4)
        if (component.createdWithSetPosition) sb.append("Sp")
        if (component.createdWithSetSize) sb.append("Ss")
        if (component.rawRescalePosition) sb.append("Rp")
        if (component.rawRescaleSize) sb.append("Rs")
        if (component.layoutIgnored) sb.append('I')
        if (sb.isEmpty()) sb.append("none")
        return sb.toString()
    }

    private fun getInputStateString(state: Byte) = when (state) {
        INPUT_NONE -> "none"
        INPUT_HOVERED -> "hovered"
        INPUT_PRESSED -> "pressed"
        else -> "??"
    }

}
