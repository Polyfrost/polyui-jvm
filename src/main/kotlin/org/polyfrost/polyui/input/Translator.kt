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

package org.polyfrost.polyui.input

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.utils.getResourceStreamNullable
import java.text.MessageFormat
import java.util.Locale
import kotlin.collections.ArrayDeque

/**
 * # PolyTranslator
 *
 * PolyTranslator is an i18n/l10n system used by PolyUI. It finds a language file according to the following spec:
 *
 *  `/`[translationDir]/[lang][java.util.Locale.getLanguage]`_`[COUNTRY][java.util.Locale.getCountry]`.lang`
 *
 *  for example `/`[translationDir]`/en_GB.lang`.
 *
 *  This can also be set with `-Dpolyui.locale="ll_CC"` or using [Settings.defaultLocale][org.polyfrost.polyui.property.Settings.defaultLocale].
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
 *  something.else=I can also substitute in {0} objects using {1}!
 *  ```
 *
 *  @see MessageFormat
 */
class Translator(private val polyUI: PolyUI, private val translationDir: String) {
    private var resourcePath: String = if (System.getProperty("polyui.locale") != null) {
        "$translationDir/${System.getProperty("polyui.locale")}.lang"
    } else {
        "$translationDir/${Locale.getDefault().language}_${Locale.getDefault().country}.lang"
    }
    private val map = HashMap<String, String>()
    private val queue = ArrayDeque<String>()

    /**
     * Set to true to disable warnings when a translation is not found.
     */
    @ApiStatus.Internal
    var dontWarn: Boolean = false

    /** default file loading tracker */
    private var loadState = 0

    init {
        if (polyUI.settings.loadTranslationsOnInit) {
            loadKeys(resourcePath, true)
        }
    }

    /** set the locale of this translator. All the keys are cleared, and PolyUI is reloaded. */
    fun setLocale(locale: String) {
        resourcePath = "$translationDir/$locale.lang"
        map.clear()
        polyUI.master.reset()
    }

    /**
     * Add a file to the translation table, which will be used to load keys.
     *
     * Unless [now] is `true`, the file will only be loaded if the key is not found in the existing table.
     * @see Translator
     * @see getResourceStreamNullable
     * @since 0.17.5
     */
    fun loadKeys(resource: String, now: Boolean = false) {
        if (!now) {
            queue.add(resource)
        } else {
            polyUI.timed("Loading translation table $resource...") {
                val stream = getResourceStreamNullable(resource)?.bufferedReader()?.lines()
                if (polyUI.settings.parallelLoading) stream?.parallel()
                stream?.forEach {
                    if (it.isEmpty()) return@forEach
                    val split = it.split("=")
                    if (split.size == 2) {
                        if (map.put(split[0], split[1]) != null) PolyUI.LOGGER.warn("Duplicate key: '${split[0]}', overwriting with $resource -> ${split[1]}")
                    } else {
                        throw IllegalArgumentException("Invalid key-value pair in $resource: $it")
                    }
                } ?: PolyUI.LOGGER.warn("\t\t> Table not found!")
            }
        }
    }

    /**
     * Add a map of keys to the translation table.
     *
     * @see loadKeys
     * @see Translator
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
     * @see Translator
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
     * The string can be [translate]d if there is an attached [Translator] to this instance and the key is present in the file.
     * @see Translator
     */
    class Text(val key: String, @Transient vararg val objects: Any?) : Cloneable {
        inline val length get() = string.length

        @Transient
        var translator: Translator? = null
            set(value) {
                if (value === field) return
                canTranslate = key.isNotEmpty()
                field = value
            }

        /**
         * Weather this Text has objects to be substituted in.
         * @see MessageFormat
         * @since 0.21.1
         */
        inline val hasObjects get() = objects.isNotEmpty()

        /** the translated string. This value is automatically set to the translated value when retrieved. */
        var string: String = key
            get() {
                @Suppress("ReplaceCallWithBinaryOperator")
                if (canTranslate && field.equals(key)) {
                    field = translator!!.translate(this)
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
        @Transient
        var canTranslate = false
            private set

        /** if this Text has been initialized (it is not the key) */
        inline val initialized get() = string != key

        public override fun clone(): Text {
            return Text(this.key, *this.objects).also {
                translator = this.translator
            }
        }

        override fun toString() = string
    }

    /** translate the provided key, returning the key as per the translation table.
     *
     * If the key is not present in the table, then the [resourcePath] file is checked for the key. The value is then returned.
     * Warnings will be issued if the file/key does not exist.
     * @throws IllegalArgumentException if multiple values exist for the same key.
     */
    fun translate(text: Text): String {
        val key = text.key
        val line: String? = map[key] ?: run {
            while (queue.isNotEmpty()) {
                loadKeys(queue.removeFirst(), true)
                if (map.containsKey(key)) return@run map[key]
            }
            if (loadState == 0) {
                PolyUI.LOGGER.warn("No translation for '$key'! Attempting to load global file. Country-specific features will be ignored.")
                val s = "${resourcePath.substring(0, resourcePath.lastIndex - 6)}default.lang"
                loadKeys(s, true)
                loadState = if (s == "$translationDir/${polyUI.settings.defaultLocale}.lang") {
                    2 // asm: dodge double load if files are same
                } else {
                    1
                }
                if (map.containsKey(key)) return@run map[key]
            }
            if (loadState == 1) {
                PolyUI.LOGGER.warn("No translation for '$key'! Attempting to load default file. Locale features will be ignored.")
                loadKeys("$translationDir/${polyUI.settings.defaultLocale}.lang", true)
                loadState = 2
                if (map.containsKey(key)) return@run map[key]
            }
            null
        }
        if (line == null) {
            if (!dontWarn) PolyUI.LOGGER.warn("No translation for '$key'!")
            return key
        }
        if (text.hasObjects) {
            return MessageFormat.format(line, *text.objects)
        }
        return line
    }

    companion object {
        /** Use {idx} in the key for the object substitution
         * @see MessageFormat
         */
        @JvmStatic
        fun String.localised(vararg objects: Any?) = Text(this, *objects)
    }
}

/** # [click here][Translator] */
typealias PolyText = Translator.Text
