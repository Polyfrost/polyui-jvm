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

package cc.polyfrost.polyui.input

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.utils.getResourceStreamNullable
import java.util.Locale

/**
 * # PolyTranslator
 *
 * PolyTranslator is an i18n/l10n system used by PolyUI. It finds a language file according to the following spec:
 *
 *  `/`[translationDir]/[lang][java.util.Locale.getLanguage]`_`[COUNTRY][java.util.Locale.getCountry]`.lang`
 *
 *  for example `/`[translationDir]`/en_GB.lang`.
 *
 *  This can also be set with `-Dpolyui.locale="ll_CC"` or using [Settings.defaultLocale][cc.polyfrost.polyui.property.Settings.defaultLocale].
 *
 *  If the file cannot be found, PolyUI will check for a file named `ll_default.lang` (for example `en_default.lang`) and use that instead if present.
 *  If that isn't found, it will use the default locale (above), and if that isn't found it will crash.
 *
 *  Keys can also be added using
 *
 *  The file contains a simple table of `key=value`, with each line meaning a new translation. It will return the key if the file is found but the key is not present. We recommend using dot notation for your keys:
 *  ```
 *  my.key=hello there!
 *  hello.world=This works well!
 *  something.else=I can also substitute in {} objects using {}!
 *  ```
 */
class PolyTranslator(private val polyUI: PolyUI, private val translationDir: String) {
    private var resourcePath: String = if (System.getProperty("polyui.locale") != null) {
        "$translationDir/${System.getProperty("polyui.locale")}.lang"
    } else {
        "$translationDir/${Locale.getDefault().language}_${Locale.getDefault().country}.lang"
    }
    private val map = HashMap<String, String>()

    /** set the locale of this translator. All the keys are cleared, and PolyUI is reloaded. */
    fun setLocale(locale: String) {
        resourcePath = "$translationDir/$locale.lang"
        map.clear()
        polyUI.master.reset()
    }

    /**
     * Load a file of keys.
     * @see PolyTranslator
     * @see getResourceStreamNullable
     * @since 0.17.5
     */
    fun loadKeys(resource: String) {
        PolyUI.LOGGER.info("Loading translation table $resourcePath...")
        var i = 0
        getResourceStreamNullable(resource)?.bufferedReader()?.lines()?.forEach {
            val split = it.split("=")
            require(split.size == 2) { "Invalid translation table entry (line $i): $it" }
            map[split[0]] = split[1]
            i++
        }
    }

    /**
     * Add a map of keys to the translation table.
     *
     * @see loadKeys
     * @see PolyTranslator
     * @since 0.17.5
     */
    fun addKeys(keys: Map<out String, String>) = map.putAll(keys)

    /**
     * Add a set of keys to the translation table. You can use the Kotlin map syntax here:
     * ```
     * addKeys(
     *   "my.key" to "hello there!",
     *   "hello.world" to "This works well!",
     * )
     * ```
     * @see loadKeys
     * @see PolyTranslator
     * @since 0.17.5
     */
    fun addKeys(vararg pairs: Pair<String, String>) = map.putAll(pairs)

    /**
     * Add a key to the translation table.
     * @see addKeys
     * @see loadKeys
     * @since 0.17.5
     */
    fun addKey(key: String, value: String) {
        map[key] = value
    }

    /** Internal representation of a string in PolyUI.
     *
     * The string can be [translate]d if there is an attached [PolyTranslator] to this instance and the key is present in the file.
     * @see PolyTranslator
     */
    class Text(val key: String, private vararg val objects: Any?) : Cloneable {
        inline val length get() = string.length

        var polyTranslator: PolyTranslator? = null
            set(value) {
                canTranslate = true
                field = value
            }

        /** the translated string. This value is automatically set to the translated value when retrieved. */
        var string: String = key
            get() {
                @Suppress("ReplaceCallWithBinaryOperator")
                if (canTranslate && field.equals(key)) {
                    field = polyTranslator!!.translate(key, objects)
                    if (field == key) canTranslate = false // key/file missing
                }
                return field
            }
            set(value) {
                if (canTranslate && !initialized) string.length // prompt it to initialize
                field = value
            }

        /** reset this text according to the translation table (if attached) */
        fun reset() {
            if (canTranslate) string = key
            string.length
        }

        /** if this Text has an attached PolyTranslator, meaning it is able to attempt to translate. */
        var canTranslate = false
            private set

        /** if this Text has been initialized (it is not the key) */
        inline val initialized get() = string != key

        public override fun clone(): Text {
            return Text(this.key, *this.objects).also {
                polyTranslator = this.polyTranslator
            }
        }
    }

    /** translate the provided key, returning the key as per the translation table.
     *
     * If the key is not present in the table, then the [resourcePath] file is checked for the key. The value is then returned.
     * Warnings will be issued if the file/key does not exist.
     * @throws IllegalArgumentException if multiple values exist for the same key.
     */
    fun translate(key: String, objects: Array<out Any?>): String {
        val k = "$key="
        return map[key] ?: with(
            getResourceStreamNullable(resourcePath)?.bufferedReader()?.lines()?.filter { it.startsWith(k) }
                ?.toArray { arrayOfNulls<String>(it) }
        ) {
            if (this == null) {
                PolyUI.LOGGER.warn("No translation for $resourcePath!")
                val path = "${resourcePath.substring(0, resourcePath.lastIndex - 6)}default.lang"
                return if (getResourceStreamNullable(path) != null) {
                    PolyUI.LOGGER.warn("\t\t> Global language file found ($path), using that instead. Country-specific language features will be ignored. Use -Dpolyui.locale to set a locale.")
                    resourcePath = path
                    translate(key, objects)
                } else {
                    resourcePath = "$translationDir/${polyUI.settings.defaultLocale}.lang"
                    if (getResourceStreamNullable(resourcePath) == null) throw IllegalArgumentException("No global language file found ($path) or default language file found ($resourcePath)!")
                    PolyUI.LOGGER.warn("\t\t> No global language file found ($path) either, using default language instead. Use -Dpolyui.locale to set a locale.")
                    translate(key, objects)
                }
            }
            if (isEmpty()) {
                PolyUI.LOGGER.warn("No value found for key '$key' in translation table $resourcePath!")
                return key
            }
            if (size > 1) throw IllegalArgumentException("Multiple values found for key '$key' in translation table $resourcePath! ${this.contentToString()}")
            var s = this[0]!!.substring(k.length)
            var i = 0
            while (s.contains("{}")) {
                s = s.replace(
                    "{}",
                    objects.getOrNull(i)?.toString()
                        ?: throw IllegalArgumentException("Found ${i + 1} substitutions for '$s'; but only ${objects.size} object(s) to substitute!")
                )
                i++
            }
            if (i != objects.size) throw IllegalArgumentException("Found ${objects.size} object(s) for '$s'; but only ${i + 1} substitutions were present! (objs: ${objects.contentToString()})")
            map[key] = s
            return s
        }
    }

    companion object {
        /** Use {} in the key for the object substitution */
        @JvmStatic
        fun String.localised(vararg objects: Any?) = Text(this, *objects)

        @JvmStatic
        fun String.asPolyText(vararg objects: Any?) = Text(this, *objects)
    }
}

/** # [click here][PolyTranslator] */
typealias PolyText = PolyTranslator.Text
