/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.unit

data class Box<T : Unit>(val point: Point<T>, val sized: Size<T>) {
    var x by point[0]::px
    var y by point[1]::px
    var width by sized[0]::px
    var height by sized[1]::px

    fun isInside(x: Float, y: Float): Boolean =
        x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height

    /** add the given amount of pixels to each edge of this box */
    fun expand(amount: Float): Box<T> {
        this.x -= amount
        this.y -= amount
        this.width += amount
        this.height += amount
        return this
    }
}