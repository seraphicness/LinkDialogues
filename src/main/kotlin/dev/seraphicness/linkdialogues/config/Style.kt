package dev.seraphicness.linkdialogues.config

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.logging.Logger

enum class MoveEffect { SLOWNESS, FREEZE, NONE }

data class SoundSpec(val sound: Sound) {
    fun play(player: Player) = player.playSound(sound, Sound.Emitter.self())

    companion object {
        fun parse(section: ConfigurationSection?, log: Logger, where: String): SoundSpec? {
            if (section == null) return null
            val id = section.getString("id")?.takeIf { it.isNotBlank() } ?: return null
            val key = runCatching { Key.key(id) }.getOrElse {
                log.warning("[$where] 잘못된 사운드 id: $id"); return null
            }
            val source = runCatching { Sound.Source.valueOf(section.getString("source", "MASTER")!!.uppercase()) }
                .getOrDefault(Sound.Source.MASTER)
            return SoundSpec(
                Sound.sound(key, source, section.getDouble("volume", 1.0).toFloat(), section.getDouble("pitch", 1.0).toFloat()),
            )
        }
    }
}

data class StyleOffsets(
    val name: Int,
    val nameBackground: Int,
    val dialogueBackground: Int,
    val dialogueLine: Int,
    val answerBackground: Int,
    val answerLine: Int,
    val arrow: Int,
    val character: Int,
    val information: Int,
)

data class StyleImages(
    val characterBackground: String,
    val arrow: String,
    val dialogueBackground: String,
    val answerBackground: String,
    val nameStart: String,
    val nameMid: String,
    val nameEnd: String,
    val fog: String,
)

data class StyleColors(
    val name: TextColor,
    val nameBackground: TextColor,
    val dialogue: TextColor,
    val dialogueBackground: TextColor,
    val answer: TextColor,
    val answerBackground: TextColor,
    val characterBackground: TextColor,
    val arrow: TextColor,
    val selected: TextColor,
    val fog: TextColor,
    val information: TextColor,
)

data class StyleTexts(
    val typingInfo: String,
    val steadyInfo: String,
    val nextInfo: String,
    val finalInfo: String,
)

data class Style(
    val name: String,
    val typingSpeed: Int,
    val typingSoundEvery: Int,
    val effect: MoveEffect,
    val answerNumbers: Boolean,
    val preventExit: Boolean,
    val preventSkip: Boolean,
    val showName: Boolean,
    val showCharacter: Boolean,
    val showFog: Boolean,
    val npcFocus: Boolean,
    val maxLineWidth: Int,
    val texts: StyleTexts,
    val typingSound: SoundSpec?,
    val selectionSound: SoundSpec?,
    val scrollSound: SoundSpec?,
    val offsets: StyleOffsets,
    val images: StyleImages,
    val portraits: Map<String, String>,
    val colors: StyleColors,
) {
    companion object {
        fun load(file: File, log: Logger): Style {
            val name = file.nameWithoutExtension
            val y = YamlConfiguration.loadConfiguration(file)
            val s = y.getConfigurationSection("Settings")
            val t = y.getConfigurationSection("Texts")
            val o = y.getConfigurationSection("Offsets")
            val i = y.getConfigurationSection("Images")
            val c = y.getConfigurationSection("Colors")
            fun color(key: String, def: String): TextColor = parseColor(c?.getString(key) ?: def) ?: run {
                log.warning("[$name] 잘못된 색상 Colors.$key"); parseColor(def)!!
            }
            fun img(key: String, def: String) = i?.getString(key)?.takeIf { it.isNotBlank() } ?: def
            fun off(key: String, def: Int) = o?.getInt(key, def) ?: def
            return Style(
                name = name,
                typingSpeed = (s?.getInt("typing-speed", 1) ?: 1).coerceAtLeast(0),
                typingSoundEvery = (s?.getInt("typing-sound-every", 1) ?: 1).coerceAtLeast(1),
                effect = when ((s?.getString("effect") ?: "Slowness").lowercase()) {
                    "freeze" -> MoveEffect.FREEZE
                    "none" -> MoveEffect.NONE
                    else -> MoveEffect.SLOWNESS
                },
                answerNumbers = s?.getBoolean("answer-numbers", true) ?: true,
                preventExit = s?.getBoolean("prevent-exit", false) ?: false,
                preventSkip = s?.getBoolean("prevent-skip", false) ?: false,
                showName = s?.getBoolean("character-name", true) ?: true,
                showCharacter = s?.getBoolean("character-image", true) ?: true,
                showFog = s?.getBoolean("background-fog", true) ?: true,
                npcFocus = s?.getBoolean("npc-focus", true) ?: true,
                maxLineWidth = s?.getInt("max-line-width", 0) ?: 0,
                texts = StyleTexts(
                    typingInfo = t?.getString("typing-info-line") ?: "",
                    steadyInfo = t?.getString("steady-info-line") ?: "",
                    nextInfo = t?.getString("next-info-line") ?: "",
                    finalInfo = t?.getString("final-info-line") ?: "",
                ),
                typingSound = SoundSpec.parse(y.getConfigurationSection("Sounds.typing"), log, name),
                selectionSound = SoundSpec.parse(y.getConfigurationSection("Sounds.selection"), log, name),
                scrollSound = SoundSpec.parse(y.getConfigurationSection("Sounds.scroll"), log, name),
                offsets = StyleOffsets(
                    name = off("name", 0),
                    nameBackground = off("name-background", 20),
                    dialogueBackground = off("dialogue-background", 0),
                    dialogueLine = off("dialogue-line", 10),
                    answerBackground = off("answer-background", 140),
                    answerLine = off("answer-line", 13),
                    arrow = off("arrow", -7),
                    character = off("character", -16),
                    information = off("information", 27),
                ),
                images = StyleImages(
                    characterBackground = img("character-background", "character-background"),
                    arrow = img("arrow", "hand"),
                    dialogueBackground = img("dialogue-background", "dialogue-background"),
                    answerBackground = img("answer-background", "answer-background"),
                    nameStart = img("name-start", "name-start"),
                    nameMid = img("name-mid", "name-mid"),
                    nameEnd = img("name-end", "name-end"),
                    fog = img("fog", "fog"),
                ),
                portraits = y.getConfigurationSection("Portraits").toStringMap(),
                colors = StyleColors(
                    name = color("name", "#4f4a3e"),
                    nameBackground = color("name-background", "#f8ffe0"),
                    dialogue = color("dialogue", "#4f4a3e"),
                    dialogueBackground = color("dialogue-background", "#f8ffe0"),
                    answer = color("answer", "#4f4a3e"),
                    answerBackground = color("answer-background", "#f8ffe0"),
                    characterBackground = color("character-background", "#ffffff"),
                    arrow = color("arrow", "#cdff29"),
                    selected = color("selected", "#2d87c3"),
                    fog = color("fog", "#000000"),
                    information = color("information", "#f8ffe0"),
                ),
            )
        }

        fun parseColor(raw: String): TextColor? {
            val v = raw.trim()
            return if (v.startsWith("#")) TextColor.fromHexString(v) else NamedTextColor.NAMES.value(v.lowercase())
        }
    }
}
