/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.event.FocusedEvents

/** implement this in order to receive focus event, such as key presses */
interface Focusable {
    /** called when this component is focused */
    fun focus()

    /** called when this component is unfocused */
    fun unfocus()

    fun accept(event: FocusedEvents)
}