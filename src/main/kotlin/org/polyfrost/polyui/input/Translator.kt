/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui.input

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.utils.dont
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.getResourceStreamNullable

/**
 * # PolyTranslator
 *
 * PolyTranslator is an i18n/l10n system used by PolyUI. It finds a language file according to the following spec:
 *
 *  `/`[translationDir]/[lang][java.util.Locale.getLanguage]`_`[COUNTRY][java.util.Locale.getCountry]`.lang`
 *
 *  for example `/`[translationDir]`/en_GB.lang`.
 *
 *  This can also be set with `-Dpolyui.locale="ll_CC"` or using [Settings.defaultLocale][org.polyfrost.polyui.Settings.defaultLocale].
 *
 *  If the file cannot be found, PolyUI will check for a file named `ll_default.lang` (for example `en_default.lang`) and use that instead if present.
 *  If that isn't found, it will use the default locale (above), and if that isn't found it will crash.
 *
 *  Keys can also be added using
 *
 *  The file contains a simple table of `my.key=value`, with each line meaning a new translation. It will return the key if the file is found but the key is not present. We recommend using dot notation for your keys:
 *  ```
 *  my.key=hello there!
 *  hello.world=This works well!
 *  something.else=I can also substitute in %s objects using %s!
 *  really.useful=I use the sprintf(3) type format strings as specified by java.lang.String.format().
 *  ```
 *
 *  @see java.util.Formatter
 */
class Translator(private val settings: Settings, private val translationDir: String, map: HashMap<String, Text>? = null) {
    private var resourcePath = System.getProperty("polyui.locale")?.let { "$translationDir/$it.lang" }
        ?: run {
            val locale = java.util.Locale.getDefault()
            "$translationDir/${locale.language}_${locale.country}.lang"
        }


    private val map = map ?: HashMap()
    private val queue = ArrayDeque<String>(4)
    private var delegates: ArrayList<Translator>? = null

    /**
     * add a *delegate* translator to this master translator.
     *
     * The specified [translationDir] will be used to load matching files of the current locale with keys.
     * @since 1.2.1
     */
    fun addDelegate(translationDir: String): Translator {
        val delegates = delegates ?: ArrayList(2)
        this.delegates = delegates
        delegates.add(Translator(settings, translationDir, map))
        return this
    }

    /**
     * Set to true to disable warnings when a translation is not found.
     */
    @ApiStatus.Internal
    var dontWarn: Boolean = false
        get() = field || !settings.debug

    /** default file loading tracker */
    private var loadState = 0

    init {
        if (settings.loadTranslationsOnInit) {
            loadKeys(resourcePath, true)
        }
    }

    /** set the locale of this translator. All the keys are cleared, and PolyUI is reloaded. */
    fun setLocale(locale: String): Translator {
        resourcePath = "$translationDir/$locale.lang"
        delegates?.fastEach { it.resourcePath = "${it.translationDir}/$locale.lang" }
        @Suppress("unchecked_cast") // kotlin moment
        val d = map.clone() as Map<String, Text>
        map.clear()
        d.forEach { (key, value) ->
            val new = if (value is Text.Formatted) translate(key, *value.args) else translate(key)
            value.string = new.string
            map[key] = value
        }
        return this
    }

    /**
     * Add a file to the translation table, which will be used to load keys.
     *
     * Unless [now] is `true`, the file will only be loaded if the key is not found in the existing table.
     *
     * **Note:** Although this function is public it is technically internal API.
     * using it directly probably won't work correctly when the locale is changed.
     * You probably meant to use [addDelegate] instead.
     * @see Translator
     * @see getResourceStreamNullable
     * @since 0.17.5
     */
    @ApiStatus.Internal
    fun loadKeys(resource: String, now: Boolean = false): Translator {
        if (!now) {
            queue.add(resource)
        } else {
            PolyUI.timed(true, "Loading translation table $resource...") {
                getResourceStreamNullable(resource)?.reader()?.forEachLine {
                    if (it.isEmpty()) return@forEachLine
                    val sep = it.indexOf('=')
                    // assertThat: contains('=') && !startsWith('=') && !endsWith('=')
                    require(sep > 0 && sep != it.length - 1) { "Invalid key-value pair in $resource: $it" }
                    val key = it.substring(0, sep).trim()
                    val value = it.substring(sep + 1)
                    val v = map[key]
                    if (v != null) {
                        PolyUI.LOGGER.warn("Duplicate key: '$key', overwriting with $resource -> $value")
                        v.string = value
                    } else {
                        require('.' in key && ' ' !in key) { "Invalid key $key: keys must contain at least one dot, and no spaces" }
                        map[key] = Text.Simple(value)
                    }
                } ?: PolyUI.LOGGER.warn("\t\t> Table not found!")
            }
        }
        return this
    }

    /**
     * Add a map of keys to the translation table.
     *
     * @see loadKeys
     * @see Translator
     * @since 0.17.5
     */
    fun addKeys(keys: Map<out String, String>): Translator {
        for ((k, v) in keys) {
            addKey(k, v)
        }
        return this
    }

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
    fun addKeys(vararg pairs: Pair<String, String>): Translator {
        for ((k, v) in pairs) {
            addKey(k, v)
        }
        return this
    }

    /**
     * Add a key to the translation table.
     * @see addKeys
     * @see loadKeys
     * @since 0.17.5
     */
    fun addKey(key: String, value: String): Translator {
        require('.' in key) { "Invalid key $key: keys must contain at least one dot" }
        val v = map[key]
        if (v == null) {
            map[key] = Text.Simple(value)
        } else {
            PolyUI.LOGGER.warn("Duplicate key: '$key', overwriting with $value")
            v.string = value
        }
        return this
    }

    interface Text {
        var string: String

        class Simple(override var string: String) : Text {
            override fun toString() = string
        }

        class Formatted(private val text: Text, vararg val args: Any?) : Text by text {
            init {
                try {
                    string = string.format(*args)
                } catch (e: Exception) {
                    PolyUI.LOGGER.error("Failed to format $string with ${args.contentToString()}!", e)
                }
            }

            override fun toString() = string
        }

        /**
         * Use this class to prevent the translation from occurring.
         * @since 1.1.63
         */
        class Dont(override var string: String) : Text {
            override fun toString() = string
        }
    }

    private fun translate0(key: String): Text? {
        while (queue.isNotEmpty()) {
            loadKeys(queue.removeFirst(), true)
            val v = map[key]
            if (v != null) return v
        }

        if (loadState == 0) {
            PolyUI.LOGGER.warn("No translation for '$key'! Attempting to load global file. Country-specific features will be ignored.")
            val s = "${resourcePath.substring(0, resourcePath.length - 7)}default.lang"
            loadKeys(s, true)
            loadState = if (s == "$translationDir/${settings.defaultLocale}.lang") {
                2 // asm: dodge double load if files are same
            } else {
                1
            }
            val v = map[key]
            if (v != null) return v
        }
        if (loadState == 1) {
            PolyUI.LOGGER.warn("No translation for '$key'! Attempting to load default file. Locale features will be ignored.")
            loadKeys("$translationDir/${settings.defaultLocale}.lang", true)
            loadState = 2
            val v = map[key]
            if (v != null) return v
        }
        return null
    }

    /** translate the provided key, returning the key as per the translation table.
     *
     * If the key is not present in the table, then the [resourcePath] file is checked for the key. The value is then returned.
     * Warnings will be issued if the file/key does not exist.
     * @throws IllegalArgumentException if multiple values exist for the same key.
     */
    fun translate(key: String): Text {
        if (key.isEmpty()) return Text.Simple("").dont()
        if ('.' !in key || ' ' in key) return Text.Simple(key).dont()
        val text = map[key] ?: run {
            val new = translate0(key)
            if (new != null) return new
            delegates?.fastEach {
                val delegated = it.translate0(key)
                if (delegated != null) return delegated
            }
            if (!dontWarn) PolyUI.LOGGER.warn("No translation for '$key'!")
            val dummy = Text.Simple(key)
            map[key] = dummy
            return dummy
        }
        return text
    }

    fun translate(key: String, vararg args: Any?) = Text.Formatted(translate(key), *args)
}

/** # [click here][Translator] */
typealias PolyText = Translator.Text
