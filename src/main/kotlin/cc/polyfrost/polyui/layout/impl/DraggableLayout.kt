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

class DraggableLayout(layout: Layout) : PointerLayout(layout) {
    init {
        layout.acceptsInput = true
    }

    private var mouseClickX = 0f
    private var mouseClickY = 0f
    private var mouseDown = false

    override fun reRenderIfNecessary() {
        if (mouseDown) {
            val movementX = polyui.eventManager.mouseX - mouseClickX - self.at.a.px
            val movementY = polyui.eventManager.mouseY - mouseClickY - self.at.b.px
            self.at.a.px = polyui.eventManager.mouseX - mouseClickX
            self.at.b.px = polyui.eventManager.mouseY - mouseClickY
            self.children.fastEach {
                it.at.a.px += movementX
                it.at.b.px += movementY
            }
            self.components.fastEach {
                it.at.a.px += movementX
                it.at.b.px += movementY
            }
            needsRedraw = true
        }
        super.reRenderIfNecessary()
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MousePressed) {
            mouseClickX = polyui.eventManager.mouseX - self.at.a.px
            mouseClickY = polyui.eventManager.mouseY - self.at.b.px
            mouseDown = true
        }
        if (event is Events.MouseReleased) {
            mouseDown = false
        }
        return false
    }
}