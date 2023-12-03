/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

/** This file contains various utilities for arrays and LinkedLists. */
@file:Suppress("ReplaceManualRangeWithIndicesCalls", "ReplaceSizeZeroCheckWithIsEmpty", "Invisible_Member", "Invisible_Reference")
@file:JvmName("ArrayUtils")

package org.polyfrost.polyui.utils

/**
 * Moves the given element from the [from] index to the [to] index.
 *
 * **Note**: this method makes absolutely no attempt to verify if the given
 * indices are valid.
 *
 * @param from the index of the element to move
 * @param to the index to move the element to
 */
@kotlin.internal.InlineOnly
inline fun <E> Array<E>.moveElement(from: Int, to: Int) {
    val item = this[from]
    this[from] = this[to]
    this[to] = item
}

/**
 * Return this collection as an LinkedList. **Note:** if it is already an LinkedList, it will be returned as-is.
 */
@kotlin.internal.InlineOnly
inline fun <T> Collection<T>.asLinkedList(): LinkedList<T> = if (this is LinkedList) this else LinkedList(this)

inline fun <T, R : Comparable<R>> LinkedList<T>.sortBy(crossinline selector: (T) -> R?) {
    if (size > 1) java.util.Collections.sort(this as MutableList<T>, compareBy(selector))
}

@kotlin.internal.InlineOnly
inline fun <T> Array<T>.asLinkedList(): LinkedList<T> = LinkedList(*this)
