package org.polyfrost.polyui.utils

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_DISABLED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.countChildren
import org.polyfrost.polyui.component.impl.PopupMenu
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.seconds

fun PolyUI.addDebug(keybinds: Boolean): PolyUI {
    if (keybinds) {
        this.keyBinder?.add(
            KeyBinder.Bind('I', mods = mods(KeyModifiers.LCONTROL, KeyModifiers.LSHIFT)) {
                settings.debug = !settings.debug
                master.needsRedraw = true
                val s = if (settings.debug) "enabled" else "disabled"
                PolyUI.LOGGER.info("Debug mode $s")
                true
            },
        )
        this.keyBinder?.add(
            KeyBinder.Bind('P', mods = mods(KeyModifiers.LCONTROL)) {
                if (settings.debug) PolyUI.LOGGER.info(debugString())
                true
            },
        )
        this.keyBinder?.add(
            KeyBinder.Bind(chars = null, mouse = intArrayOf(0), mods = mods(KeyModifiers.LSHIFT), durationNanos = 0.4.seconds) {
                if (settings.debug) openDebugWindow(inputManager.rayCheckUnsafe(master, mouseX, mouseY))
                true
            }
        )
    }
    return this
}

fun PolyUI.openDebugWindow(d: Drawable?) {
    if (d == null) return
    val f = PolyUI.monospaceFont
    PopupMenu(
        Text(d.simpleName, font = f, fontSize = 16f),
        Text(
            """
parent: ${d.parent?.simpleName}   siblings: ${d.parent?.countChildren()?.dec()}
children: ${d.countChildren()};  fbo: null
at: ${d.x}x${d.y}
size: ${d.size} visible: ${d.visibleSize0}
rgba: ${d.color.r}, ${d.color.g}, ${d.color.b}, ${d.color.a}
scale: ${d.scaleX}x${d.scaleY};  skew: ${d.skewX}x${d.skewY}
rotation: ${d.rotation};  alpha: ${d.alpha}
redraw: ${d.needsRedraw};  ops=${d.operating};  scroll: ${d.scrolling}
state: ${getInputStateString(d.inputState)}
            """ + (d.debugString()?.let { "\n\n$it" } ?: ""),
            font = f
        ),
        align = Align(maxRowSize = 1),
        polyUI = this,
        position = Point.Below
    )
}

fun getInputStateString(state: Byte) = when (state) {
    INPUT_DISABLED -> "disabled"
    INPUT_NONE -> "none"
    INPUT_HOVERED -> "hovered"
    INPUT_PRESSED -> "pressed"
    else -> "??"
}
