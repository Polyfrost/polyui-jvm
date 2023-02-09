/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.PolyUI

/** # Window
 * This class represents the physical window that PolyUI will render to.
 * As a rendering implementation, you must:
 *  - implement all the methods
 *  - create callbacks for the event methods in PolyUI e.g. [PolyUI.onResize]
 *  - create callbacks for all the event-related methods in [cc.polyfrost.polyui.event.EventManager]
 *  - call [open] to start the rendering loop; this can be blocking or non-blocking. Please note that after open is called, the rendering implementation will be created. This means that in a thread-based system such as LWJGL's OpenGL, you **must** ensure that it is fully setup before exiting `init {}`.
 */
abstract class Window(var width: Int, var height: Int) {
    abstract fun open(polyUI: PolyUI): Window

    /** Create the callbacks for window event.
     * @see Window */
    abstract fun createCallbacks()
}
