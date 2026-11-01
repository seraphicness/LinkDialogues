package dev.seraphicness.linkdialogues.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class PackResult(
    val zip: File,
    val folder: File,
    val craftEngineFolder: File?,
    /** true when the CraftEngine copy changed and CraftEngine should rebuild its pack. */
    val craftEngineChanged: Boolean,
    val copiedTo: File?,
)

/** Generates the resource pack (fonts, textures, sounds) described by [PackData]. */
class PackBuilder(private val data: PackData, private val dataFolder: File, private val serverRoot: File) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val ns = data.namespace

    fun build(): PackResult {
        val assets = collectAssets()

        val outRoot = File(dataFolder, "output")
        val folder = File(outRoot, data.settings.fileName)
        writeTree(folder, assets + ("pack.mcmeta" to mcmeta()))
        val zip = File(outRoot, data.settings.fileName + ".zip")
        writeZip(zip, assets + ("pack.mcmeta" to mcmeta()))

        var ceFolder: File? = null
        var ceChanged = false
        val ce = data.settings.craftEngine
        val cePlugin = File(serverRoot, "plugins/CraftEngine")
        if (ce.enabled && cePlugin.isDirectory) {
            ceFolder = File(cePlugin, "resources/${ce.folder}")
            val files = assets.mapKeys { "resourcepack/" + it.key } +
                ("pack.yml" to ceFolderPackYml().toByteArray())
            val stamp = File(ceFolder, ".linkdialogues-hash")
            val hash = hash(files)
            ceChanged = !stamp.isFile || stamp.readText() != hash
            if (ceChanged) {
                writeTree(ceFolder, files)
                stamp.writeText(hash)
            }
        }

        var copied: File? = null
        if (data.settings.copyTo.isNotBlank()) {
            copied = File(serverRoot, data.settings.copyTo)
            writeTree(copied, assets)
        }
        return PackResult(zip, folder, ceFolder, ceChanged, copied)
    }

    private fun collectAssets(): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        val base = "assets/$ns"

        // default font: space glyphs + images
        val providers = JsonArray()
        providers.add(JsonObject().apply {
            addProperty("type", "space")
            add("advances", JsonObject().apply { data.spaces.advances.forEach { (c, a) -> addProperty(c, a) } })
        })
        val glyphs = data.images.values + data.arrows.values.flatten()
        for (g in glyphs) {
            out["$base/textures/font/images/${g.texturePath}.png"] = png(g)
            providers.add(JsonObject().apply {
                addProperty("type", "bitmap")
                addProperty("file", "$ns:font/images/${g.texturePath}.png")
                addProperty("height", g.height)
                addProperty("ascent", g.ascent)
                add("chars", JsonArray().apply { add(g.char) })
            })
        }
        out["$base/font/default.json"] = json(JsonObject().apply { add("providers", providers) })

        // text fonts: one per row, only the vertical shift differs
        out["$base/font/text.ttf"] = data.fontFile.readBytes()
        fun textFont(ascent: Int) = json(JsonObject().apply {
            add("providers", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "ttf")
                    addProperty("file", "$ns:text.ttf")
                    addProperty("size", data.metrics.size)
                    addProperty("oversample", data.metrics.oversample)
                    add("shift", JsonArray().apply { add(0); add(data.metrics.shiftFor(ascent)) })
                })
            })
        })
        data.dialogueFonts.forEachIndexed { i, k -> out["$base/font/${k.value()}.json"] = textFont(data.dialogueLines.ascentOf(i)) }
        data.answerFonts.forEachIndexed { i, k -> out["$base/font/${k.value()}.json"] = textFont(data.answerLines.ascentOf(i)) }
        out["$base/font/${data.nameFont.value()}.json"] = textFont(data.nameAscent)
        out["$base/font/${data.infoFont.value()}.json"] = textFont(data.infoAscent)

        // sounds
        if (data.sounds.isNotEmpty()) {
            val sj = JsonObject()
            for ((name, file) in data.sounds) {
                out["$base/sounds/$name.ogg"] = file.readBytes()
                sj.add(name, JsonObject().apply { add("sounds", JsonArray().apply { add("$ns:$name") }) })
            }
            out["$base/sounds.json"] = json(sj)
        }
        return out
    }

    private fun mcmeta(): ByteArray = json(JsonObject().apply {
        add("pack", JsonObject().apply {
            addProperty("description", data.settings.description)
            addProperty("min_format", data.settings.minFormat)
            addProperty("max_format", data.settings.maxFormat)
        })
    })

    private fun ceFolderPackYml() = """
        |author: LinkDialogues
        |version: 1.0
        |description: ${data.settings.description}
        |namespace: $ns
        |enable: true
        |""".trimMargin()

    private fun png(g: ImageGlyph): ByteArray = ByteArrayOutputStream().also { ImageIO.write(g.texture, "png", it) }.toByteArray()

    private fun json(o: JsonObject) = gson.toJson(o).toByteArray(Charsets.UTF_8)

    private fun hash(files: Map<String, ByteArray>): String {
        val md = MessageDigest.getInstance("SHA-1")
        files.toSortedMap().forEach { (k, v) -> md.update(k.toByteArray()); md.update(v) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun writeTree(root: File, files: Map<String, ByteArray>) {
        if (root.exists()) root.deleteRecursively()
        for ((path, bytes) in files) {
            val f = File(root, path)
            f.parentFile.mkdirs()
            f.writeBytes(bytes)
        }
    }

    private fun writeZip(zip: File, files: Map<String, ByteArray>) {
        zip.parentFile.mkdirs()
        ZipOutputStream(zip.outputStream()).use { z ->
            for ((path, bytes) in files.toSortedMap()) {
                z.putNextEntry(ZipEntry(path))
                z.write(bytes)
                z.closeEntry()
            }
        }
    }
}
