package dev.seraphicness.linkdialogues.dialogue

import dev.seraphicness.linkdialogues.LinkDialogues
import dev.seraphicness.linkdialogues.config.HotbarMode
import dev.seraphicness.linkdialogues.config.MoveEffect
import dev.seraphicness.linkdialogues.config.Style
import dev.seraphicness.linkdialogues.render.FrameState
import dev.seraphicness.linkdialogues.render.RichGlyph
import dev.seraphicness.linkdialogues.render.RichLine
import dev.seraphicness.linkdialogues.render.RichText
import net.kyori.adventure.text.Component
import org.bukkit.Input
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import kotlin.math.atan2
import kotlin.math.sqrt

/** Callbacks from the UI to whatever drives the conversation (BetonQuest or a preview). */
interface DialogueHost {
    /** Player picked answer [index] (0-based). */
    fun onAnswer(index: Int)

    /** Player asked to leave. Return false to refuse. */
    fun onExitRequest(): Boolean
}

class DialogueSession(
    private val plugin: LinkDialogues,
    val player: Player,
    val style: Style,
    private val host: DialogueHost,
) {
    enum class Phase { IDLE, TYPING, PAGE_DONE, CHOOSING, FINAL }

    var phase = Phase.IDLE
        private set
    var closed = false
        private set

    private val pack get() = plugin.pack
    private val renderer get() = plugin.renderer

    private var name: List<RichGlyph>? = null
    private var portrait: String? = null
    private var pages: List<List<RichLine>> = emptyList()
    private var pageIndex = 0
    private var revealed = 0
    private var pageLength = 0
    private var answers: List<RichLine> = emptyList()
    private var selected = 0
    private var windowStart = 0

    private var typingTimer = 0
    private var typedSinceSound = 0
    private var refreshTimer = 0
    private var dirty = true
    private var finalClicked = false
    private var pendingEnd: Runnable? = null
    private var finalTicks = 0

    var lastClickTick = -100L
    var lastScrollTick = -100L
    /** Last movement-key state received from the client (for W/S edge detection). */
    var lastInput: Input? = null

    private var lockApplied = false
    private var appliedSpeed: Double? = null
    private var originalSlot = -1
    private var slotSynced = false

    // ───────────────────────── lifecycle ─────────────────────────

    fun open(focus: Location?) {
        lastInput = player.currentInput
        updateControls()
        if (style.npcFocus && focus != null) face(focus)
    }

    /** Shows a new NPC text with the given options and starts typing. */
    fun show(npcName: Component?, text: Component, options: List<Component>) {
        if (closed) return
        val m = pack.metrics
        val plainName = npcName?.let { RichText.plain(it).trim() }.orEmpty()
        name = if (plainName.isEmpty()) null else RichText.sanitize(RichText.parse(npcName!!), m)
        portrait = style.portraits[plainName]

        val bgAdv = pack.images[style.images.dialogueBackground]?.advance ?: 200
        val maxW = (if (style.maxLineWidth > 0) style.maxLineWidth else bgAdv - style.offsets.dialogueLine * 2).toFloat()
        val lines = RichText.wrap(RichText.sanitize(RichText.parse(text), m), maxW, m)
        pages = lines.chunked(pack.dialogueLines.count).ifEmpty { listOf(emptyList()) }
        pageIndex = 0

        val abg = pack.images[style.images.answerBackground]
        val answerMax = ((abg?.advance ?: 130) - style.offsets.answerLine - 4).toFloat()
        answers = options.mapIndexed { i, opt ->
            val prefix = if (style.answerNumbers) plugin.answerNumber(i + 1) else ""
            val glyphs = RichText.plainGlyphs(prefix) + RichText.parse(opt).filter { it.cp != '\n'.code }
            RichText.truncate(RichText.sanitize(glyphs, m), answerMax, m)
        }
        selected = 0
        windowStart = 0
        finalClicked = false
        startPage()
    }

    /** Called between conversation cycles (after the player answered). */
    fun clearCycle() {
        answers = emptyList()
        pages = emptyList()
        phase = Phase.IDLE
        dirty = true
    }

    /**
     * BetonQuest wants to end the conversation. If the last NPC line has no answers, keep it on screen until the player
     * clicks (like the original plugin), unless [force].
     */
    fun requestEnd(callback: Runnable, force: Boolean) {
        if (closed) { callback.run(); return }
        val deferrable = !force && plugin.settings.waitFinalClick && player.isOnline &&
            answers.isEmpty() && pages.any { p -> p.any { it.isNotEmpty() } } && !finalClicked &&
            phase in setOf(Phase.TYPING, Phase.PAGE_DONE, Phase.FINAL)
        if (deferrable) {
            pendingEnd = callback
            finalTicks = 0
        } else {
            close()
            callback.run()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        removeEffect(player)
        if (originalSlot >= 0 && player.isOnline) player.inventory.heldItemSlot = originalSlot
        if (player.isOnline) player.sendActionBar(Component.empty())
        plugin.sessions.remove(this)
    }

    // ───────────────────────── ticking ─────────────────────────

    fun tick() {
        if (closed) return
        if (!player.isOnline) { closeWithPending(); return }
        updateControls()
        if (phase == Phase.TYPING) {
            if (style.typingSpeed <= 0) {
                revealAll()
            } else if (++typingTimer >= style.typingSpeed) {
                typingTimer = 0
                revealed++
                val cp = glyphAt(revealed - 1)
                if (cp != null && cp != ' '.code && ++typedSinceSound >= style.typingSoundEvery) {
                    typedSinceSound = 0
                    style.typingSound?.play(player)
                }
                if (revealed >= pageLength) finishPage()
                dirty = true
            }
        }
        if (pendingEnd != null && plugin.settings.finalAutoClose > 0 && phase == Phase.FINAL &&
            ++finalTicks >= plugin.settings.finalAutoClose
        ) {
            closeWithPending(); return
        }
        if (dirty || ++refreshTimer >= plugin.settings.refreshInterval) send()
    }

    private fun send() {
        dirty = false
        refreshTimer = 0
        if (phase == Phase.IDLE && pages.isEmpty()) {
            // keep the box (without text) visible between cycles to avoid flicker
            player.sendActionBar(renderer.render(frame(emptyList(), null)))
            return
        }
        val visible = visibleLines()
        val info = when (phase) {
            Phase.TYPING -> style.texts.typingInfo
            Phase.PAGE_DONE -> style.texts.nextInfo
            Phase.CHOOSING -> style.texts.steadyInfo
            Phase.FINAL -> style.texts.finalInfo
            Phase.IDLE -> null
        }
        player.sendActionBar(renderer.render(frame(visible, info)))
    }

    private fun frame(lines: List<RichLine>, info: String?) = FrameState(
        style = style,
        name = name,
        portrait = portrait,
        lines = lines,
        info = info?.takeIf { it.isNotEmpty() },
        answers = if (phase == Phase.CHOOSING) answers else emptyList(),
        selected = selected,
        windowStart = windowStart,
    )

    private fun visibleLines(): List<RichLine> {
        val page = pages.getOrNull(pageIndex) ?: return emptyList()
        if (phase != Phase.TYPING) return page
        var left = revealed
        val out = ArrayList<RichLine>()
        for (line in page) {
            if (left <= 0) break
            out += if (line.size <= left) line else line.subList(0, left)
            left -= line.size
        }
        return out
    }

    private fun glyphAt(index: Int): Int? {
        var i = index
        for (line in pages.getOrNull(pageIndex) ?: return null) {
            if (i < line.size) return line[i].cp
            i -= line.size
        }
        return null
    }

    private fun startPage() {
        revealed = 0
        typingTimer = 0
        typedSinceSound = 0
        pageLength = pages.getOrNull(pageIndex)?.sumOf { it.size } ?: 0
        phase = Phase.TYPING
        if (pageLength == 0 || style.typingSpeed <= 0) revealAll()
        dirty = true
    }

    private fun revealAll() {
        revealed = pageLength
        finishPage()
        dirty = true
    }

    private fun finishPage() {
        phase = when {
            pageIndex < pages.size - 1 -> Phase.PAGE_DONE
            answers.isNotEmpty() -> Phase.CHOOSING
            else -> Phase.FINAL
        }
        updateControls()
    }

    // ───────────────────────── input ─────────────────────────

    fun onClick() {
        if (closed) return
        when (phase) {
            Phase.TYPING -> if (!style.preventSkip) revealAll()
            Phase.PAGE_DONE -> { pageIndex++; startPage() }
            Phase.CHOOSING -> {
                if (answers.isEmpty()) return
                style.selectionSound?.play(player)
                val choice = selected
                clearCycle()
                host.onAnswer(choice)
            }
            Phase.FINAL -> {
                style.selectionSound?.play(player)
                finalClicked = true
                if (pendingEnd != null) closeWithPending()
            }
            Phase.IDLE -> {}
        }
    }

    fun onScroll(down: Boolean): Boolean {
        if (closed || phase != Phase.CHOOSING || answers.size < 2) return false
        val n = answers.size
        return selectIndex(if (down) (selected + 1) % n else (selected - 1 + n) % n)
    }

    /**
     * Hotbar input in number mode. The held hotbar slot *is* the selected answer (slot 1 = answer 1, ...), so number
     * keys and the mouse wheel both simply move the slot and work together without any guessing.
     * Returns true when the slot change must be cancelled (no answer on that slot: the wheel stops at the ends).
     */
    fun onHotbar(newSlot: Int): Boolean {
        if (closed || phase != Phase.CHOOSING) return false
        if (newSlot !in answers.indices) return true
        if (newSlot != selected) selectIndex(newSlot, toTop = true, syncSlot = false)
        return false
    }

    /**
     * Number-key selection: moves the cursor to answer [index] and scrolls it to the top of the answer window so the
     * hidden answers below it come into view (e.g. 4 answers / 3 rows: pressing 2 shows 2,3,4). F (or Space) confirms.
     */
    fun onNumber(index: Int) {
        if (closed || phase != Phase.CHOOSING || index !in answers.indices || index == selected) return
        selectIndex(index, toTop = true)
    }

    private fun selectIndex(index: Int, toTop: Boolean = false, syncSlot: Boolean = true): Boolean {
        val n = answers.size
        selected = index
        val rows = pack.answerLines.count
        if (n <= rows) windowStart = 0
        else if (toTop) windowStart = minOf(selected, n - rows)
        else if (selected < windowStart) windowStart = selected
        else if (selected >= windowStart + rows) windowStart = selected - rows + 1
        style.scrollSound?.play(player)
        // keep the held slot on the selected answer so number keys / wheel continue from here (W/S moves)
        if (syncSlot && slotSynced && selected < 9) player.inventory.heldItemSlot = selected
        dirty = true
        return true
    }

    fun onExit() {
        if (closed || style.preventExit) return
        if (pendingEnd != null) { closeWithPending(); return }
        host.onExitRequest()
    }

    private fun closeWithPending() {
        val cb = pendingEnd
        pendingEnd = null
        close()
        cb?.run()
    }

    // ───────────────────────── effects ─────────────────────────

    /**
     * Keeps the movement lock and the held hotbar slot in sync with the phase. While choosing with W/S the player is
     * fully frozen (otherwise W/S would walk); in number mode the held slot follows the selected answer.
     */
    fun updateControls() {
        if (closed) return
        val choosing = phase == Phase.CHOOSING
        val speed = if (choosing && plugin.settings.selectKeys) -1.0 else when (style.effect) {
            MoveEffect.SLOWNESS -> -0.85
            MoveEffect.FREEZE -> -1.0
            MoveEffect.NONE -> null
        }
        if (!lockApplied || speed != appliedSpeed) {
            lockApplied = true
            appliedSpeed = speed
            if (speed == null) removeEffect(player) else applyLock(speed)
        }
        if (!choosing || plugin.settings.hotbarMode != HotbarMode.NUMBER) {
            slotSynced = false
        } else if (!slotSynced) {
            // entering the answer list: put the held slot on the selected answer (slot 1 = answer 1)
            if (originalSlot < 0) originalSlot = player.inventory.heldItemSlot
            player.inventory.heldItemSlot = selected.coerceAtMost(8)
            slotSynced = true
        }
    }

    private fun applyLock(speed: Double) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
            it.removeModifier(MODIFIER_KEY)
            it.addTransientModifier(AttributeModifier(MODIFIER_KEY, speed, AttributeModifier.Operation.MULTIPLY_SCALAR_1))
        }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let {
            it.removeModifier(MODIFIER_KEY)
            it.addTransientModifier(AttributeModifier(MODIFIER_KEY, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1))
        }
    }

    private fun face(target: Location) {
        val eye = player.eyeLocation
        if (target.world != eye.world) return
        val dx = target.x - eye.x
        val dy = (target.y + 1.5) - eye.y
        val dz = target.z - eye.z
        val horizontal = sqrt(dx * dx + dz * dz)
        if (horizontal < 0.75) return
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(-atan2(dy, horizontal)).toFloat()
        player.setRotation(yaw, pitch)
    }

    companion object {
        val MODIFIER_KEY = NamespacedKey("linkdialogues", "dialogue_lock")

        fun removeEffect(player: Player) {
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.removeModifier(MODIFIER_KEY)
            player.getAttribute(Attribute.JUMP_STRENGTH)?.removeModifier(MODIFIER_KEY)
        }
    }
}
