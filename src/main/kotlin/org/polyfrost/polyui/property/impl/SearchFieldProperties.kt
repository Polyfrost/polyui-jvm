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

package org.polyfrost.polyui.property.impl

import org.polyfrost.polyui.utils.levenshteinDistance

/**
 * @param searchAlgorithm the searching algorithm to use. It takes Any, the object, and String, the query, and returns a boolean.
 * True means that it matches, false means it does not.
 * By default, the search algorithm is a case-insensitive, combination fuzzy/substring search, utilizing [levenshteinDistance] and [String.contains].
 */
open class SearchFieldProperties(
    @Transient open val searchAlgorithm: Any.(String) -> Boolean = {
        if (it.isEmpty()) {
            true
        } else {
            val dist = 2
            val query = it.lowercase()
            val obj = this.toString().lowercase()
            if (obj.length <= dist) {
                obj.contains(query)
            } else {
                var similar = false
                for (word in obj.split(" ")) {
                    similar = word.contains(query) || word.levenshteinDistance(query) <= dist
                    if (similar) break
                }
                similar || query.contains(obj) || obj.levenshteinDistance(query) <= dist
            }
        }
    },
) : TextInputProperties()
