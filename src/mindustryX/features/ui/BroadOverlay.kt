package mindustryX.features.ui

import arc.Core
import arc.Events
import arc.func.Prov
import arc.scene.ui.ScrollPane
import arc.scene.ui.layout.Table
import arc.util.Align
import arc.util.Strings
import mindustry.Vars
import mindustry.game.EventType.ResetEvent
import mindustry.game.EventType.WorldLoadEvent
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.ui.Styles
import mindustryX.features.UIExt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BroadOverlay {
    private const val prefix = "[#FEBBEF][]"
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private var content = ""
    private var updatedAt = 0L
    private var formattedUpdatedAt = ""
    private lateinit var window: OverlayUI.Window

    init {
        Events.on(ResetEvent::class.java) { clear() }
        Events.on(WorldLoadEvent::class.java) { clear() }
    }

    @JvmStatic
    fun init() {
        if (::window.isInitialized) return

        window = OverlayUI.registerWindow("broadOverlay", BroadOverlayTable()).apply {
            resizable = true
            availability = Prov { Vars.state.isGame && hasContent() }
        }
    }

    @JvmStatic
    fun tryHandle(message: String): Boolean {
        if (!message.startsWith(prefix)) return false

        content = message.removePrefix(prefix).trimStart().trimEnd()
        updatedAt = System.currentTimeMillis()
        formattedUpdatedAt = timeFormat.format(Date(updatedAt))
        UIExt.arcMessageDialog.addMsg(ArcMessageDialog.Msg(ArcMessageDialog.Type.serverMsg, content))

        if (!window.data.enabled && !window.data.pinned && window.data.value.center == null) {
            window.data.set(window.data.value.copy(enabled = true, pinned = true))
        }

        window.updateVisibility()
        if (window.visible && window.children.size == 0) window.rebuild()
        return true
    }

    fun hasContent(): Boolean = content.isNotEmpty()

    fun displayContent(): String = content

    fun displayUpdatedAt(): String = formattedUpdatedAt

    private fun clear() {
        content = ""
        updatedAt = 0L
        formattedUpdatedAt = ""
        if (::window.isInitialized) {
            window.updateVisibility()
        }
    }
}

private class BroadOverlayTable : Table(Tex.pane) {
    init {
        margin(6f)
        defaults().growX().left()

        table { header ->
            header.defaults().pad(2f)
            header.label { Core.bundle.get("settingV2.overlayUI.broadOverlay.name") }.style(Styles.outlineLabel).left().growX()
            header.label {
                BroadOverlay.displayUpdatedAt().takeIf(String::isNotEmpty)?.let { "[lightgray]$it[]" } ?: ""
            }.style(Styles.outlineLabel).right().padRight(4f)
            header.button(Icon.copySmall, Styles.clearNonei) {
                Core.app.setClipboardText(Strings.stripGlyphs(Strings.stripColors(BroadOverlay.displayContent())))
            }.tooltip("@copy")
        }.fillX().row()

        val content = Table().left().top()
        content.labelWrap {
            BroadOverlay.displayContent()
        }.left().growX()

        add(ScrollPane(content, Styles.noBarPane).apply {
            setFadeScrollBars(false)
            setScrollingDisabled(true, false)
        }).minSize(260f, 160f).size(360f, 220f).grow().row()

        add(OverlayUI.PreferAnyWidth()).fillX()
    }
}
