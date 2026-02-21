package mindustryX.features.ui;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.features.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class ReplayManagerDialog extends BaseDialog{
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ReplayMeta unreadableMeta = new ReplayMeta(false, null, "", "", 0);

    private final Table list = new Table();
    private final ScrollPane pane = new ScrollPane(list, Styles.noBarPane);
    private final Seq<Fi> replayFiles = new Seq<>();
    private final Map<String, ReplayMeta> metaCache = new ConcurrentHashMap<>();
    private final Set<String> loadingMeta = ConcurrentHashMap.newKeySet();
    private final ExecutorService metaLoader = Threads.executor("Replay Meta Loader", 1);

    private boolean rebuildPosted;
    private String search;
    private TextField searchField;

    public ReplayManagerDialog(){
        super("回放管理器");

        setup();
        addCloseButton();

        shown(() -> {
            search = null;
            if(searchField != null) searchField.setText("");
            refreshReplayFiles();
            rebuildList();
        });
        onResize(this::rebuildList);
    }

    private void setup(){
        cont.clear();

        cont.table(tools -> {
            tools.defaults().height(54f);
            tools.image(Icon.zoom).padRight(6f);
            searchField = tools.field("", text -> {
                String value = text.trim().toLowerCase(Locale.ROOT);
                search = value.isEmpty() ? null : value;
                rebuildList();
            }).maxTextLength(80).growX().padRight(8f).get();
            searchField.setMessageText("搜索回放");

            tools.button("加载外部回放", Icon.upload, this::loadExternalReplay).padRight(8f);
            tools.button(Icon.refresh, Styles.cleari, this::refreshAndRebuild).size(54f);
        }).growX().row();

        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        list.margin(8f);
        cont.add(pane).grow().minHeight(420f);
    }

    private void rebuildList(){
        list.clearChildren();

        if(replayFiles.isEmpty()){
            list.add("没有可管理的回放文件").pad(12f).color(Color.lightGray);
            return;
        }

        int shown = 0;
        for(Fi file : replayFiles){
            if(!matchesSearch(file)) continue;
            shown++;
            addReplayItem(file);
        }

        if(shown == 0){
            list.add("没有匹配的回放文件").pad(12f).color(Color.lightGray);
        }
    }

    private void refreshAndRebuild(){
        refreshReplayFiles();
        rebuildList();
    }

    private void refreshReplayFiles(){
        replayFiles.clear();
        replayFiles.addAll(scanReplayFiles());

        HashSet<String> aliveFiles = new HashSet<>();
        for(Fi file : replayFiles){
            aliveFiles.add(file.absolutePath());
        }
        metaCache.keySet().removeIf(key -> !aliveFiles.contains(key));
        loadingMeta.removeIf(key -> !aliveFiles.contains(key));
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

        ReplayMeta meta = metaCache.get(file.absolutePath());
        if(meta == null || !meta.readable) return false;
        return meta.serverIp.toLowerCase(Locale.ROOT).contains(search)
        || meta.player.toLowerCase(Locale.ROOT).contains(search);
    }

    private void addReplayItem(Fi file){
        list.table(Styles.grayPanel, item -> {
            item.margin(10f);
            item.defaults().left();

            item.table(title -> {
                title.defaults().left();
                title.add("[accent]" + file.nameWithoutExtension() + "[]").growX().padRight(8f).wrap();

                title.table(buttons -> {
                    buttons.right();
                    buttons.defaults().size(40f);
                    boolean exists = file.exists();
                    buttons.button(Icon.play, Styles.emptyi, () -> playReplay(file)).disabled(b -> !exists);
                    buttons.button(Icon.trash, Styles.emptyi, () -> confirmDelete(file));
                }).right();
            }).growX().row();

            item.add(buildBaseInfo(file)).color(Color.lightGray).growX().wrap().row();

            ReplayMeta meta = metaCache.get(file.absolutePath());
            if(meta == null){
                item.add("[lightgray]读取回放头信息中...[]").growX().wrap();
                requestMeta(file);
            }else if(meta.readable){
                item.add(buildMetaInfo(meta)).color(Color.lightGray).growX().wrap();
            }else{
                item.add("[lightgray]无法读取回放头信息[]").growX().wrap();
            }
        }).growX().pad(4f).row();
    }

    private String buildBaseInfo(Fi file){
        return "修改时间: " + formatDate(new Date(file.lastModified())) + "  |  文件大小: " + formatSize(file.length());
    }

    private String buildMetaInfo(ReplayMeta meta){
        return "录制时间: " + formatDate(meta.time)
        + "  |  玩家: " + meta.player
        + "  [white]|  服务器: " + meta.serverIp
        + "  |  版本: " + meta.version;
    }

    private void requestMeta(Fi file){
        String key = file.absolutePath();
        if(metaCache.containsKey(key) || !loadingMeta.add(key)) return;

        metaLoader.execute(() -> {
            ReplayMeta meta = unreadableMeta;
            try(ReplayData.Reader reader = new ReplayData.Reader(file)){
                ReplayData header = reader.getMeta();
                meta = new ReplayMeta(true, header.getTime(), header.getServerIp(), header.getRecordPlayer(), header.getVersion());
            }catch(Throwable ignored){
            }

            ReplayMeta finalMeta = meta;
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
        ui.showConfirm("@confirm", "确认删除回放文件?\n" + file.name(), () -> {
            if(!file.delete()){
                ui.showErrorMessage("删除回放文件失败: " + file.name());
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
            ui.showErrorMessage("回放文件不存在: " + file.name());
            rebuildList();
            return;
        }
        hide();
        ReplayController.startPlay(file, true);
    }

    private void loadExternalReplay(){
        FileChooser.setLastDirectory(saveDirectory);
        platform.showFileChooser(true, "打开回放文件", "mrep", file -> Core.app.post(() -> {
            hide();
            ReplayController.startPlay(file, true);
        }));
    }

    private static String formatSize(long bytes){
        if(bytes < 1024) return bytes + " B";
        if(bytes < 1024L * 1024L) return Strings.fixed(bytes / 1024f, 1) + " KB";
        if(bytes < 1024L * 1024L * 1024L) return Strings.fixed(bytes / 1024f / 1024f, 1) + " MB";
        return Strings.fixed(bytes / 1024f / 1024f / 1024f, 1) + " GB";
    }

    private static class ReplayMeta{
        final boolean readable;
        final Date time;
        final String serverIp;
        final String player;
        final int version;

        ReplayMeta(boolean readable, Date time, String serverIp, String player, int version){
            this.readable = readable;
            this.time = time;
            this.serverIp = serverIp;
            this.player = player;
            this.version = version;
        }
    }
}
