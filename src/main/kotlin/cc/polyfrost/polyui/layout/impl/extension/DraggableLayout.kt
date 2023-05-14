/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl.extension

import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.utils.noneAre

/**
 * A layout that you can drag around.
 *
 * This is a so-called "extension layout", meaning that you apply it to an existing layout, like this:
 * `DraggableLayout(myLayout)` or using [myLayout.draggable()][Layout.draggable]
 */
class DraggableLayout(layout: Layout) : PointerLayout(layout) {
    init {
        layout.acceptsInput = true
        layout.simpleName += " [Draggable]"
    }

    private var mouseClickX = 0f
    private var mouseClickY = 0f
    private var mouseDown = false

    override fun reRenderIfNecessary() {
        if (mouseDown) {
            ptr.at.a.px = polyui.eventManager.mouseX - mouseClickX
            ptr.at.b.px = polyui.eventManager.mouseY - mouseClickY
        }
        super.reRenderIfNecessary()
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MousePressed) {
            mouseClickX = polyui.eventManager.mouseX - ptr.at.a.px
            mouseClickY = polyui.eventManager.mouseY - ptr.at.b.px
            mouseDown = components.noneAre { it.mouseOver } && children.noneAre { it.mouseOver }
        }
        if (event is Events.MouseReleased) {
            mouseDown = false
        }
        return super.accept(event)
    }
}
