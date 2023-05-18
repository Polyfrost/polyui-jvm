/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.input

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.utils.getResourceStreamNullable
import java.io.Serializable
import java.util.*

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
 *  The file contains a simple table of `key=value`, with each line meaning a new translation. We recommend using dot notation for your keys:
 *  ```
 *  my.key=hello there!
 *  hello.world=This works well!
 *  something.else=I can also substitute in {} objects using {}!
 *  ```
 */
class PolyTranslator(private val polyUI: PolyUI, private val translationDir: String) {
    private var currentFile: String = if (polyUI.renderer.settings.defaultLocale != null) {
        "$translationDir/${polyUI.renderer.settings.defaultLocale}.lang"
    } else {
        "$translationDir/${Locale.getDefault().language}_${Locale.getDefault().country}.lang"
    }
    private val map = HashMap<String, String>()

    /** set the locale of this translator. All the keys are cleared, and PolyUI is reloaded. */
    fun setLocale(locale: String) {
        currentFile = "$translationDir/$locale.lang"
        map.clear()
        polyUI.master.resetText()
    }

    /** Internal representation of a string in PolyUI.
     *
     * The string can be [translate]d if there is an attached [PolyTranslator] to this instance and the key is present in the file.
     * @see PolyTranslator
     */
    class Text(val key: String, private vararg val objects: Any?) : Cloneable, Serializable {
        var polyTranslator: PolyTranslator? = null
            set(value) {
                canTranslate = true
                field = value
            }

        /** the translated string. This value is automatically set to the translated value when retrieved. */
        var string: String = key
            get() {
                if (canTranslate && field == key) {
                    field = polyTranslator!!.translate(key, *objects)
                    if (field == key) canTranslate = false // key/file missing
                }
                return field
            }
            set(value) {
                if (!initialized) string.length // prompt it to initialize
                field = value
            }

        inline fun get() = string

        inline fun set(value: String) {
            string = value
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
            return Text(this.key, this.objects).also {
                polyTranslator = this.polyTranslator
            }
        }
    }

    /** translate the provided key, returning the key as per the translation table.
     *
     * If the key is not present in the table, then the [currentFile] file is checked for the key. The value is then returned.
     * Warnings will be issued if the file/key does not exist.
     * @throws IllegalArgumentException if multiple values exist for the same key.
     */
    fun translate(key: String, vararg objects: Any?): String {
        val k = "$key="
        return map[key] ?: with(
            getResourceStreamNullable(currentFile)?.bufferedReader()?.lines()?.filter { it.startsWith(k) }
                ?.toArray { arrayOfNulls<String>(it) }
        ) {
            if (this == null) {
                PolyUI.LOGGER.warn("No translation for $currentFile!")
                return key
            }
            if (isEmpty()) {
                PolyUI.LOGGER.warn("No value found for key '$key' in translation table $currentFile!")
                return key
            }
            if (size > 1) throw IllegalArgumentException("Multiple values found for key '$key' in translation table $currentFile! ${this.contentToString()}")
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
            if (i != objects.size) throw IllegalArgumentException("Found ${objects.size} object(s) for '$s'; but only ${i + 1} substitutions were present!")
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
