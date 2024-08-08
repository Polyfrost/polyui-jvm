/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui.notify

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.image

class Notifications(private val polyUI: PolyUI, max: Int = 5) {
    private val current = arrayOfNulls<Component>(max)
    private val queue = ArrayList<Component>(5)
    private val PADDING = 8f
    private var lastY = 0f
    private var i = 0

    fun enqueue(vararg components: Component, progressFunc: Animation): Block {
        val out = Block(children = components, at = Vec2(polyUI.size.x + PADDING, 0f)).withBoarder().withStates()
        var failedToFinishNormally = false
        out.afterInit {
            val HEIGHT = (height / 12f).coerceAtMost(8f)
            val radii = out.radii
            val rad = if (radii == null) 0f else if (radii.size > 3) radii[2] else radii[0]
            addChild(
                Block(
                    at = Vec2(x, y + height - HEIGHT),
                    size = Vec2(0f, HEIGHT),
                    radii = floatArrayOf(0f, 0f, rad, rad),
                ).ignoreLayout().setPalette(polyUI.colors.brand.fg).also {
                    Resize(it, out.width, add = false, animation = progressFunc, onFinish = {
                        if (out.inputState > INPUT_NONE) {
                            failedToFinishNormally = true
                            return@Resize
                        }
                        finish(out)
                    }).add()
                }, recalculate = false
            )
        }
        out.apply {
            on(Event.Mouse.Exited) {
                if (failedToFinishNormally) finish(out)
                false
            }
        }
        out.setup(polyUI)
        queue.add(out)
        pop()
        return out
    }

    @JvmOverloads
    fun enqueueStandard(type: Type, title: String = type.title, description: String, durationNanos: Long) = enqueue(
        Block(
            Image(type.icon.image(Vec2(32f, 32f))),
            Text(title, fontSize = 14f).setFont { medium },
            Text(description, fontSize = 12f),
            size = Vec2(235f, 100f),
        ),
        progressFunc = Animations.Linear.create(durationNanos)
    )

    private fun pop() {
        if (queue.isEmpty() || i > current.size - 1) return
        val out = queue.removeAt(0)
        lastY -= out.height + PADDING
        out.y = polyUI.size.y + lastY
        Move(out, x = polyUI.size.x - out.width - PADDING, add = false, animation = Animations.Default.create(0.6.seconds)).add()
        current[i] = out
        i++
        polyUI.master.addChild(out, recalculate = false)
        polyUI.master.needsRedraw = true
    }

    enum class Type(val title: String, val icon: String) {
        Info("polyui.info", "polyui/info.svg"),
        Warning("polyui.warning", "polyui/warning.svg"),
        Error("polyui.error", "polyui/error.svg"),
        Success("polyui.success", "polyui/success.svg"),
    }

    private fun finish(old: Component) {
        i--
        current[0] = null
        lastY += old.height + PADDING
        for (i in current.indices) {
            val it = current[i] ?: continue
            Move(it, x = 0f, y = old.height + PADDING, add = true, animation = Animations.Default.create(0.5.seconds)).add()
        }
        pop()
        Move(old, x = polyUI.size.x + PADDING, add = false, animation = Animations.Default.create(0.5.seconds), onFinish = {
            parent.removeChild(this, recalculate = false)
        }).add()
    }
}
