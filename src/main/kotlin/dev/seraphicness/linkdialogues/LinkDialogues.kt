package dev.seraphicness.linkdialogues

import dev.seraphicness.linkdialogues.bq.LinkConversationIO
import dev.seraphicness.linkdialogues.config.PluginSettings
import dev.seraphicness.linkdialogues.config.Style
import dev.seraphicness.linkdialogues.dialogue.DialogueHost
import dev.seraphicness.linkdialogues.dialogue.SessionManager
import dev.seraphicness.linkdialogues.pack.PackBuilder
import dev.seraphicness.linkdialogues.pack.PackData
import dev.seraphicness.linkdialogues.render.FrameRenderer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class LinkDialogues : JavaPlugin(), TabCompleter {
    private companion object {
        const val CONFIG_VERSION = 4
    }

    lateinit var settings: PluginSettings
        private set
    lateinit var pack: PackData
        private set
    lateinit var renderer: FrameRenderer
        private set
    lateinit var sessions: SessionManager
        private set
    private var styles: Map<String, Style> = emptyMap()
    private val mm = MiniMessage.miniMessage()

    override fun onEnable() {
        saveDefaults()
        loadAll()
        sessions = SessionManager(this)
        server.pluginManager.registerEvents(sessions, this)
        sessions.start()
        if (settings.pack.buildOnEnable) buildPack(null, reloadCraftEngine = false)
        registerBetonQuest()
    }

    private fun registerBetonQuest() {
        if (!server.pluginManager.isPluginEnabled("BetonQuest")) {
            logger.warning("BetonQuest 가 없거나 활성화되지 않아 conversationIO 를 등록하지 않았습니다. (/ldl preview, /ldl pack 은 사용 가능)")
            return
        }
        runCatching { LinkConversationIO.register(this, styles.keys) }
            .onSuccess { logger.info("BetonQuest conversationIO 등록: linkdialogues, " + styles.keys.joinToString { "linkdialogues_$it" }) }
            .onFailure { logger.severe("BetonQuest conversationIO 등록 실패 (BetonQuest 3.x 필요): $it") }
    }

    override fun onDisable() {
        if (::sessions.isInitialized) sessions.stop()
    }

    private fun saveDefaults() {
        saveDefaultConfig()
        val defaults = listOf(
            "styles/default.yml", "styles/dark.yml",
            "pack/layout.yml",
            "pack/fonts/Galmuri7.ttf", "pack/fonts/Galmuri9.ttf", "pack/fonts/Galmuri-LICENSE.txt",
            "pack/images/dialogue.png", "pack/images/answer.png", "pack/images/character.png", "pack/images/hand.png",
            "pack/images/fog.png", "pack/images/name_start.png", "pack/images/name_mid.png", "pack/images/name_end.png",
        )
        for (path in defaults) if (!File(dataFolder, path).exists()) saveResource(path, false)
        File(dataFolder, "pack/sounds").mkdirs()
        migrate()
    }

    /**
     * v1 → v2: 진행/선택 입력을 F키(SwapHand)로, 안내 문구의 '우클릭'을 'F키'로 변경.
     * v2 → v3: W/S 선택(select-keys), 핫바 모드(hotbar=Number) 추가.
     * v3 → v4: 안내 문구를 숫자키 기준으로 ('스크롤로 선택' / '숫자키·W/S로 선택' → '숫자키로 선택').
     */
    private fun migrate() {
        val cfgFile = File(dataFolder, "config.yml")
        val cfg = YamlConfiguration.loadConfiguration(cfgFile)
        val from = cfg.getInt("config-version", 1)
        if (from >= CONFIG_VERSION) return
        val replace = mutableListOf<Pair<String, String>>()
        if (from < 2) {
            cfg.set("Settings.interaction", listOf("SwapHand"))
            replace += "우클릭" to "F키"
        }
        if (from < 3) {
            if (!cfg.isSet("Settings.select-keys")) cfg.set("Settings.select-keys", true)
            if (!cfg.isSet("Settings.hotbar")) cfg.set("Settings.hotbar", "Number")
        }
        if (from < 4) {
            replace += "숫자키·W/S로 선택" to "숫자키로 선택"
            replace += "스크롤로 선택" to "숫자키로 선택"
        }
        cfg.set("config-version", CONFIG_VERSION)
        cfg.save(cfgFile)
        File(dataFolder, "styles").listFiles { f -> f.extension.equals("yml", true) }?.forEach { f ->
            val y = YamlConfiguration.loadConfiguration(f)
            val texts = y.getConfigurationSection("Texts") ?: return@forEach
            var changed = false
            for (k in texts.getKeys(false)) {
                var v = texts.getString(k) ?: continue
                for ((a, b) in replace) if (a in v) { v = v.replace(a, b); changed = true }
                texts.set(k, v)
            }
            if (changed) y.save(f)
        }
        logger.info("설정을 v$from → v$CONFIG_VERSION 으로 변환했습니다.")
    }

    private fun loadAll() {
        reloadConfig()
        settings = PluginSettings.load(config)
        pack = PackData.load(File(dataFolder, "pack"), settings.pack, logger)
        renderer = FrameRenderer(pack)
        val dir = File(dataFolder, "styles")
        styles = (dir.listFiles { f -> f.extension.equals("yml", true) } ?: emptyArray())
            .associate { it.nameWithoutExtension.lowercase() to Style.load(it, logger) }
        if (styles.isEmpty()) logger.warning("styles 폴더에 스타일이 없습니다.")
    }

    fun resolveStyle(fixed: String?, conversationFull: String, conversationName: String): Style {
        val name = fixed
            ?: settings.conversationStyles[conversationFull]
            ?: settings.conversationStyles[conversationName]
            ?: settings.defaultStyle
        return styles[name.lowercase()] ?: styles[settings.defaultStyle.lowercase()] ?: styles.values.first()
    }

    fun answerNumber(n: Int) = "$n. "

    private fun buildPack(sender: CommandSender?, reloadCraftEngine: Boolean) {
        runCatching { PackBuilder(pack, dataFolder, dataFolder.absoluteFile.parentFile.parentFile).build() }
            .onSuccess { r ->
                val path = r.craftEngineFolder?.path ?: r.zip.path
                sender?.let { msg(it, settings.message("pack-built").replace("{path}", path)) }
                logger.info("리소스팩 생성: ${r.zip.path}" + (r.craftEngineFolder?.let { " / CraftEngine: ${it.path}" } ?: ""))
                if (reloadCraftEngine && r.craftEngineChanged && settings.pack.craftEngine.reloadCommand.isNotBlank()) {
                    server.dispatchCommand(server.consoleSender, settings.pack.craftEngine.reloadCommand)
                }
            }
            .onFailure {
                logger.severe("리소스팩 생성 실패: ${it.message}")
                it.printStackTrace()
                sender?.let { s -> msg(s, settings.message("pack-failed").replace("{error}", it.message ?: it.toString())) }
            }
    }

    private fun msg(to: CommandSender, text: String) = to.sendMessage(mm.deserialize(settings.message("prefix") + text))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.firstOrNull()?.lowercase()) {
            "reload" -> {
                loadAll()
                registerBetonQuest()
                msg(sender, settings.message("reload"))
            }
            "pack" -> {
                loadAll()
                buildPack(sender, reloadCraftEngine = true)
            }
            "preview" -> {
                val p = sender as? Player ?: return true
                val styleName = args.getOrNull(1)?.lowercase() ?: settings.defaultStyle.lowercase()
                val style = styles[styleName] ?: run {
                    msg(sender, settings.message("unknown-style").replace("{style}", styleName)); return true
                }
                preview(p, style)
            }
            else -> msg(sender, settings.message("usage"))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> =
        when (args.size) {
            1 -> listOf("reload", "pack", "preview").filter { it.startsWith(args[0], true) }
            2 -> if (args[0].equals("preview", true)) styles.keys.filter { it.startsWith(args[1], true) } else emptyList()
            else -> emptyList()
        }

    /** Shows a sample dialogue without BetonQuest, useful for tuning offsets and the resource pack. */
    private fun preview(player: Player, style: Style) {
        lateinit var host: DialogueHost
        val name = mm.deserialize("<white>안내원</white>")
        val text = mm.deserialize(
            "안녕하세요! 이것은 <#2d87c3>LinkDialogues</#2d87c3> 미리보기입니다. " +
                "대사가 길면 자동으로 줄바꿈되고, 창보다 길면 다음 페이지로 넘어갑니다. " +
                "숫자키로 선택지를 고르고 F키로 결정하세요.",
        )
        val options = listOf("좋아요!", "<#e52b2b>별로예요</#e52b2b>", "다시 보여줘", "선택지가 많으면 스크롤됩니다")
            .map { mm.deserialize(it) }
        host = object : DialogueHost {
            override fun onAnswer(index: Int) {
                val s = sessions.get(player) ?: return
                when (index) {
                    2 -> s.show(name, text, options)
                    else -> s.show(name, mm.deserialize(if (index == 0) "감사합니다! <3" else "아쉽네요..."), emptyList())
                }
            }

            override fun onExitRequest(): Boolean {
                sessions.get(player)?.close(); return true
            }
        }
        val session = sessions.open(player, style, host, null)
        session.show(name, text, options)
        // emulate BetonQuest ending the conversation after a line without answers
        server.scheduler.runTaskTimer(this, { task ->
            val s = sessions.get(player)
            if (s == null || s !== session || s.closed) { task.cancel(); return@runTaskTimer }
            if (s.phase == dev.seraphicness.linkdialogues.dialogue.DialogueSession.Phase.FINAL) {
                s.requestEnd({}, force = false); task.cancel()
            }
        }, 1L, 1L)
    }
}
