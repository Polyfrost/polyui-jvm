/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

package org.polyfrost.polyui.input.search

import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.levenshteinDistance

/**
 * A search algorithm.
 * @see search
 * @since 0.19.1
 */
fun interface SearchAlgorithm {
    /**
     * Search for objects that match the query.
     * @param query The query to search for.
     * @param objs The objects to search through.
     * @param output The list to output the results to. It will be empty.
     */
    fun search(query: String, objs: ArrayList<Any>, output: ArrayList<Any>)

    companion object {
        /**
         * A search algorithm using [String.contains] (ignores case).
         */
        @JvmField
        val contains = SearchAlgorithm { query, objs, output ->
            objs.fastEach {
                if (it.toString().contains(query, true)) {
                    output.add(it)
                }
            }
        }

        /**
         * A search algorithm using [String.equals] (ignores case).
         */
        @JvmField
        val equals = SearchAlgorithm { query, objs, output ->
            objs.fastEach {
                if (it.toString().equals(query, true)) {
                    output.add(it)
                }
            }
        }

        /**
         * A search algorithm using [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance).
         */
        @JvmField
        val levenshtein = SearchAlgorithm { query, objs, output ->
            objs.fastEach {
                if (it.toString().levenshteinDistance(query) <= 3) {
                    output.add(it)
                }
            }
        }
    }
}
