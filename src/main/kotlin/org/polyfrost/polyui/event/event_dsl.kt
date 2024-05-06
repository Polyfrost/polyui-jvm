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

package org.polyfrost.polyui.event

import org.polyfrost.polyui.component.Drawable

/**
 * DSL for events.
 * @since 0.23.2
 */
class EventDSL<S : Drawable>(val self: S) {
    // kotlin bug: resolution error means self.run {} has to be used
    // target fix is set for 2.1.0
    // https://youtrack.jetbrains.com/issue/KT-63581/

    /** specify a handler for this event.
     *
     * in the given [handler], you can perform things on this component, such as recoloring and rotating
     *
     * Return true to consume the event. True by default.
     * @see then
     * @since 0.19.2
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun <E : Event> E.then(handler: S.(E) -> Boolean) = self.run {
        addEventHandler(this@then, handler)
    }

    /** specify a handler for this event.
     *
     * in the given [handler], you can perform things on this component, such as recoloring and rotating.
     *
     * Return true to consume the event. True by default.
     * @see then
     * @since 0.19.2
     * */
    @JvmName("to")
    @OverloadResolutionByLambdaReturnType
    infix fun <E : Event> E.then(handler: S.(E) -> Unit) = self.run {
        addEventHandler(this@then, handler)
    }

    /** marker class for preventing illegal nesting. */
    @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
    @DslMarker
    annotation class Marker
}
