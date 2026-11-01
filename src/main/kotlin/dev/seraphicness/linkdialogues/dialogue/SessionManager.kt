package dev.seraphicness.linkdialogues.dialogue

import dev.seraphicness.linkdialogues.LinkDialogues
import dev.seraphicness.linkdialogues.config.HotbarMode
import dev.seraphicness.linkdialogues.config.InputType
import dev.seraphicness.linkdialogues.config.Style
import org.bukkit.Input
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SessionManager(private val plugin: LinkDialogues) : Listener {
    private val sessions = ConcurrentHashMap<UUID, DialogueSession>()
    private var task: BukkitTask? = null
    private var tick = 0L

    fun start() {
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            tick++
            for (s in sessions.values.toList()) {
                runCatching { s.tick() }.onFailure {
                    plugin.logger.warning("대화 표시 중 오류 (${s.player.name}): ${it.message}")
                    it.printStackTrace()
                }
            }
        }, 1L, 1L)
    }

    fun stop() {
        task?.cancel()
        sessions.values.toList().forEach { it.close() }
        sessions.clear()
    }

    fun get(player: Player): DialogueSession? = sessions[player.uniqueId]

    fun open(player: Player, style: Style, host: DialogueHost, focus: Location?): DialogueSession {
        sessions.remove(player.uniqueId)?.close()
        val s = DialogueSession(plugin, player, style, host)
        sessions[player.uniqueId] = s
        s.open(focus)
        return s
    }

    fun remove(session: DialogueSession) {
        sessions.remove(session.player.uniqueId, session)
    }

    // ───────────────────────── input ─────────────────────────

    private fun click(player: Player, type: InputType?): Boolean {
        val s = get(player) ?: return false
        if (type != null && type !in plugin.settings.interaction) return true
        if (tick - s.lastClickTick < plugin.settings.clickCooldown) return true
        s.lastClickTick = tick
        s.onClick()
        return true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(e: PlayerInteractEvent) {
        if (get(e.player) == null) return
        e.isCancelled = true
        if (e.hand == EquipmentSlot.OFF_HAND) return
        when (e.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> click(e.player, InputType.RIGHT_CLICK)
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> click(e.player, InputType.LEFT_CLICK)
            else -> {}
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteractEntity(e: PlayerInteractEntityEvent) {
        if (get(e.player) == null) return
        e.isCancelled = true
        if (e.hand == EquipmentSlot.HAND && e !is PlayerInteractAtEntityEvent) click(e.player, InputType.RIGHT_CLICK)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAttack(e: EntityDamageByEntityEvent) {
        val p = e.damager as? Player ?: return
        if (get(p) == null) return
        e.isCancelled = true
        click(p, InputType.LEFT_CLICK)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onSwap(e: PlayerSwapHandItemsEvent) {
        if (get(e.player) == null) return
        e.isCancelled = true
        click(e.player, InputType.SWAP_HAND)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDrop(e: PlayerDropItemEvent) {
        if (get(e.player) != null) e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onScroll(e: PlayerItemHeldEvent) {
        val s = get(e.player) ?: return
        if (s.phase != DialogueSession.Phase.CHOOSING) return
        when (plugin.settings.hotbarMode) {
            HotbarMode.NONE -> return
            // slot change is allowed: the held slot itself is the selection
            HotbarMode.NUMBER -> e.isCancelled = s.onHotbar(e.newSlot)
            HotbarMode.SCROLL -> {
                e.isCancelled = true
                if (tick - s.lastScrollTick < plugin.settings.scrollCooldown) return
                s.lastScrollTick = tick
                s.onScroll(isDown(e.previousSlot, e.newSlot))
            }
        }
    }

    /** W/S (forward/back) move the selection, Space confirms. Works even while movement is locked. */
    @EventHandler
    fun onMoveKeys(e: PlayerInputEvent) {
        val s = get(e.player) ?: return
        val prev = s.lastInput
        val now = e.input
        s.lastInput = now
        if (!plugin.settings.selectKeys || s.phase != DialogueSession.Phase.CHOOSING) return
        fun pressed(key: (Input) -> Boolean) = key(now) && (prev == null || !key(prev))
        when {
            pressed(Input::isForward) -> s.onScroll(false)
            pressed(Input::isBackward) -> s.onScroll(true)
            pressed(Input::isJump) -> click(e.player, null)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSneak(e: PlayerToggleSneakEvent) {
        if (!e.isSneaking || !plugin.settings.exitOnSneak) return
        get(e.player)?.onExit()
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        DialogueSession.removeEffect(e.player)
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        // safety net in case the server stopped mid-dialogue
        DialogueSession.removeEffect(e.player)
    }

    private fun isDown(from: Int, to: Int): Boolean {
        for (offset in 1..4) if ((from + offset) % 9 == to) return true
        return false
    }
}
