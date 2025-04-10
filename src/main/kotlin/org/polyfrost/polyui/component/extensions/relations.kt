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

package org.polyfrost.polyui.component.extensions

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.utils.fastEach

/**
 * Prioritize this component, meaning it will, in relation to its siblings:
 * - be drawn last (so visually on top)
 * - receive events first
 *
 * **this method is experimental as it may interfere with statically-typed references to children.**
 * @see relegate
 * @since 1.0.0
 */
@ApiStatus.Experimental
fun <S : Component> S.prioritize(): S {
    val children = _parent?.children ?: return this
    if (children.last() === this) return this
    children.remove(this)
    children.add(this)
    return this
}

/**
 * Relegate this component, meaning it will, in relation to its siblings:
 * - be drawn first (so visually on the bottom)
 * - receive events last
 *
 * **this method is experimental as it may interfere with statically-typed references to children.**
 * @see prioritize
 * @since 1.1.71
 */
@ApiStatus.Experimental
fun <S : Component> S.relegate(): S {
    val children = _parent?.children ?: return this
    if (children.first() === this) return this
    children.remove(this)
    children.add(0, this)
    return this
}

/**
 * Register this component to be a child of [parent].
 *
 * This allows for an [Elementa](https://github.com/EssentialGG/Elementa)-style syntax to be used for creating UIs.
 *
 * @since 1.7.27
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline infix fun <T : Component> T.childOf(parent: Component): T {
    parent.addChild(this)
    return this
}

/**
 * Locate a component by its name.
 *
 * This method is recursive, meaning it will search through all children of this component.
 * @param id the name of the drawable to locate.
 * @return the drawable with the given name, or `null` if it was not found.
 * @since 1.1.72
 */
fun <S : Component> Component.locate(id: String): S? {
    @Suppress("UNCHECKED_CAST")
    if (this.name == id) return this as S
    children?.fastEach {
        val res = it.locate<S>(id)
        if (res != null) return res
    }
    return null
}

/**
 * Returns `true` if this component is a child of the specified [component].
 * @see isRelatedTo
 * @since 1.4.2
 */
fun Component.isChildOf(component: Component?): Boolean {
    if (component == null) return false
    var p: Component? = this._parent
    while (p != null) {
        if (p === component) return true
        p = p._parent
    }
    return false
}

/**
 * Returns `true` if this component is a child of the specified [component], or if the [component] is a child of this.
 * @see isChildOf
 * @since 1.4.2
 */
fun Component.isRelatedTo(component: Component?) = component != null && component.isChildOf(this) || this.isChildOf(component)

/**
 * Returns a count of this component's children, including children of children.
 * @since 1.0.1
 */
fun Component.countChildren(): Int {
    var i = children?.size ?: 0
    children?.fastEach {
        i += it.countChildren()
    }
    return i
}

/**
 * @return `true` if this drawable has a child that intersects the provided rectangle.
 * @since 1.0.4
 */
fun Component.hasChildIn(x: Float, y: Float, width: Float, height: Float): Boolean {
    val children = this.children ?: return false
    children.fastEach {
        if (!it.renders) return@fastEach
        if (it.intersects(x, y, width, height)) return true
    }
    return false
}
