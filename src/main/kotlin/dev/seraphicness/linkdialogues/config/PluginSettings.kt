package dev.seraphicness.linkdialogues.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

enum class InputType { RIGHT_CLICK, LEFT_CLICK, SWAP_HAND }

/** 핫바(숫자키/휠) 입력 처리 방식. */
enum class HotbarMode { SCROLL, NUMBER, NONE }

data class FontSettings(
    val file: String,
    val size: Float,
    val oversample: Float,
    val capHeight: Float,
)

data class CraftEngineSettings(
    val enabled: Boolean,
    val folder: String,
    val reloadCommand: String,
)

data class PackSettings(
    val namespace: String,
    val fileName: String,
    val description: String,
    val minFormat: Int,
    val maxFormat: Int,
    val buildOnEnable: Boolean,
    val copyTo: String,
    val craftEngine: CraftEngineSettings,
    val font: FontSettings,
)

data class PluginSettings(
    val defaultStyle: String,
    val interaction: Set<InputType>,
    val exitOnSneak: Boolean,
    val selectKeys: Boolean,
    val hotbarMode: HotbarMode,
    val scrollCooldown: Int,
    val clickCooldown: Int,
    val refreshInterval: Int,
    val waitFinalClick: Boolean,
    val finalAutoClose: Int,
    val conversationStyles: Map<String, String>,
    val pack: PackSettings,
    val messages: Map<String, String>,
) {
    fun message(key: String): String = messages[key] ?: key

    companion object {
        fun load(config: FileConfiguration): PluginSettings {
            val s = config.getConfigurationSection("Settings")
            val interaction = (s?.getStringList("interaction")?.takeIf { it.isNotEmpty() } ?: listOf("SwapHand"))
                .mapNotNull { parseInput(it) }.toSet() + InputType.SWAP_HAND // F키는 항상 사용 가능
            val p = config.getConfigurationSection("Pack")
            val f = p?.getConfigurationSection("font")
            val ce = p?.getConfigurationSection("craftengine")
            return PluginSettings(
                defaultStyle = s?.getString("default-style") ?: "default",
                interaction = interaction,
                exitOnSneak = !(s?.getString("exit") ?: "Sneak").equals("None", true),
                selectKeys = s?.getBoolean("select-keys", true) ?: true,
                hotbarMode = when ((s?.getString("hotbar") ?: "Number").lowercase()) {
                    "number", "numbers" -> HotbarMode.NUMBER
                    "none", "off" -> HotbarMode.NONE
                    else -> HotbarMode.SCROLL
                },
                scrollCooldown = s?.getInt("scroll-cooldown", 2) ?: 2,
                clickCooldown = s?.getInt("click-cooldown", 4) ?: 4,
                refreshInterval = (s?.getInt("refresh-interval", 10) ?: 10).coerceIn(1, 40),
                waitFinalClick = s?.getBoolean("wait-final-click", true) ?: true,
                finalAutoClose = s?.getInt("final-auto-close", 0) ?: 0,
                conversationStyles = config.getConfigurationSection("Conversation-Styles").toStringMap(),
                pack = PackSettings(
                    namespace = (p?.getString("namespace") ?: "linkdialogues").lowercase(),
                    fileName = p?.getString("file-name") ?: "LinkDialogues",
                    description = p?.getString("description") ?: "LinkDialogues UI",
                    minFormat = p?.getInt("min-format", 84) ?: 84,
                    maxFormat = p?.getInt("max-format", 999) ?: 999,
                    buildOnEnable = p?.getBoolean("build-on-enable", true) ?: true,
                    copyTo = p?.getString("copy-to") ?: "",
                    craftEngine = CraftEngineSettings(
                        enabled = ce?.getBoolean("enabled", true) ?: true,
                        folder = ce?.getString("folder") ?: "linkdialogues",
                        reloadCommand = ce?.getString("reload-command") ?: "ce reload pack",
                    ),
                    font = FontSettings(
                        file = f?.getString("file") ?: "Galmuri7.ttf",
                        size = (f?.getDouble("size", 8.0) ?: 8.0).toFloat(),
                        oversample = (f?.getDouble("oversample", 4.0) ?: 4.0).toFloat().coerceAtLeast(1f),
                        capHeight = (f?.getDouble("cap-height", -1.0) ?: -1.0).toFloat(),
                    ),
                ),
                messages = config.getConfigurationSection("Messages").toStringMap(),
            )
        }

        private fun parseInput(raw: String): InputType? = when (raw.lowercase().replace("_", "").replace("-", "")) {
            "rightclick", "rightclickentity", "rightclickplayer" -> InputType.RIGHT_CLICK
            "leftclick", "leftclickentity", "leftclickplayer" -> InputType.LEFT_CLICK
            "swaphand", "f" -> InputType.SWAP_HAND
            else -> null
        }
    }
}

fun ConfigurationSection?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return getKeys(false).associateWith { getString(it) ?: "" }
}
