package dev.seraphicness.linkdialogues.render

import dev.seraphicness.linkdialogues.config.Style
import dev.seraphicness.linkdialogues.pack.ImageGlyph
import dev.seraphicness.linkdialogues.pack.PackData
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.floor
import kotlin.math.roundToInt

/** Everything visible in one action-bar frame. */
class FrameState(
    val style: Style,
    val name: List<RichGlyph>?,
    val portrait: String?,
    val lines: List<RichLine>,
    val info: String?,
    val answers: List<RichLine>,
    val selected: Int,
    val windowStart: Int,
)

/**
 * Builds the action-bar component.
 *
 * Layout model (same as the original plugin): the dialogue background is drawn to the LEFT of the origin and the
 * cursor is left at its left edge, so the whole component has a net advance of `-backgroundWidth`. Minecraft centres
 * action-bar text by its total advance, which therefore puts the background exactly at the screen centre. Every other
 * element is placed relative to the background's left edge and rewinds the cursor after drawing.
 */
class FrameRenderer(private val pack: PackData) {
    private val m = pack.metrics

    fun render(f: FrameState): Component {
        val b = Builder()
        val st = f.style
        val o = st.offsets
        val c = st.colors

        // fog (net 0)
        if (st.showFog) pack.images[st.images.fog]?.let { fog ->
            val tile = (fog.advance - 1).coerceAtLeast(1)
            val n = (3072 + tile - 1) / tile
            val total = n * tile
            b.offset(-total / 2)
            repeat(n) { b.glyph(fog, c.fog); b.offset(-1) }
            b.offset(-(total - total / 2))
        }

        // dialogue background (net -A → cursor at background left edge)
        val bg = pack.images[st.images.dialogueBackground]
        val bgAdv = bg?.advance ?: 0
        if (bg != null) {
            b.offset(o.dialogueBackground - bgAdv)
            b.glyph(bg, c.dialogueBackground)
            b.offset(-o.dialogueBackground - bgAdv)
        }

        // name tag (net 0)
        if (st.showName && !f.name.isNullOrEmpty()) renderName(b, f.name, st)

        // character / portrait (net 0)
        if (st.showCharacter) {
            val img = f.portrait?.let { pack.images[it] } ?: pack.images[st.images.characterBackground]
            if (img != null) {
                b.offset(o.character)
                b.glyph(img, c.characterBackground)
                b.offset(-o.character - img.advance)
            }
        }

        // dialogue lines + information line (net 0)
        b.offset(o.dialogueLine)
        f.lines.take(pack.dialogueFonts.size).forEachIndexed { i, line ->
            b.text(line, pack.dialogueFonts[i], c.dialogue)
        }
        if (!f.info.isNullOrEmpty()) {
            b.offset(o.information)
            b.text(RichText.sanitize(RichText.plainGlyphs(f.info), m), pack.infoFont, c.information)
            b.offset(-o.information)
        }
        b.offset(-o.dialogueLine)

        // answers (net 0)
        if (f.answers.isNotEmpty()) renderAnswers(b, f)

        return b.build()
    }

    private fun renderName(b: Builder, name: List<RichGlyph>, st: Style) {
        val o = st.offsets
        val start = pack.images[st.images.nameStart]
        val mid = pack.images[st.images.nameMid]
        val end = pack.images[st.images.nameEnd]
        val w = RichText.width(name, m).roundToInt()
        b.offset(o.nameBackground)
        if (start != null && mid != null && end != null) {
            val midW = (mid.advance - 1).coerceAtLeast(1)
            val count = floor(w.toDouble() / midW).toInt()
            b.glyph(start, st.colors.nameBackground); b.offset(-1)
            repeat(count) { b.glyph(mid, st.colors.nameBackground); b.offset(-1) }
            b.glyph(end, st.colors.nameBackground); b.offset(-1)
            val drawn = (start.advance - 1) + (mid.advance - 1) * count + (end.advance - 1)
            b.offset(-drawn + 3)
        } else {
            b.offset(3)
        }
        b.offset(o.name)
        b.text(name, pack.nameFont, st.colors.name, rewind = false)
        b.offset(-o.name)
        b.offset(-w - 3)
        b.offset(-o.nameBackground)
    }

    private fun renderAnswers(b: Builder, f: FrameState) {
        val st = f.style
        val o = st.offsets
        val c = st.colors
        val abg = pack.images[st.images.answerBackground]
        b.offset(o.answerBackground)
        if (abg != null) {
            b.glyph(abg, c.answerBackground)
            b.offset(-abg.advance)
        }
        b.offset(o.answerLine)
        val arrows = pack.arrows[st.images.arrow]
        val rows = pack.answerFonts.size
        for (row in 0 until rows) {
            val idx = f.windowStart + row
            if (idx >= f.answers.size) break
            val selected = idx == f.selected
            if (selected && arrows != null) {
                val arrow = arrows[row]
                b.offset(-o.answerLine + o.arrow)
                b.glyph(arrow, c.arrow)
                b.offset(-o.arrow - arrow.advance + o.answerLine)
            }
            b.text(f.answers[idx], pack.answerFonts[row], if (selected) c.selected else c.answer, forceColor = selected)
        }
        b.offset(-o.answerLine)
        b.offset(-o.answerBackground)
    }

    private inner class Builder {
        private val children = ArrayList<Component>()
        private val pending = StringBuilder()

        fun offset(px: Int) {
            if (px != 0) pending.append(pack.spaces.encode(px))
        }

        private fun flushSpaces() {
            if (pending.isNotEmpty()) {
                children += Component.text(pending.toString())
                pending.setLength(0)
            }
        }

        fun glyph(g: ImageGlyph, color: TextColor) {
            flushSpaces()
            children += Component.text(g.char, color)
        }

        /** Draws [line] and, unless [rewind] is false, moves the cursor back to where it started. */
        fun text(line: List<RichGlyph>, font: Key, color: TextColor, rewind: Boolean = true, forceColor: Boolean = false) {
            if (line.isEmpty()) return
            val l = if (forceColor) line.map { it.copy(color = null) } else line
            flushSpaces()
            children += RichText.toComponent(l, font, color)
            if (rewind) offset(-RichText.width(line, m).roundToInt())
        }

        fun build(): Component {
            flushSpaces()
            return Component.text()
                .font(pack.defaultFont)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .shadowColor(ShadowColor.none())
                .append(children)
                .build()
        }
    }
}
