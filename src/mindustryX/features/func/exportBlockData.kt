@file:JvmName("FuncX")
@file:JvmMultifileClass

package mindustryX.features.func

import arc.Core
import mindustry.Vars
import mindustry.io.SaveVersion
import mindustry.ui.dialogs.BaseDialog
import mindustry.world.Block
import mindustryX.features.UIExt

@Suppress("unused")//js X.func.FuncX.exportBlockData()
fun exportBlockData() {
    val data = buildString {
        fun writeBlock(block: Block, name: String = block.name) {
            append(name)
            append(" ")
            append(if (block.synthetic()) "1" else "0")
            append(" ")
            append(if (block.solid) "1" else "0")
            append(" ")
            append(block.size.toString())
            append(" ")
            append((block.mapColor.rgba() ushr 8).toString())
            appendLine()
        }

        val allBlock = mutableListOf<Pair<String, Block>>()
        Vars.content.blocks().forEach { allBlock.add(it.name to it) }
        SaveVersion.fallback.forEach {
            Vars.content.block(it.value)
                ?.let { block -> allBlock.add(it.key to block) }
        }

        allBlock.sortedBy { it.second.name }
            .forEach { (key, block) -> writeBlock(block, key) }
    }
    BaseDialog("Export Block Data").apply {
        cont.add("Date Lines: ${data.lines().size}").row()

        addCloseButton()
        buttons.button("Copy to Clipboard") {
            Core.app.clipboardText = data
            UIExt.announce("Copied to Clipboard")
        }
        buttons.button("Save to File") {
            Vars.platform.showFileChooser(false, "Export Block Data", "dat") { file ->
                if (file == null) return@showFileChooser
                file.writeBytes(data.toByteArray())
            }
        }
        closeOnBack()
    }.show()
}