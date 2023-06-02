/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.utils.rgba

/**
 * The default light color set used in PolyUI.
 *
 * @see Colors
 * @since 0.17.0
 */
open class LightTheme : Colors {
    override val page = LightPage()
    override val brand = LightBrand()
    override val onBrand = LightOnBrand()
    override val state = LightState()
    override val component = LightComponent()
    override val text = LightText()

    open class LightPage : Colors.Page() {
        override val bg = rgba(232, 237, 255)
        override val bgElevated = rgba(222, 228, 252)
        override val bgDepressed = rgba(239, 243, 255)
        override val bgOverlay = rgba(0, 0, 0, 0.25f)

        override val fg = rgba(17, 23, 28)
        override val fgElevated = rgba(26, 34, 41)
        override val fgDepressed = rgba(14, 19, 23)
        override val fgOverlay = rgba(255, 255, 255, 0.1f)

        override val border20 = rgba(0, 0, 0, 0.2f)
        override val border10 = rgba(0, 0, 0, 0.1f)
        override val border5 = rgba(0, 0, 0, 0.05f)
    }
    open class LightBrand : Colors.Brand() {
        override val fg = rgba(64, 93, 255)
        override val fgHovered = rgba(40, 67, 221)
        override val fgPressed = rgba(57, 87, 255)
        override val fgDisabled = rgba(64, 93, 255, 0.5f)
        override val accent = rgba(223, 236, 253)
        override val accentHovered = rgba(183, 208, 251)
        override val accentPressed = rgba(177, 206, 255)
        override val accentDisabled = rgba(15, 28, 51, 0.5f)
    }
    open class LightOnBrand : Colors.OnBrand() {
        override val fg = rgba(213, 219, 255)
        override val fgHovered = rgba(215, 220, 251)
        override val fgPressed = rgba(225, 229, 255)
        override val fgDisabled = rgba(213, 219, 255, 0.5f)
        override val accent = rgba(63, 124, 228)
        override val accentHovered = rgba(63, 124, 228, 0.85f)
        override val accentPressed = rgba(37, 80, 154)
        override val accentDisabled = rgba(63, 124, 228, 0.5f)
    }
    open class LightState : Colors.State() {
        override val danger = rgba(255, 68, 68)
        override val dangerHovered = rgba(214, 52, 52)
        override val dangerPressed = rgba(255, 86, 86)
        override val dangerDisabled = rgba(255, 68, 68, 0.5f)
        override val warning = rgba(255, 171, 29)
        override val warningHovered = rgba(233, 156, 27)
        override val warningPressed = rgba(255, 178, 49)
        override val warningDisabled = rgba(255, 171, 29, 0.5f)
        override val success = rgba(35, 154, 96)
        override val successHovered = rgba(26, 135, 82)
        override val successPressed = rgba(44, 72, 110)
        override val successDisabled = rgba(35, 154, 96, 0.5f)
    }
    open class LightComponent : Colors.Component() {
        override val bg = rgba(222, 228, 252)
        override val bgHovered = rgba(213, 219, 243)
        override val bgPressed = rgba(238, 241, 255)
        override val bgDeselected = Color.TRANSPARENT
        override val bgDisabled = rgba(222, 228, 252, 0.5f)
    }
    open class LightText : Colors.Text() {
        override val primary = rgba(2, 3, 7)
        override val primaryHovered = rgba(11, 15, 33)
        override val primaryPressed = rgba(2, 5, 15)
        override val primaryDisabled = rgba(2, 3, 7, 0.5f)
        override val secondary = rgba(117, 120, 131)
        override val secondaryHovered = rgba(101, 104, 116)
        override val secondaryPressed = rgba(136, 139, 150)
        override val secondaryDisabled = rgba(117, 120, 131, 0.5f)
    }
}
