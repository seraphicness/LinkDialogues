package dev.seraphicness.linkdialogues

import dev.seraphicness.linkdialogues.config.PluginSettings
import dev.seraphicness.linkdialogues.config.Style
import dev.seraphicness.linkdialogues.pack.PackBuilder
import dev.seraphicness.linkdialogues.pack.PackData
import dev.seraphicness.linkdialogues.render.FrameRenderer
import dev.seraphicness.linkdialogues.render.FrameState
import dev.seraphicness.linkdialogues.render.RichText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.util.logging.Logger
import java.util.zip.ZipFile
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackLayoutTest {
    private val res = File("src/main/resources")
    private val log = Logger.getLogger("test")
    private val settings = PluginSettings.load(YamlConfiguration.loadConfiguration(File(res, "config.yml")))
    private val pack = PackData.load(File(res, "pack"), settings.pack, log)
    private val style = Style.load(File(res, "styles/default.yml"), log)

    /** Sum of advances, resolving each text run's font like the client would. */
    private fun advance(c: Component, font: net.kyori.adventure.key.Key?): Float {
        val f = c.font() ?: font
        var w = 0f
        if (c is TextComponent) c.content().codePoints().forEach { cp ->
            w += if (f == pack.defaultFont) {
                val s = String(Character.toChars(cp))
                pack.spaces.advances[s]?.toFloat()
                    ?: (pack.images.values + pack.arrows.values.flatten()).first { it.char == s }.advance.toFloat()
            } else pack.metrics.width(cp)
        }
        c.children().forEach { w += advance(it, f) }
        return w
    }

    @Test
    fun frameIsCentredOnDialogueBackground() {
        val mm = MiniMessage.miniMessage()
        val m = pack.metrics
        val lines = RichText.wrap(RichText.parse(mm.deserialize("안녕하세요 모험가님! <red>빨간 글씨</red> 와 English text 가 섞인 긴 대사입니다. 줄바꿈 테스트")), 189f, m)
        val answers = listOf("1. 좋아요", "2. 싫어요", "3. 다른 이야기").map { RichText.plainGlyphs(it) }
        val frame = FrameState(style, RichText.plainGlyphs("안내원"), null, lines, "우클릭으로 넘기기", answers, 1, 0)
        val c = FrameRenderer(pack).render(frame)
        val bg = pack.images.getValue("dialogue-background").advance
        assertEquals(-bg.toFloat(), advance(c, null), "net advance must be -background width")
        lines.forEach { assertTrue(RichText.width(it, m) <= 189f) }
        println("cap=${m.capHeight} bg=$bg lines=${lines.size} widths=${lines.map { RichText.width(it, m).roundToInt() }}")
    }

    @Test
    fun buildsPack() {
        val tmp = Files.createTempDirectory("ldpack").toFile()
        val server = File(tmp, "server").apply { File(this, "plugins/CraftEngine").mkdirs() }
        val r = PackBuilder(pack, File(server, "plugins/LinkDialogues"), server).build()
        ZipFile(r.zip).use { z ->
            val names = z.entries().toList().map { it.name }
            println(names.joinToString("\n"))
            assertTrue("assets/linkdialogues/font/line_1.json" in names)
            println(String(z.getInputStream(z.getEntry("assets/linkdialogues/font/answer_1.json")).readBytes()))
        }
        assertTrue(File(server, "plugins/CraftEngine/resources/linkdialogues/pack.yml").isFile)
        assertTrue(File(server, "plugins/CraftEngine/resources/linkdialogues/resourcepack/assets/linkdialogues/font/default.json").isFile)
        assertTrue(r.craftEngineChanged)
        val r2 = PackBuilder(pack, File(server, "plugins/LinkDialogues"), server).build()
        assertTrue(!r2.craftEngineChanged, "unchanged pack must not trigger a CraftEngine reload")
    }
}
