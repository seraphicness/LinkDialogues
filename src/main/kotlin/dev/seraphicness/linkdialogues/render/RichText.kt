package dev.seraphicness.linkdialogues.render

import dev.seraphicness.linkdialogues.pack.TextMetrics
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/** A single code point with the style that matters for the action-bar UI. */
data class RichGlyph(val cp: Int, val color: TextColor?, val bold: Boolean)

typealias RichLine = List<RichGlyph>

object RichText {
    private const val NEWLINE = '\n'.code

    fun plain(c: Component): String = PlainTextComponentSerializer.plainText().serialize(c)

    fun parse(c: Component): List<RichGlyph> {
        val out = ArrayList<RichGlyph>()
        val stack = ArrayDeque<Style>()
        stack.addLast(Style.empty())
        ComponentFlattener.basic().flatten(c, object : FlattenerListener {
            override fun pushStyle(style: Style) {
                stack.addLast(stack.last().merge(style, Style.Merge.Strategy.ALWAYS))
            }

            override fun component(text: String) {
                // legacy '§' codes that slipped through as plain text
                val comp = if (text.indexOf('§') >= 0) LegacyComponentSerializer.legacySection().deserialize(text) else null
                if (comp != null) {
                    val base = stack.last()
                    parse(comp).forEach { g ->
                        out += g.copy(color = g.color ?: base.color(), bold = g.bold || base.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE)
                    }
                    return
                }
                val st = stack.last()
                val bold = st.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE
                text.codePoints().forEach { cp -> if (cp != '\r'.code) out += RichGlyph(cp, st.color(), bold) }
            }

            override fun popStyle(style: Style) {
                if (stack.size > 1) stack.removeLast()
            }
        })
        return out
    }

    fun plainGlyphs(text: String): List<RichGlyph> = text.codePoints().toArray().map { RichGlyph(it, null, false) }

    fun width(line: List<RichGlyph>, m: TextMetrics): Float = line.fold(0f) { acc, g -> acc + m.width(g.cp, g.bold) }

    /** Greedy word wrap. Breaks at spaces when possible, otherwise between any two characters. */
    fun wrap(glyphs: List<RichGlyph>, maxWidth: Float, m: TextMetrics): List<RichLine> {
        val lines = ArrayList<RichLine>()
        var cur = ArrayList<RichGlyph>()
        var curW = 0f
        var lastSpace = -1
        for (g in glyphs) {
            if (g.cp == NEWLINE) {
                lines += trimEnd(cur); cur = ArrayList(); curW = 0f; lastSpace = -1; continue
            }
            val w = m.width(g.cp, g.bold)
            if (curW + w > maxWidth && cur.isNotEmpty()) {
                if (g.cp == ' '.code) { // break exactly here
                    lines += trimEnd(cur); cur = ArrayList(); curW = 0f; lastSpace = -1; continue
                }
                if (lastSpace > 0) {
                    val head = cur.subList(0, lastSpace).toList()
                    val tail = cur.subList(lastSpace + 1, cur.size).toList()
                    lines += trimEnd(head)
                    cur = ArrayList(tail)
                } else {
                    lines += trimEnd(cur)
                    cur = ArrayList()
                }
                curW = width(cur, m)
                lastSpace = -1
            }
            if (g.cp == ' '.code) {
                if (cur.isEmpty()) continue // no leading spaces on wrapped lines
                lastSpace = cur.size
            }
            cur += g
            curW += w
        }
        if (cur.isNotEmpty() || lines.isEmpty()) lines += trimEnd(cur)
        return lines
    }

    /** Cuts a line to [maxWidth], appending an ellipsis if something was removed. */
    fun truncate(line: RichLine, maxWidth: Float, m: TextMetrics): RichLine {
        if (width(line, m) <= maxWidth) return line
        val dots = RichGlyph('…'.code.takeIf { m.canDisplay(it) } ?: '.'.code, line.lastOrNull()?.color, false)
        val dotW = m.width(dots.cp) * (if (dots.cp == '.'.code) 3 else 1)
        val out = ArrayList<RichGlyph>()
        var w = 0f
        for (g in line) {
            val gw = m.width(g.cp, g.bold)
            if (w + gw + dotW > maxWidth) break
            out += g; w += gw
        }
        repeat(if (dots.cp == '.'.code) 3 else 1) { out += dots }
        return out
    }

    private fun trimEnd(l: List<RichGlyph>): RichLine {
        var end = l.size
        while (end > 0 && l[end - 1].cp == ' '.code) end--
        return l.subList(0, end).toList()
    }

    /** Replaces code points the font cannot render (they would show as missing boxes and break widths). */
    fun sanitize(glyphs: List<RichGlyph>, m: TextMetrics): List<RichGlyph> =
        glyphs.map { if (it.cp == NEWLINE || m.canDisplay(it.cp)) it else it.copy(cp = '?'.code) }

    /** Builds components for [line] in [font], grouping runs with identical style. */
    fun toComponent(line: List<RichGlyph>, font: Key, defaultColor: TextColor): Component {
        val root = Component.text().font(font).color(defaultColor).decoration(TextDecoration.BOLD, false)
        if (line.isEmpty()) return root.build()
        val sb = StringBuilder()
        var color: TextColor? = line.first().color
        var bold = line.first().bold
        fun flush() {
            if (sb.isEmpty()) return
            val t: TextComponent.Builder = Component.text().content(sb.toString()).color(color ?: defaultColor)
            if (bold) t.decoration(TextDecoration.BOLD, true)
            root.append(t)
            sb.setLength(0)
        }
        for (g in line) {
            if (g.color != color || g.bold != bold) { flush(); color = g.color; bold = g.bold }
            sb.appendCodePoint(g.cp)
        }
        flush()
        return root.build()
    }
}
