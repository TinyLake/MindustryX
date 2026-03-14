package mindustryX.features.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.*;
import mindustryX.features.*;
import mindustryX.features.ui.comp.*;

import java.util.*;

import static mindustry.Vars.*;
import static mindustryX.features.UIExt.i;

public class ReplayManagerDialog extends BaseDialog{
    private static final ReplayData unreadableMeta = new ReplayData(-1, new Date(0), "", "");

    private final GridTable list = new GridTable();
    private final ObjectMap<String, ReplayData> metaCache = new ObjectMap<>();
    private String search;

    public ReplayManagerDialog(){
        super(i("回放管理器"));

        cont.table(searchRow -> {
            searchRow.image(Icon.zoom).size(iconMed).padRight(6f);
            TextField searchField = searchRow.field(search, text -> {
                String value = text.trim().toLowerCase(Locale.ROOT);
                search = value.isEmpty() ? null : value;
            }).maxTextLength(80).growX().get();
            searchField.setMessageText(i("搜索回放"));
        }).growX().row();
        cont.pane(Styles.noBarPane, list).scrollX(false).pad(8f).grow().row();

        addCloseButton();
        buttons.button(i("加载外部回放"), Icon.upload, () -> {
            FileChooser.setLastDirectory(saveDirectory);
            platform.showFileChooser(true, i("打开回放文件"), ReplayController.extension, file -> Core.app.post(() -> {
                hide();
                ReplayController.startPlay(file);
            }));
        });
        buttons.button("@refresh", Icon.refresh, this::rebuildList);

        //lazy loading
        shown(() -> {
            if(list.hasChildren()) return;
            rebuildList();
        });
    }

    private void rebuildList(){
        list.clearChildren();
        list.top().defaults().minWidth(480f).growX().pad(4f);
        Seq<Fi> replayFiles = new Seq<>();
        for(Fi file : saveDirectory.list()){
            if(!file.isDirectory() && ReplayController.extension.equalsIgnoreCase(file.extension())){
                replayFiles.add(file);
            }
        }
        //TODO 优化这个性能
        replayFiles.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for(Fi file : replayFiles){
            var item = new ReplayItem(file);
            list.add(item).visible(() -> matchesSearch(item)).pad(4f);
        }
    }

    private boolean matchesSearch(ReplayItem item){
        if(search == null) return true;
        if(item.key.toLowerCase(Locale.ROOT).contains(search)) return true;

        ReplayData meta = item.meta;
        if(meta == null || meta == unreadableMeta) return false;
        return meta.getServerIp().toLowerCase(Locale.ROOT).contains(search)
        || meta.getRecordPlayer().toLowerCase(Locale.ROOT).contains(search);
    }

    private void addInfoPair(Table table, String key, String value){
        table.table(row -> {
            row.add(key).color(Color.lightGray);
            row.add(":").color(Color.lightGray).padRight(8f);
            row.labelWrap(value == null ? "" : value).color(Color.lightGray).growX().minWidth(0f);
        }).growX().pad(2f).row();
    }

    private class ReplayItem extends Table{
        private final Fi file;
        private final String key;
        private ReplayData meta = null;

        private ReplayItem(Fi file){
            super(Styles.grayPanel);
            this.file = file;
            this.key = file.absolutePath();
            loadMeta();
            rebuild();
        }

        void loadMeta(){
            ReplayData cache = metaCache.get(key);
            if(cache != null){
                meta = cache;
                return;
            }

            mainExecutor.execute(() -> {
                ReplayData meta = unreadableMeta;
                try(ReplayData.Reader reader = new ReplayData.Reader(file)){
                    meta = reader.getMeta();
                }catch(Exception e){
                    Log.warn("Failed to read replay meta for '@': @", file.name(), e.toString());
                }

                ReplayData finalMeta = meta;
                Core.app.post(() -> {
                    this.meta = finalMeta;
                    metaCache.put(key, finalMeta);
                    rebuild();
                });
            });
        }

        private void rebuild(){
            clearChildren();
            margin(4f);

            table(title -> {
                title.add(file.name()).color(Pal.accent).growX().left().wrap();
                buildActionButtons(title);
            }).growX().row();

            addInfoPair(this, i("修改时间"), FormatDefault.datetime(new Date(file.lastModified())));
            addInfoPair(this, i("文件大小"), FormatDefault.fileSize(file.length()));

            if(meta == null){
                add(i("读取回放头信息中...")).color(Color.lightGray).growX().wrap();
            }else if(meta == unreadableMeta){
                add(i("无法读取回放头信息")).color(Color.lightGray).growX().wrap();
            }else{
                addInfoPair(this, i("录制时间"), FormatDefault.datetime(meta.getTime()));
                addInfoPair(this, i("玩家"), meta.getRecordPlayer());
                addInfoPair(this, i("服务器"), meta.getServerIp());
                addInfoPair(this, i("版本"), String.valueOf(meta.getVersion()));
            }
        }

        private void buildActionButtons(Table buttons){
            boolean exists = file.exists();
            buttons.button(Icon.play, Styles.emptyi, iconMed, () -> {
                if(!file.exists()){
                    ui.showErrorMessage(VarsX.bundle.fileNotFound(file.name()));
                    doDelete();
                    return;
                }
                hide();
                ReplayController.startPlay(file);
            }).disabled(b -> !exists);
            buttons.button(Icon.trash, Styles.emptyi, iconMed, () -> confirmDelete(file)).padLeft(8f);
        }


        private void confirmDelete(Fi file){
            ui.showConfirm("@confirm", VarsX.bundle.confirmDeleteFile(file.name()), this::doDelete);
        }

        void doDelete(){
            file.delete();
            metaCache.remove(key);
            remove();
        }
    }
}
