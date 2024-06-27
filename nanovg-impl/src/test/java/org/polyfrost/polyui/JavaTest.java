/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.org https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package org.polyfrost.polyui;

import org.polyfrost.polyui.unit.Align;
import org.polyfrost.polyui.unit.Vec2;

public class JavaTest {
	public static void main(String[] args) {
		long p = Vec2.of(32f, 12f);
		new Align(Align.Main.Center, Align.Cross.Center, Align.Mode.Horizontal, Vec2.of(32f, 12f), 50);
		//GLWindow window = new GLWindow("Java Window", 800, 800);
		//LinkedList<Drawable> things = new LinkedList<>(50);
		//for (int i = 0; i < 51; i++) { // creates 50 rectangles with random sizes
		//	int finalI = i;
		//	things.add(new Block(Units.flex(), new Vec2<>(Units.pixels(Math.random() * 40 + 40), Units.pixels(Math.random() * 40 + 40)), new MouseClicked(0).to(component -> {
		//		System.out.println("Mouse clicked! " + finalI);
		//	}), new MouseClicked(0, 2).to(component -> {
		//		System.out.println("Mouse double-clicked! " + finalI);
		//	}), new MouseClicked(1).to(component -> {
		//		System.out.println("Mouse right clicked! " + finalI);
		//	})));
		//}
		//PolyUI polyUI = new PolyUI("", new NVGRenderer(window.getWidth(), window.getHeight()), Layout.drawables(
		//		new Text(new TextProperties(), Translator.localised("Java...         rainbow!     and image"), Units.pixels(32), Units.times(Units.pixels(20), Units.pixels(570))),
        //        new Block(new BlockProperties(), Units.times(Units.pixels(20), Units.pixels(600)), Units.times(Units.pixels(120), Units.pixels(120))),
        //        new Block(new BlockProperties(), Units.times(Units.pixels(200), Units.pixels(600)), Units.times(Units.pixels(120), Units.pixels(120))),
		//		new Image(new PolyImage("/s.png", 120, 120), Units.times(Units.pixels(380), Units.pixels(600))),
		//		new FlexLayout(Units.times(Units.pixels(20), Units.pixels(30)), Units.percent(80), things.toArray(new Drawable[50]))));
		//window.open(polyUI);
	}
}
