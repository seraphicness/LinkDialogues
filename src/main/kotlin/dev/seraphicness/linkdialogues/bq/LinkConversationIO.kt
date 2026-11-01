package dev.seraphicness.linkdialogues.bq

import dev.seraphicness.linkdialogues.LinkDialogues
import dev.seraphicness.linkdialogues.config.Style
import dev.seraphicness.linkdialogues.dialogue.DialogueHost
import dev.seraphicness.linkdialogues.dialogue.DialogueSession
import net.kyori.adventure.text.Component
import org.betonquest.betonquest.BetonQuest
import org.betonquest.betonquest.api.profile.OnlineProfile
import org.betonquest.betonquest.conversation.Conversation
import org.betonquest.betonquest.conversation.ConversationIO
import org.betonquest.betonquest.conversation.ConversationIOFactory
import org.betonquest.betonquest.kernel.registry.feature.ConversationIORegistry
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import java.util.Collections

/**
 * BetonQuest conversation IO. BetonQuest calls the methods from both the main thread and async threads,
 * so every state change of the session is moved onto the main thread.
 */
class LinkConversationIO(
    private val plugin: LinkDialogues,
    private val conversation: Conversation,
    profile: OnlineProfile,
    private val style: Style,
) : ConversationIO, DialogueHost {
    private val player = profile.player

    @Volatile private var npcName: Component = Component.empty()
    @Volatile private var npcText: Component = Component.empty()
    private val options: MutableList<Component> = Collections.synchronizedList(ArrayList())
    private var session: DialogueSession? = null
    @Volatile private var exitRequested = false

    override fun begin() = sync {
        session = plugin.sessions.open(player, style, this, conversation.center)
    }

    override fun setNpcResponse(npcName: Component, response: Component) {
        this.npcName = npcName
        this.npcText = response
    }

    override fun addPlayerOption(option: Component, properties: ConfigurationSection) {
        options.add(option)
    }

    override fun display() = sync {
        val opts = synchronized(options) { options.toList() }
        if (Component.empty() == npcText && opts.isEmpty()) {
            end {}
            return@sync
        }
        val s = session ?: plugin.sessions.open(player, style, this, conversation.center).also { session = it }
        s.show(npcName, npcText, opts)
    }

    override fun clear() {
        options.clear()
        npcText = Component.empty()
        sync { session?.clearCycle() }
    }

    override fun end(callback: Runnable) = sync {
        val s = session
        if (s == null || s.closed) {
            callback.run()
        } else {
            s.requestEnd(callback, force = exitRequested || !player.isOnline)
        }
    }

    // ── DialogueHost ──

    override fun onAnswer(index: Int) {
        conversation.passPlayerAnswer(index + 1)
    }

    override fun onExitRequest(): Boolean {
        if (conversation.isMovementBlock) return false
        exitRequested = true
        conversation.endConversation()
        return true
    }

    private fun sync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block() else Bukkit.getScheduler().runTask(plugin, Runnable(block))
    }

    class Factory(private val plugin: LinkDialogues, private val fixedStyle: String?) : ConversationIOFactory {
        override fun parse(conversation: Conversation, onlineProfile: OnlineProfile): ConversationIO {
            val style = plugin.resolveStyle(fixedStyle, conversation.id.full, conversation.id.get())
            return LinkConversationIO(plugin, conversation, onlineProfile, style)
        }
    }

    companion object {
        const val IO_NAME = "linkdialogues"

        /** Registers `linkdialogues` and `linkdialogues_<style>` for every loaded style. */
        fun register(plugin: LinkDialogues, styles: Collection<String>) {
            val registry = BetonQuest.getInstance().componentLoader.get(ConversationIORegistry::class.java)
            registry.register(IO_NAME, Factory(plugin, null))
            for (style in styles) registry.register("${IO_NAME}_$style", Factory(plugin, style))
        }
    }
}
