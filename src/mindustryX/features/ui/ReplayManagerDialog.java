package mindustryX.features.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.*;
import mindustryX.features.*;
import mindustryX.features.ui.comp.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;
import static mindustryX.features.UIExt.i;

public class ReplayManagerDialog extends BaseDialog{
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ReplayData unreadableMeta = new ReplayData(-1, new Date(0), "", "");
    private static final float minCardWidth = 360f;
    private static final float minPaneHeight = 340f;

    private final GridTable list = new GridTable();
    private final ScrollPane pane = new ScrollPane(list, Styles.noBarPane);
    private final Seq<Fi> replayFiles = new Seq<>();
    private final ObjectMap<String, ReplayData> metaCache = new ObjectMap<>();
    private final ObjectSet<String> loadingMeta = new ObjectSet<>();
    private final ExecutorService metaLoader = Threads.executor("Replay Meta Loader", 1);

    private boolean rebuildPosted;
    private boolean portraitLayout;
    private String search;
    private String searchText = "";
    private TextField searchField;

    public ReplayManagerDialog(){
        super(i("回放管理器"));

        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        list.top().left().margin(6f);
        list.defaults().minWidth(minCardWidth).growX().pad(4f);

        Events.on(DisposeEvent.class, e -> metaLoader.shutdown());

        addCloseButton();

        shown(() -> {
            search = null;
            searchText = "";
            refreshReplayFiles();
            rebuildLayout();
        });
        resized(this::rebuildLayout);
    }

    private void rebuildLayout(){
        portraitLayout = Core.graphics.isPortrait();

        float availableWidth = Math.max(minCardWidth, Core.scene.getWidth() / Scl.scl() - 32f);
        float availableHeight = Math.max(minPaneHeight, Core.scene.getHeight() / Scl.scl() - (mobile ? 110f : 140f));

        cont.clearChildren();
        cont.top();
        cont.defaults().growX();

        buildToolbar();
        cont.add(pane).width(availableWidth).height(availableHeight).growX().row();

        cont.invalidateHierarchy();
        rebuildList();
    }

    private void buildToolbar(){
        if(portraitLayout){
            cont.table(searchRow -> {
                searchRow.defaults().height(46f);
                searchRow.image(Icon.zoom).padRight(6f);
                buildSearchField(searchRow);
            }).growX().row();

            cont.table(actionRow -> {
                actionRow.defaults().height(46f);
                actionRow.button(i("加载外部回放"), Icon.upload, this::loadExternalReplay).growX();
                actionRow.button(Icon.refresh, Styles.cleari, this::refreshAndRebuild).size(46f).padLeft(8f);
            }).growX().padTop(8f).row();
        }else{
            cont.table(toolbar -> {
                toolbar.defaults().height(46f);
                toolbar.image(Icon.zoom).padRight(6f);
                buildSearchField(toolbar);
                toolbar.button(i("加载外部回放"), Icon.upload, this::loadExternalReplay).padLeft(8f).padRight(8f);
                toolbar.button(Icon.refresh, Styles.cleari, this::refreshAndRebuild).size(46f);
            }).growX().row();
        }
    }

    private void buildSearchField(Table table){
        searchField = table.field(searchText, text -> {
            applySearchText(text);
            rebuildList();
        }).maxTextLength(80).growX().get();
        searchField.setMessageText(i("搜索回放"));
        searchField.setCursorPosition(searchText.length());
        searchField.setTextFieldListener((field, c) -> {
            if(c == '\n' || c == '\r'){
                applySearchText(field.getText());
                rebuildList();
            }
        });
    }

    private void rebuildList(){
        list.clearChildren();

        if(replayFiles.isEmpty()){
            list.add(i("没有可管理的回放文件")).pad(12f).color(Color.lightGray);
            return;
        }

        int shown = 0;
        for(Fi file : replayFiles){
            if(!matchesSearch(file)) continue;
            shown++;
            list.add(new ReplayItem(file));
        }

        if(shown == 0){
            list.add(i("没有匹配的回放文件")).pad(12f).color(Color.lightGray);
        }
    }

    private void refreshAndRebuild(){
        refreshReplayFiles();
        rebuildList();
    }

    private void applySearchText(String text){
        searchText = text;
        String value = text.trim().toLowerCase(Locale.ROOT);
        search = value.isEmpty() ? null : value;
    }

    private void refreshReplayFiles(){
        replayFiles.clear();
        replayFiles.addAll(scanReplayFiles());

        ObjectSet<String> aliveFiles = new ObjectSet<>();
        for(Fi file : replayFiles){
            aliveFiles.add(file.absolutePath());
        }

        Seq<String> staleKeys = new Seq<>();
        for(String key : metaCache.keys()){
            if(!aliveFiles.contains(key)) staleKeys.add(key);
        }
        for(String key : staleKeys){
            metaCache.remove(key);
        }

        staleKeys.clear();
        for(String key : loadingMeta){
            if(!aliveFiles.contains(key)) staleKeys.add(key);
        }
        for(String key : staleKeys){
            loadingMeta.remove(key);
        }
    }

    private Seq<Fi> scanReplayFiles(){
        Seq<Fi> files = new Seq<>();
        for(Fi file : saveDirectory.list()){
            if(!file.isDirectory() && "mrep".equalsIgnoreCase(file.extension())){
                files.add(file);
            }
        }
        files.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files;
    }

    private boolean matchesSearch(Fi file){
        if(search == null) return true;
        if(file.name().toLowerCase(Locale.ROOT).contains(search)) return true;

        ReplayData meta = metaCache.get(file.absolutePath());
        if(meta == null || meta == unreadableMeta) return false;
        return meta.getServerIp().toLowerCase(Locale.ROOT).contains(search)
        || meta.getRecordPlayer().toLowerCase(Locale.ROOT).contains(search);
    }

    private void buildBaseInfo(Table table, Fi file){
        table.left().top();
        table.defaults().growX();
        addInfoPair(table, i("修改时间"), formatDate(new Date(file.lastModified())));
        addInfoPair(table, i("文件大小"), formatSize(file.length()));
    }

    private void buildMetaInfo(Table table, ReplayData meta){
        table.left().top();
        table.defaults().growX();
        addInfoPair(table, i("录制时间"), formatDate(meta.getTime()));
        addInfoPair(table, i("玩家"), meta.getRecordPlayer());
        addInfoPair(table, i("服务器"), meta.getServerIp());
        addInfoPair(table, i("版本"), String.valueOf(meta.getVersion()));
    }

    private void addInfoPair(Table table, String key, String value){
        table.table(row -> {
            row.left().top();
            row.defaults().left().top();
            row.add("[lightgray]" + key + ":[]").padRight(6f);
            row.labelWrap(value == null ? "" : value).color(Color.lightGray).growX().minWidth(0f);
        }).growX().row();
    }

    private void buildActionButtons(Table buttons, Fi file){
        buttons.defaults().size(portraitLayout ? 42f : 36f);
        boolean exists = file.exists();
        buttons.button(Icon.play, Styles.emptyi, () -> playReplay(file)).disabled(b -> !exists);
        buttons.button(Icon.trash, Styles.emptyi, () -> confirmDelete(file)).padLeft(8f);
    }

    private void requestMeta(Fi file){
        String key = file.absolutePath();
        if(metaCache.containsKey(key) || loadingMeta.contains(key)) return;
        loadingMeta.add(key);

        metaLoader.execute(() -> {
            ReplayData meta = unreadableMeta;
            try(ReplayData.Reader reader = new ReplayData.Reader(file)){
                meta = reader.getMeta();
            }catch(Exception e){
                Log.warn("Failed to read replay meta for '@': @", file.name(), e.toString());
            }

            ReplayData finalMeta = meta;
            Core.app.post(() -> {
                loadingMeta.remove(key);
                metaCache.put(key, finalMeta);
                queueRebuild();
            });
        });
    }

    private void queueRebuild(){
        if(rebuildPosted) return;
        rebuildPosted = true;
        Core.app.post(() -> {
            rebuildPosted = false;
            rebuildList();
        });
    }

    private String formatDate(Date date){
        return date.toInstant().atZone(ZoneId.systemDefault()).format(dateFormat);
    }

    private void confirmDelete(Fi file){
        ui.showConfirm("@confirm", i("确认删除回放文件?") + "\n" + file.name(), () -> {
            if(!file.delete()){
                ui.showErrorMessage(i("删除回放文件失败") + ": " + file.name());
                return;
            }
            String key = file.absolutePath();
            metaCache.remove(key);
            loadingMeta.remove(key);
            replayFiles.remove(file);
            rebuildList();
        });
    }

    private void playReplay(Fi file){
        if(!file.exists()){
            ui.showErrorMessage(i("回放文件不存在") + ": " + file.name());
            rebuildList();
            return;
        }
        hide();
        ReplayController.startPlay(file);
    }

    private void loadExternalReplay(){
        FileChooser.setLastDirectory(saveDirectory);
        platform.showFileChooser(true, i("打开回放文件"), "mrep", file -> Core.app.post(() -> {
            hide();
            ReplayController.startPlay(file);
        }));
    }

    private static String formatSize(long bytes){
        if(bytes < 1024) return bytes + " B";
        if(bytes < 1024L * 1024L) return Strings.fixed(bytes / 1024f, 1) + " KB";
        if(bytes < 1024L * 1024L * 1024L) return Strings.fixed(bytes / 1024f / 1024f, 1) + " MB";
        return Strings.fixed(bytes / 1024f / 1024f / 1024f, 1) + " GB";
    }

    private class ReplayItem extends Table{
        private final Fi file;

        private ReplayItem(Fi file){
            super(Styles.grayPanel);
            this.file = file;
            rebuild();
        }

        private void rebuild(){
            clearChildren();
            top().margin(10f);
            defaults().left().growX();

            if(portraitLayout){
                add("[accent]" + file.nameWithoutExtension() + "[]").wrap().row();
                table(buttons -> buildActionButtons(buttons, file)).growX().padTop(8f).row();
            }else{
                table(title -> {
                    title.defaults().left();
                    title.add("[accent]" + file.nameWithoutExtension() + "[]").growX().wrap();
                    title.add().growX();
                    title.table(buttons -> buildActionButtons(buttons, file)).right();
                }).growX().row();
            }

            table(info -> buildBaseInfo(info, file)).growX().padTop(4f).row();

            ReplayData meta = metaCache.get(file.absolutePath());
            if(meta == null){
                add("[lightgray]" + i("读取回放头信息中...") + "[]").growX().wrap();
                requestMeta(file);
            }else if(meta == unreadableMeta){
                add("[lightgray]" + i("无法读取回放头信息") + "[]").growX().wrap();
            }else{
                table(info -> buildMetaInfo(info, meta)).growX().padTop(2f).row();
            }
        }
    }

}
