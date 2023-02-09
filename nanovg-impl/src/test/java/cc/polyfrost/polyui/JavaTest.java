/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui;

import cc.polyfrost.polyui.color.Color;
import cc.polyfrost.polyui.component.Drawable;
import cc.polyfrost.polyui.component.impl.Block;
import cc.polyfrost.polyui.component.impl.ImageBlock;
import cc.polyfrost.polyui.component.impl.Text;
import cc.polyfrost.polyui.event.Events;
import cc.polyfrost.polyui.layout.Layout;
import cc.polyfrost.polyui.layout.impl.FlexLayout;
import cc.polyfrost.polyui.property.impl.BlockProperties;
import cc.polyfrost.polyui.renderer.data.Image;
import cc.polyfrost.polyui.renderer.impl.GLWindow;
import cc.polyfrost.polyui.renderer.impl.NVGRenderer;
import cc.polyfrost.polyui.unit.Units;
import cc.polyfrost.polyui.unit.Vec2;

import java.util.ArrayList;

public class JavaTest {
    public static void main(String[] args) {
        GLWindow window = new GLWindow("Java Window", 800, 800);
        ArrayList<Drawable> things = new ArrayList<>(50);
        for (int i = 0; i < 51; i++) { // creates 50 rectangles with random sizes
            int finalI = i;
            things.add(new Block(Units.flex(), new Vec2<>(Units.pixels(Math.random() * 40 + 40), Units.pixels(Math.random() * 40 + 40)), new Events.MouseClicked(0).to(component -> {
                System.out.println("Mouse clicked! " + finalI);
            }), new Events.MouseClicked(0, 2).to(component -> {
                System.out.println("Mouse double-clicked! " + finalI);
            }), new Events.MouseClicked(1).to(component -> {
                System.out.println("Mouse right clicked! " + finalI);
            })));
        }
        PolyUI polyUI = new PolyUI(window.getWidth(), window.getHeight(), new NVGRenderer(), Layout.items(
                new Text("Java...         rainbow!     and image", Units.pixels(32), Units.times(Units.pixels(20), Units.pixels(570))),
                new Block(new BlockProperties(new Color.Gradient(new Color(1f, 0f, 0f), new Color(0f, 1f, 1f))), Units.times(Units.pixels(20), Units.pixels(600)), Units.times(Units.pixels(120), Units.pixels(120))),
                new Block(new BlockProperties(new Color.Chroma(Units.seconds(5))), Units.times(Units.pixels(200), Units.pixels(600)), Units.times(Units.pixels(120), Units.pixels(120))),
                new ImageBlock(new Image("/s.png", 120, 120), Units.times(Units.pixels(380), Units.pixels(600))),
                new FlexLayout(Units.times(Units.pixels(20), Units.pixels(30)), Units.percent(80), things.toArray(new Drawable[50]))));
        window.open(polyUI);
    }
}
