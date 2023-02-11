/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.utils.fastEach
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
    }

    private var mouseClickX = 0f
    private var mouseClickY = 0f
    private var mouseDown = false

    override fun reRenderIfNecessary() {
        if (mouseDown) {
            val movementX = polyui.eventManager.mouseX - mouseClickX - ptr.at.a.px
            val movementY = polyui.eventManager.mouseY - mouseClickY - ptr.at.b.px
            ptr.at.a.px = polyui.eventManager.mouseX - mouseClickX
            ptr.at.b.px = polyui.eventManager.mouseY - mouseClickY
            ptr.children.fastEach {
                it.at.a.px += movementX
                it.at.b.px += movementY
            }
            ptr.components.fastEach {
                it.at.a.px += movementX
                it.at.b.px += movementY
            }
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
        return false
    }
}