package mindustryX.features.ui

import arc.Core
import arc.graphics.Color
import arc.scene.ui.*
import arc.scene.ui.layout.Table
import arc.util.Align
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.graphics.Pal
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustryX.features.GithubAccelerationService
import mindustryX.features.GithubProxyConfig

/**
 * Dialog for configuring GitHub acceleration proxies
 */
class GithubAccelerationDialog : BaseDialog("GH 加速配置") {
    
    private val contentTable = Table()
    
    init {
        addCloseButton()
        rebuild()
        
        shown(this::rebuild)
    }
    
    private fun rebuild() {
        cont.clear()
        cont.top()
        
        // Header
        cont.table(Tex.button) { header ->
            header.defaults().pad(4f).height(40f)
            
            header.add("序号").width(50f)
            header.add("启用").width(50f)
            header.add("镜像地址/备注").minWidth(200f).growX()
            header.add("Asset").width(60f)
            header.add("API").width(60f)
            header.add("操作").width(120f)
        }.growX().row()
        
        // Separator
        cont.image().color(Pal.accent).fillX().height(3f).pad(4f).row()
        
        // Proxy list
        val scrollPane = cont.pane { pane ->
            contentTable.clear()
            GithubAccelerationService.proxies.forEachIndexed { index, proxy ->
                buildProxyRow(contentTable, proxy, index)
            }
        }.grow().get()
        
        scrollPane.setScrollingDisabled(true, false)
        
        cont.row()
        
        // Separator
        cont.image().color(Pal.accent).fillX().height(3f).pad(4f).row()
        
        // Add button
        cont.button("+ 添加", Styles.cleart) {
            showAddProxyDialog()
        }.fillX().height(50f).row()
        
        // Separator
        cont.image().color(Pal.accent).fillX().height(3f).pad(4f).row()
        
        // Footer message
        cont.add("修改配置后，请点击保存图标生效")
            .color(Color.yellow)
            .pad(8f)
            .row()
    }
    
    private fun buildProxyRow(table: Table, proxy: GithubProxyConfig, index: Int) {
        table.table(Tex.pane) { row ->
            row.defaults().pad(4f).height(40f)
            
            // Index
            row.add("${proxy.id}").width(50f)
            
            // Enabled checkbox
            row.check("", proxy.enabled) { enabled ->
                proxy.enabled = enabled
            }.width(50f).disabled(proxy.locked)
            
            // URL and name
            row.table { urlTable ->
                urlTable.add(proxy.name).left().row()
                urlTable.add("[gray]${proxy.url}[]").left().labelAlign(Align.left).row()
            }.minWidth(200f).growX().left()
            
            // Asset toggle
            row.check("", proxy.assetEnabled) { enabled ->
                proxy.assetEnabled = enabled
            }.width(60f).disabled(proxy.locked)
            
            // API toggle  
            row.check("", proxy.apiEnabled) { enabled ->
                proxy.apiEnabled = enabled
            }.width(60f).disabled(proxy.locked)
            
            // Action buttons
            row.table { actions ->
                // Lock icon for locked items
                if (proxy.locked) {
                    actions.image(Icon.lock).size(24f).pad(4f)
                } else {
                    // Delete button
                    actions.button(Icon.trash, Styles.cleari, 24f) {
                        Vars.ui.showConfirm("@confirm", "确定要删除这个代理吗？") {
                            GithubAccelerationService.removeProxy(proxy.id)
                            rebuild()
                        }
                    }.pad(4f)
                    
                    // Save button
                    actions.button(Icon.save, Styles.cleari, 24f) {
                        GithubAccelerationService.updateProxy(proxy)
                        Vars.ui.showInfoFade("已保存")
                    }.pad(4f)
                }
            }.width(120f)
            
        }.growX().pad(2f).row()
    }
    
    private fun showAddProxyDialog() {
        val dialog = BaseDialog("添加代理")
        
        var url = ""
        var name = ""
        var assetEnabled = true
        var apiEnabled = true
        
        dialog.cont.table { t ->
            t.defaults().pad(4f).left()
            
            t.add("镜像地址:").row()
            t.field("https://") { url = it }.growX().row()
            
            t.add("备注名称:").row()
            t.field("") { name = it }.growX().row()
            
            t.table { checks ->
                checks.check("Asset", assetEnabled) { assetEnabled = it }.padRight(20f)
                checks.check("API", apiEnabled) { apiEnabled = it }
            }.row()
        }
        
        dialog.buttons.defaults().size(200f, 50f).pad(4f)
        dialog.buttons.button("@cancel") { dialog.hide() }
        dialog.buttons.button("@ok") {
            if (url.isNotEmpty()) {
                val cleanUrl = url.trim().trimEnd('/')
                val displayName = name.ifEmpty { cleanUrl }
                
                GithubAccelerationService.addProxy(
                    url = cleanUrl,
                    name = displayName,
                    assetEnabled = assetEnabled,
                    apiEnabled = apiEnabled
                )
                
                rebuild()
                dialog.hide()
            } else {
                Vars.ui.showInfo("请输入镜像地址")
            }
        }
        
        dialog.show()
    }
}
