package mindustryX.features.ui

import arc.Core
import arc.Events
import arc.func.Prov
import arc.scene.ui.layout.Table
import mindustry.Vars
import mindustry.game.EventType.ResetEvent
import mindustry.game.EventType.WorldLoadEvent
import mindustry.ui.Styles
import mindustryX.features.ui.AdsorptionSystem.Axis

/** 把服务器通过 `Call.infoPopup` 发送的积分榜转换为可固定的 Overlay 窗口。 */
object BroadOverlay {
    private val labelId = "scoreboard"

    var content = ""
        private set
    private lateinit var window: OverlayUI.Window

    init {
        Events.on(ResetEvent::class.java) { content = "" }
        Events.on(WorldLoadEvent::class.java) { content = "" }
    }

    @JvmStatic
    fun init() {
        if (::window.isInitialized) return

        window = OverlayUI.registerWindow("broadOverlay", BroadOverlayTable()).apply {
            availability = Prov { Vars.state.isGame && Vars.net.client() }
        }
    }

    /** 返回 true 表示已接管该 infoPopup，调用方不应再显示原弹窗。 */
    @JvmStatic
    fun tryHandleInfoPopup(message: String?, id: String?): Boolean {
        if (id != labelId) return false
        if (message == null) {
            content = ""
            return true
        }

        content = message.trim()

        // 首次收到时默认可见，并吸附到左侧、状态栏下方；用户改过窗口参数则保留用户设置
        if (!window.data.modified) {
            window.data.set(
                OverlayUI.WindowData(
                    enabled = true,
                    pinned = true,
                    constraintX = AdsorptionSystem.Constraint(Axis.X, "scene", AdsorptionSystem.ConstraintType.AlignLeading),
                    constraintY = AdsorptionSystem.Constraint(Axis.Y, "statusFrag", AdsorptionSystem.ConstraintType.AttachLeading),
                )
            )
        }
        return true
    }

    private class BroadOverlayTable : Table(Styles.black3) {
        init {
            // 无内容（占位模式）只在 Overlay 编辑状态显示
            visible { content.isNotEmpty() || OverlayUI.open }
            margin(4f)
            label { content.ifEmpty { Core.bundle.get("settingV2.overlayUI.broadOverlay.placeholder") } }
                .style(Styles.outlineLabel)
        }
    }
}
