package com.logflare.android.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Every translatable string and plural of the app and the design system has a Korean version with
 * the same format arguments, and every Korean plural has the `other` quantity (the only one Korean
 * uses).
 */
class TranslationCompletenessTest {

    @Test
    fun app_hasKoreanForEveryString() = assertComplete(File("src/main/res"))

    @Test
    fun designSystem_hasKoreanForEveryString() = assertComplete(File("../core/designsystem/src/main/res"))

    private fun assertComplete(res: File) {
        val base = read(File(res, "values/strings.xml"))
        val ko = read(File(res, "values-ko/strings.xml"))
        val problems = mutableListOf<String>()
        base.strings.forEach { (name, text) ->
            val translated = ko.strings[name]
            when {
                translated == null -> problems += "missing string $name"
                args(translated) != args(text) -> problems += "string $name: args ${args(translated)} != ${args(text)}"
            }
        }
        base.plurals.forEach { (name, items) ->
            val translated = ko.plurals[name]
            when {
                translated == null -> problems += "missing plurals $name"
                "other" !in translated -> problems += "plurals $name: no 'other' quantity"
                else -> translated.values.forEach { text ->
                    if (args(text) != args(items.getValue("other"))) problems += "plurals $name: args differ"
                }
            }
        }
        (ko.strings.keys - base.strings.keys).forEach { problems += "extra string $it" }
        (ko.plurals.keys - base.plurals.keys).forEach { problems += "extra plurals $it" }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private class Strings(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun read(file: File): Strings {
        assertTrue("missing $file", file.isFile)
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = doc.getElementsByTagName("string").elements()
            .filter { it.getAttribute("translatable") != "false" }
            .associate { it.getAttribute("name") to it.textContent }
        val plurals = doc.getElementsByTagName("plurals").elements().associate { p ->
            p.getAttribute("name") to p.getElementsByTagName("item").elements()
                .associate { it.getAttribute("quantity") to it.textContent }
        }
        return Strings(strings, plurals)
    }

    private fun org.w3c.dom.NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }

    /** The positional format arguments, e.g. `[%1$s, %2$d]`. */
    private fun args(text: String): Set<String> = Regex("""%\d+\$[sd]""").findAll(text).map { it.value }.toSet()
}
