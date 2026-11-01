package dev.seraphicness.linkdialogues.pack

import dev.seraphicness.linkdialogues.config.PackSettings
import net.kyori.adventure.key.Key
import org.bukkit.configuration.file.YamlConfiguration
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Vertical layout of a group of text rows. Row index is 0-based. */
data class LineSpec(val count: Int, val ascent: Int, val space: Int) {
    fun ascentOf(row: Int) = ascent - row * space
}

/**
 * One bitmap glyph in the `default` font.
 * [advance] mirrors Minecraft's BitmapProvider: round(inkWidth * scale) + 1.
 */
class ImageGlyph(
    val id: String,
    val char: String,
    val texturePath: String,
    val texture: BufferedImage,
    val height: Int,
    val ascent: Int,
    val advance: Int,
)

/** ±2^k pixel space glyphs; any integer offset is encoded as a short run of them. */
class SpaceGlyphs {
    private val positive = (0 until 13).map { (0xF800 + it).toChar().toString() }
    private val negative = (0 until 13).map { (0xF820 + it).toChar().toString() }
    private val cache = ConcurrentHashMap<Int, String>()

    val advances: Map<String, Int> = buildMap {
        for (k in 0 until 13) {
            put(positive[k], 1 shl k)
            put(negative[k], -(1 shl k))
        }
    }

    fun encode(px: Int): String {
        if (px == 0) return ""
        return cache.getOrPut(px) {
            val glyphs = if (px > 0) positive else negative
            val sb = StringBuilder()
            var rest = abs(px)
            for (k in 12 downTo 0) {
                val v = 1 shl k
                while (rest >= v) { sb.append(glyphs[k]); rest -= v }
            }
            sb.toString()
        }
    }
}

/** Text metrics that reproduce Minecraft's TTF provider advances. */
class TextMetrics(val awtFont: Font, val size: Float, val oversample: Float, capHeightOverride: Float) {
    private val pixelFont = awtFont.deriveFont(Math.round(size * oversample).toFloat())
    private val frc = FontRenderContext(null, false, false)
    private val widths = ConcurrentHashMap<Int, Float>()

    /** Distance from baseline to the top of capitals / hangul, in GUI pixels. */
    val capHeight: Float = if (capHeightOverride > 0) capHeightOverride else measureCapHeight()

    fun canDisplay(cp: Int) = cp == ' '.code || pixelFont.canDisplay(cp)

    fun width(cp: Int, bold: Boolean = false): Float {
        val base = widths.getOrPut(cp) {
            val s = String(Character.toChars(if (canDisplay(cp)) cp else '?'.code))
            val adv = pixelFont.createGlyphVector(frc, s).getGlyphMetrics(0).advanceX
            adv.roundToInt() / oversample // FreeType hinting rounds advances to whole raster pixels
        }
        return if (bold) base + 1f else base
    }

    fun width(text: String): Float {
        var w = 0f
        text.codePoints().forEach { w += width(it) }
        return w
    }

    /** Minecraft draws glyph tops at `7 - bearingTop`; TTF shift[1] is subtracted from bearingTop. */
    fun shiftFor(ascent: Int): Float = capHeight - ascent

    private fun measureCapHeight(): Float {
        val px = Math.round(size * oversample)
        val img = BufferedImage(px * 4, px * 4, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.font = pixelFont
        val base = px * 2
        g.drawString("A가H", px / 2, base)
        g.dispose()
        var top = base
        for (y in 0 until img.height) {
            if ((0 until img.width).any { (img.getRGB(it, y) ushr 24) != 0 }) { top = y; break }
        }
        return (base - top) / oversample
    }
}

class PackData(
    val settings: PackSettings,
    val dialogueLines: LineSpec,
    val answerLines: LineSpec,
    val nameAscent: Int,
    val infoAscent: Int,
    val images: Map<String, ImageGlyph>,
    /** arrow glyphs per image id → one glyph per answer row. */
    val arrows: Map<String, List<ImageGlyph>>,
    val sounds: Map<String, File>,
    val fontFile: File,
    val metrics: TextMetrics,
    val warnings: List<String>,
) {
    val spaces = SpaceGlyphs()
    val namespace get() = settings.namespace

    val defaultFont: Key = key("default")
    val nameFont: Key = key("name")
    val infoFont: Key = key("information")
    val dialogueFonts: List<Key> = (1..dialogueLines.count).map { key("line_$it") }
    val answerFonts: List<Key> = (1..answerLines.count).map { key("answer_$it") }

    fun key(path: String): Key = Key.key(namespace, path)

    companion object {
        fun load(packFolder: File, settings: PackSettings, log: Logger): PackData {
            val warnings = ArrayList<String>()
            val layout = YamlConfiguration.loadConfiguration(File(packFolder, "layout.yml"))
            fun spec(path: String, count: Int, ascent: Int, space: Int) = LineSpec(
                layout.getInt("$path.count", count).coerceIn(1, 12),
                layout.getInt("$path.ascent", ascent),
                layout.getInt("$path.space", space),
            )
            val dialogue = spec("Dialogue-Lines", 5, 25, 9)
            val answer = spec("Answer-Lines", 3, 75, 9)
            val nameAscent = layout.getInt("Character-Name.ascent", 40)
            val infoAscent = layout.getInt("Information-Line.ascent", -25)

            var nextChar = 0xE100
            val images = LinkedHashMap<String, ImageGlyph>()
            val arrows = LinkedHashMap<String, List<ImageGlyph>>()
            val section = layout.getConfigurationSection("Images")
            section?.getKeys(false)?.forEach { id ->
                val s = section.getConfigurationSection(id) ?: return@forEach
                val file = File(packFolder, "images/" + (s.getString("file") ?: "$id.png"))
                val src = runCatching { ImageIO.read(file) }.getOrNull()
                if (src == null) {
                    warnings += "이미지를 읽을 수 없음: ${file.path}"; return@forEach
                }
                val ratio = s.getDouble("reduction-ratio", 1.0).coerceAtLeast(0.01)
                val safeId = id.lowercase().replace(Regex("[^a-z0-9_.-]"), "_")
                if (s.getBoolean("is-arrow", false)) {
                    val shift = s.getInt("arrow-shift", 0)
                    arrows[id] = (0 until answer.count).map { row ->
                        makeGlyph(id, (nextChar++).toChar().toString(), "${safeId}_$row", src, ratio, answer.ascentOf(row) + shift)
                    }
                } else {
                    images[id] = makeGlyph(id, (nextChar++).toChar().toString(), safeId, src, ratio, s.getInt("ascent", 0))
                }
            }

            val sounds = LinkedHashMap<String, File>()
            layout.getConfigurationSection("Sounds")?.let { ss ->
                for (name in ss.getKeys(false)) {
                    val f = File(packFolder, "sounds/" + (ss.getString("$name.file") ?: "$name.ogg"))
                    if (f.isFile) sounds[name.lowercase()] = f else warnings += "사운드 파일 없음: ${f.path}"
                }
            }

            val fontFile = File(packFolder, "fonts/" + settings.font.file)
            val awt = runCatching { Font.createFont(Font.TRUETYPE_FONT, fontFile) }.getOrElse {
                throw IllegalStateException("폰트를 읽을 수 없음: ${fontFile.path} (${it.message})")
            }
            val metrics = TextMetrics(awt, settings.font.size, settings.font.oversample, settings.font.capHeight)
            warnings.forEach { log.warning(it) }
            return PackData(settings, dialogue, answer, nameAscent, infoAscent, images, arrows, sounds, fontFile, metrics, warnings)
        }

        /**
         * Pads the texture so that `ascent <= height` (a Minecraft requirement) while keeping scale = 1/ratio,
         * exactly like the original plugin's "image frame" trick.
         */
        private fun makeGlyph(id: String, ch: String, path: String, src: BufferedImage, ratio: Double, ascent: Int): ImageGlyph {
            val r = ratio
            val needed = ceil(max(src.height.toDouble(), ascent * r)).toInt()
            // texture height must make height = texH / r an integer
            var texH = needed
            while (abs(texH / r - Math.round(texH / r)) > 1e-6) texH++
            val tex = BufferedImage(src.width, texH, BufferedImage.TYPE_INT_ARGB)
            tex.createGraphics().apply { drawImage(src, 0, 0, null); dispose() }
            val height = Math.round(texH / r).toInt()
            var ink = 0
            for (x in src.width - 1 downTo 0) {
                if ((0 until src.height).any { (src.getRGB(x, it) ushr 24) != 0 }) { ink = x + 1; break }
            }
            val scale = height.toDouble() / texH
            val advance = (0.5 + ink * scale).toInt() + 1
            return ImageGlyph(id, ch, path, tex, height, ascent, advance)
        }
    }
}
