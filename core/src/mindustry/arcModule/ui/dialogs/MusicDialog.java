package mindustry.arcModule.ui.dialogs;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.Timer;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ui.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.*;
import java.net.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.RFuncs.getPrefix;

public class MusicDialog extends BaseDialog{
    public static final String version = "1.2.4";
    public static final String ShareType = "[pink]<Music>";
    public float vol;
    public Music player, sounds;
    private static final String E = "UTF-8";
    private static final Seq<MusicApi> apis = new Seq<>();
    private final Seq<MusicList> lists = new Seq<>();
    private Table lrcTable;
    private MusicApi api;
    private Runnable loadStatus;
    private float progress;
    private Slider progressBar;
    private MusicInfo nowMusic;
    private boolean loaded, updating, paused, playing;
    private BaseDialog switchDialog;
    private float fontScale = 1f;
    private String lrcColor, nextLrcColor, lrcLine1 = "松鼠音乐", lrcLine2 = "松鼠音乐";
    private LYRIC lyric;
    private Dialog settingsDialog;
    private ListDialog listDialog;
    private MusicList list;
    private MessageDigest md5;
    private Fi tmpDir;

    public MusicDialog(){
        super("松鼠音乐");
        try{
            md5 = MessageDigest.getInstance("MD5");
            nowMusic = new MusicInfo();
            progress = 0;
            addApi(null);
            addApi(new Squirrel());
            addApi(new KuGouWeb());
            try{
                addApi(new NetEaseMusic());
            }catch(Exception ignored){
                addApi(null);
            }
            api = apis.get(2);
            list = lists.get(2);
            addCloseButton();
            settingsDialog = new SettingsDialog();
            listDialog = new ListDialog();
            buttons.button("切换api", this::switchApi);
            buttons.button("立即结束音乐", Icon.cancel, () -> {
                player.stop();
                player.setVolume(0);
                player.pause(true);
                player.setPosition(0);
                player.dispose();
                sounds.stop();
                sounds.setVolume(0);
                sounds.pause(true);
                sounds.setPosition(0);
                sounds.dispose();
                player = new Music();
                sounds = new Music();
            });
            buttons.row();
            buttons.button("上传本地音乐", this::upload).disabled(b -> !api.canUpload);
            buttons.button("歌单", () -> listDialog.show());
            buttons.button(Icon.settings, () -> settingsDialog.show());
            onResize(this::setup);
            shown(() -> {
                if(check()) setup();
            });
            player = new Music();
            sounds = new Music();
            vol = Core.settings.getFloat("musicVolume", 100f);
            player.setVolume(vol / 100 * 2);
            sounds.setVolume(vol / 100 * 2);
            loaded = true;
            setup();
            switchDialog = new BaseDialog("切换api");
            switchDialog.cont.label(() -> "当前api: (" + api.thisId + ")" + api.name);
            switchDialog.cont.row();
            switchDialog.cont.pane(p -> {
                for(MusicApi a : apis){
                    if(a != null){
                        byte id = a.thisId;
                        String name = a.name;
                        p.button("(" + id + ")" + name, () -> {
                            api = apis.get(id);
                            list = lists.get(id);
                            setup();
                            switchDialog.hide();
                        }).width(200f);
                        p.row();
                    }
                }
            }).growX().growY();
            switchDialog.addCloseButton();
            fontScale = Core.settings.getFloat("lrcFontScale", 1);
            lrcColor = "[" + Core.settings.getString("lrcColor", "blue") + "]";
            nextLrcColor = "[" + Core.settings.getString("nextLrcColor", "white") + "]";
            buildLRC();
            Events.run(EventType.Trigger.update, this::updateProgress);
            netClient.addPacketHandler("forceAudio", url -> {
                if(Core.settings.getBool("arcShareMedia") && MessageDialog.arcMsgType.music.show) Http.get(url, r -> {
                    try{
                        Fi tmp = tmpDir.child("server.mp3");
                        tmp.writeBytes(r.getResult());
                        sounds.load(tmp);
                        sounds.play();
                    }catch(Exception ignored){
                    }
                });
            });
            netClient.addPacketHandler("stopAudio", url -> sounds.stop());

        }catch(Exception ignored){
        }
    }

    public void addApi(MusicApi api){
        apis.add(api);
        lists.add(api == null ? null : new MusicList(api.thisId));
    }

    private void buildLRC(){
        if(lrcTable != null) lrcTable.remove();
        if(Core.settings.getBool("showLRC")){
            lrcTable = new LRCTable();
            Core.scene.add(lrcTable);
        }
    }

    private void switchApi(){
        switchDialog.show();
    }

    private boolean check(){
        if(!loaded){
            cont.clear();
            cont.add("[red]松鼠音乐加载失败");
        }
        return loaded;
    }

    private void play(MusicInfo info){
        stop();
        nowMusic = info;
        api = apis.get(info.src);
        list = lists.get(info.src);
        list.add(info);
        list.set(list.indexOf(info));
        try{
            if(info.lrc != null) lyric = info.lrc;
            else lyric = null;
            Http.get(info.url, r -> {
                Fi tmp = tmpDir.child("squirrel.mp3");
                tmp.writeBytes(r.getResult());
                player.stop();
                player.pause(false);
                player.load(tmp);
                player.play();
                Timer.schedule(() -> playing = true, 0.5f);//badly
                loadStatus.run();
            });
        }catch(Exception e){
            ui.showException("发生了一个错误", e);
            playNext();
        }
    }

    private void playDirectly(Fi f) throws Exception{
        player.stop();
        player.pause(false);
        player.load(f);
        player.play();
        Timer.schedule(() -> playing = true, 1);
        loadStatus.run();
    }

    private void setup(){
        if(!loaded) return;
        tmpDir = tmpDirectory.child("music");
        tmpDir.mkdirs();
        tmpDir.emptyDirectory();
        cont.top();
        cont.clear();
        api.build(cont);
        cont.row();
        cont.table(t -> loadStatus = () -> {
            t.clear();
            t.bottom();
            float width = Core.graphics.getWidth() / Scl.scl() * 0.9f;
            t.table(tt -> {
                tt.left();
                Table[] img = {null};
                tt.table(ttt -> img[0] = ttt).size(64).pad(2f);
                if(nowMusic.imgBuf == null && nowMusic.img != null) Http.get(nowMusic.img, r -> {
                    Pixmap pix = new Pixmap(r.getResult());
                    Core.app.post(() -> {
                        TextureRegion cache = new TextureRegion(new Texture(pix));
                        nowMusic.imgBuf = cache;
                        pix.dispose();
                        img[0].image(cache).size(64);
                    });
                });
                if(nowMusic.imgBuf != null){
                    img[0].image(nowMusic.imgBuf).size(64);
                }
                tt.table(ms -> {
                    ms.table(ttt -> {
                        ttt.add(nowMusic.name == null ? "松鼠音乐" : (nowMusic.author + " - " + nowMusic.name)).left().wrap().style(Styles.outlineLabel).growX();
                        ttt.button(Icon.leftSmall, RStyles.clearLineNonei, this::prev).margin(3f).padTop(6f).top().right().size(32);
                        ttt.button(Icon.rightSmall, RStyles.clearLineNonei, this::playNext).margin(3f).pad(2).padTop(6f).top().right().size(32);
                        ttt.button(Icon.play, RStyles.clearLineNonei, this::play).margin(3f).pad(2).padTop(6f).top().right().size(32);
                        ttt.button(Icon.pause, RStyles.clearLineNonei, this::pause).margin(3f).pad(2).padTop(6f).top().right().size(32);
                        ttt.button(Icon.downloadSmall, RStyles.clearLineNonei, () -> {
                            if(nowMusic.url != null){
                                download(nowMusic);
                            }
                        }).margin(3f).pad(2).padTop(6f).top().right().size(32);
                        ttt.label(() -> "音量:" + (byte)vol);
                        ttt.button(Icon.upSmall, RStyles.clearLineNonei, () -> {
                            vol = Math.min(vol + 10, 100);
                            Core.settings.put("musicVolume", vol);
                            player.setVolume(vol / 100 * 2);
                            sounds.setVolume(vol / 100 * 2);
                        }).margin(3f).pad(2).padTop(6f).top().right().size(32);
                        ttt.button(Icon.downSmall, RStyles.clearLineNonei, () -> {
                            vol = Math.max(vol - 10, 0);
                            Core.settings.put("musicVolume", vol);
                            player.setVolume(vol / 100 * 2);
                            sounds.setVolume(vol / 100 * 2);
                        }).margin(3f).pad(2).padTop(6f).top().right().size(32);
                        ttt.button(Icon.refreshSmall, RStyles.clearLineNoneTogglei, () -> {
                            player.setLooping(!player.isLooping());
                            ui.announce("单曲循环已" + (player.isLooping() ? "开启" : "关闭"), 1f);
                        }).margin(3f).pad(2).padTop(6f).top().right().checked(b -> player.isLooping()).size(32);
                        ttt.button(Icon.linkSmall, RStyles.clearLineNonei, () -> {
                            if(nowMusic.url != null){
                                apis.get(nowMusic.src).share(nowMusic);
                            }
                        }).margin(3f).pad(2).pad(6).top().right().size(32);
                    }).growX();
                    ms.row();
                    progressBar = ms.slider(0.01f, nowMusic.length == 0.0 ? 0.01f : nowMusic.length, 0.01f, f -> {
                        if(!updating){
                            player.setPosition(0);
                            player.setPosition(f);
                            player.pause(false);
                            paused = false;
                        }
                    }).growX().disabled(s -> nowMusic.length == 0).get();
                    ms.row();
                    ms.label(() -> (int)(progress / 60) + ":" + (int)(progress % 60) + (nowMusic.length == 0 ? "" : "/" + (nowMusic.length / 60) + ":" + (nowMusic.length % 60)));
                }).growX().growY();
            }).width(width).height(100f);
        });
        loadStatus.run();
    }

    private void updateLRC(double pos){
        if(lyric == null){
            lrcLine1 = lrcLine2 = "松鼠音乐";
            return;
        }
        lyric.get(pos, (s1, s2) -> {
            lrcLine1 = s1;
            lrcLine2 = s2;
        });
    }

    private void play(){
        if(!player.isPlaying()){
            if(paused){
                player.pause(false);
                paused = false;
                playing = true;
            }else{
                if(nowMusic.url != null){
                    Fi f = tmpDir.child("squirrel.mp3");
                    if(f.exists()){
                        try{
                            playing = false;
                            playDirectly(f);
                        }catch(Exception ignored){
                        }
                    }
                }
            }
        }
    }

    private void pause(){
        player.pause(true);
        paused = true;
        playing = false;
    }

    private void updateProgress(){
        if(!loaded) return;
        updating = true;
        float pos = player.getPosition();
        progress = pos;
        progressBar.setValue(progress);
        updating = false;
        updateLRC(pos * 1000);
        if(pos == 0 && playing && !player.isLooping()){
            playing = false;
            playNext();
        }
    }

    private void download(MusicInfo info){
        platform.showFileChooser(false, "下载音乐", "mp3", fi -> api.getInfoOrCall(info, fullInfo -> Http.get(fullInfo.url, r -> {
            fi.writeBytes(r.getResult());
            Core.app.post(() -> Vars.ui.showInfo("下载成功"));
        })));
    }

    private void upload(){
        platform.showFileChooser(true, "选择音乐文件", "mp3", f -> {
            ui.announce("正在上传...\n很慢!(1-2分钟)\n上传完成后会自动播放");
            api.upload(f, info -> api.getInfoOrCall(info, this::play));
        });
    }

    private void loadList(MusicList list){
        if(list == null) return;
        this.list = list;
        list.set(0);
        playNext(false);
        listDialog.build();
    }

    private void stop(){
        player.stop();
        playing = false;
        paused = false;
    }

    private void playNext(){
        playNext(true);
    }

    private void playNext(boolean next){
        stop();
        MusicInfo info = next ? list.getNext() : list.currentMusic;
        playOrStop(info);
    }

    private void prev(){
        stop();
        MusicInfo info = list.getPrev();
        playOrStop(info);
    }

    private void playOrStop(MusicInfo info){
        if(info == null){
            nowMusic = new MusicInfo();
            playing = false;
            loadStatus.run();
            return;
        }
        if(info.url == null) apis.get(info.src).getInfoOrCall(info, this::play);
        else this.play(info);
    }

    public boolean resolveMsg(String msg, @Nullable Player sender){
        if(!Core.settings.getBool("arcShareMedia")) return false;
        if((!msg.contains(ShareType)) || (!loaded) || (!MessageDialog.arcMsgType.music.show)){
            return false;
        }
        try{
            MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.music, msg));
            int start = msg.indexOf(' ', msg.indexOf(ShareType) + ShareType.length());
            int split = msg.indexOf('M', start);
            String mark = msg.substring(start + 1, split);
            if(mark.equals("$")){
                Core.app.post(() -> ui.showConfirm("松鼠音乐", (sender == null ? "" : sender.name) + "分享了一个歌单\n播放?", () -> Http.get("https://pastebin.com/raw/" + msg.substring(split + 1), r -> MusicList.parse(URLDecoder.decode(r.getResultAsString(), E), MusicDialog.this::loadList), e -> Core.app.post(() -> ui.showException(e)))));
            }else{
                byte src = Byte.parseByte(mark);
                String id = msg.substring(split + 1);
                if(src < 0 || src > apis.size || apis.get(src) == null && src != 0){
                    Core.app.post(() -> arcui.arcInfo("[red]无法找到api!\n可能是学术版本太旧"));
                }
                MusicApi current = apis.get(src);
                current.getMusicInfo(id, info -> Core.app.post(() -> ui.showConfirm("松鼠音乐", (sender == null ? "" : sender.name) + "分享了一首来自" + current.name + "的音乐" + (info.name == null ? "" : ":\n" + info.author + " - " + info.name) + "\n播放?", () -> current.getInfoOrCall(info, this::play))));
            }
        }catch(Exception e){
            Log.err(e);
            Core.app.post(() -> arcui.arcInfo("[orange]音乐读取失败"));
        }
        return true;
    }

    public static class MusicInfo{
        public String name;
        public String author;
        public String url;
        public String img;
        public TextureRegion imgBuf;
        public String id;
        public byte src;
        public int length;
        public LRC lrc;
    }

    public static class MusicSet{
        public byte count;
        public MusicInfo[] list;

        public MusicSet(byte length){
            list = new MusicInfo[length];
        }

        public void add(MusicInfo m){
            list[count++] = m;
        }
    }

    public abstract static class MusicApi{
        public String name;
        public byte thisId;
        public boolean canUpload;

        public void getMusicInfo(String id, Cons<MusicInfo> callback, MusicInfo src){
            getMusicInfo(id, callback, false, src);
        }

        public void getMusicInfo(String str, Cons<MusicInfo> callback){
            getMusicInfo(str, callback, null);
        }

        public abstract void getMusicInfo(String id, Cons<MusicInfo> callback, boolean noTip, MusicInfo src);

        public void upload(Fi file, Cons<MusicInfo> callback){

        }

        public abstract void build(Table root);

        public void getInfoOrCall(MusicInfo info, Cons<MusicInfo> cb){
            getInfoOrCall(info, cb, false);
        }

        public void getInfoOrCall(MusicInfo info, Cons<MusicInfo> cb, boolean noTip){
            if(info.url != null){
                cb.get(info);
                return;
            }
            getMusicInfo(info.id, cb, noTip, info);
        }

        public void share(MusicInfo info){
            Vars.ui.showConfirm("分享", "确认分享到聊天框?", () -> getInfoOrCall(info, fullInfo -> Call.sendChatMessage(getPrefix("pink", "Music") + " " + fullInfo.src + "M" + fullInfo.id), true));
        }

        public void share(MusicList list){
            if(list.size() == 0) return;
            Vars.ui.showConfirm("分享", "确认分享到聊天框?", () -> {
                try{
                    Http.HttpRequest req = Http.post("https://pastebin.com/api/api_post.php", "api_dev_key=sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk&api_option=paste&api_paste_expire_date=10M&api_paste_code=" + URLEncoder.encode(list.build(), E));
                    req.submit(r -> {
                        String code = r.getResultAsString();
                        Call.sendChatMessage(getPrefix("pink", "Music") + " $M" + code.substring(code.lastIndexOf('/') + 1));
                    });
                    req.error(e -> Core.app.post(() -> ui.showException("分享失败", e)));
                }catch(Exception e){
                    ui.showException("分享失败", e);
                }
            });
        }

        public void loadList(String str, Cons<MusicList> cb){
            String[] all = str.split("\\$");
            MusicList list = new MusicList(thisId);
            getMusicInfo(all[0], info -> {
                list.add(info);
                for(int i = 1; i < all.length; i++) getMusicInfo(all[i], list::add, true, info);
                cb.get(list);
            }, true, null);
        }

        public String buildList(MusicList list){
            return list.build();
        }
    }

    private static class Squirrel extends MusicApi{//松鼠站

        {
            name = "松鼠站";
            canUpload = true;
            thisId = 1;
        }

        @Override
        public void getMusicInfo(String rid, Cons<MusicInfo> callback, boolean noTip, MusicInfo src){
            callback.get(new MusicInfo(){{
                src = thisId;
                url = "http://124.220.46.174/api/get?id=" + rid;
                id = rid;
            }});
        }

        @Override
        public void upload(Fi file, Cons<MusicInfo> callback){
            RFuncs.uploadToWebID(file, s -> {
                Core.app.post(() -> ui.announce("上传成功"));
                callback.get(new MusicInfo(){{
                    src = thisId;
                    id = String.valueOf(s);
                }});
            });
        }

        @Override
        public void build(Table root){

        }
    }

    private static class LRCParser{
        public static LRC parse(String input){
            String[] lrcs = input.replace("\r", "").split("\\n");
            LRC output = new LRC();
            if(!input.contains("[00:")){
                output.add(0, "暂无歌词");
                return output;
            }
            for(String now : lrcs){
                try{
                    output.add(parseTime(now), now.substring(10));
                }catch(Exception ignored){
                }
            }
            return output;
        }

        private static double parseTime(String raw){
            double sum = 60 * Math.floor(Double.parseDouble(raw.substring(1, 3))) * 1000;
            sum += 1000 * Math.floor(Double.parseDouble(raw.substring(4, 6)));
            sum += 10 * Math.floor(Double.parseDouble(raw.substring(7, 9)));
            if(Double.isNaN(sum)) sum = 0d;
            return sum;
        }
    }

    public static class LRC extends LYRIC{
        Seq<Double> timeList;
        Seq<String> lrcList;
        int size, cached;
        double last;

        public LRC(){
            timeList = new Seq<>();
            lrcList = new Seq<>();
            last = 0;
            cached = 0;
        }

        public void add(double time, String line){
            timeList.add(time);
            lrcList.add(line);
            size++;
        }

        public void get(double now, Cons2<String, String> callback){
            for(int i = now > last ? cached : 0; i < size; i++){
                if(now > timeList.get(i)){
                    cached = i;
                    callback.get(lrcList.get(i), i + 1 == size ? "" : lrcList.get(i + 1));
                }
            }
            last = now;
        }
    }

    public static class MusicList{
        public final Seq<MusicInfo> list = new Seq<>();
        public byte api;
        public int current = 0;
        public MusicInfo currentMusic = new MusicInfo();

        public MusicList(byte api){
            this.api = api;
        }

        public static void parse(String input, Cons<MusicList> cb){
            try{
                MusicApi api = apis.get(Byte.parseByte(input.substring(0, input.indexOf("$"))));
                api.loadList(input.substring(input.indexOf("$") + 1), cb);
            }catch(Exception e){
                ui.showException(e);
            }
        }

        public void add(MusicInfo info){
            if(indexOf(info) == -1) list.add(info);
        }

        public boolean remove(int id){
            list.remove(id);
            if(id < current){
                current--;
            }
            return id == current;
        }

        public boolean remove(MusicInfo info){
            return remove(indexOf(info));
        }

        public int size(){
            return list.size;
        }

        public String build(){
            StringBuilder sb = new StringBuilder();
            sb.append(api).append("$");
            for(MusicInfo musicInfo : list){
                sb.append(musicInfo.id).append("$");
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        public MusicInfo get(int id){
            return list.get(id);
        }

        public MusicInfo getNext(){
            if(++current >= list.size) return null;
            current = Math.min(current, list.size - 1);
            return update();
        }

        public MusicInfo getPrev(){
            current = Math.max(current - 1, 0);
            return update();
        }

        public void set(int id){
            if(id >= list.size) return;
            current = id;
            update();
        }

        private MusicInfo update(){
            return currentMusic = current >= size() ? null : list.get(current);
        }

        public int indexOf(MusicInfo info){
            for(int i = 0; i < list.size; i++){
                if(Objects.equals(list.get(i).id, info.id)) return i;
            }
            return -1;
        }
    }

    public static abstract class LYRIC{
        public abstract void add(double time, String line);

        public abstract void get(double now, Cons2<String, String> callback);
    }

    private class ListDialog extends BaseDialog{
        public ListDialog(){
            super("歌单列表");
            buttons.button(Icon.trash, () -> ui.showConfirm("松鼠音乐", "是否清空歌单", () -> {
                list = new MusicList((byte)0);
                build();
            }));
            build();
            buttons.button(Icon.link, () -> apis.get(list.api).share(list)).disabled(b -> list.size() == 0);
            buttons.button(Icon.download, () -> platform.showFileChooser(false, "保存歌单文件", "list", f -> f.writeString(api.buildList(list)))).disabled(b -> list.size() == 0);
            buttons.button(Icon.upload, () -> platform.showFileChooser(true, "加载歌单文件", "list", f -> MusicList.parse(f.readString(), i -> Core.app.post(() -> MusicDialog.this.loadList(i)))));
            addCloseButton();
            onResize(this::build);
            shown(this::build);
        }

        public void build(){
            cont.clear();
            float width = Core.graphics.getWidth() / Scl.scl() * 0.9f;
            for(MusicInfo info : list.list){
                Button[] buttons = {null};
                Button button = buttons[0] = cont.button(b -> {
                }, RStyles.clearLineNonei, () -> {
                    if(!buttons[0].childrenPressed()){
                        MusicDialog.this.play(info);
                    }
                }).width(width).pad(2f).get();
                Table inner = new Table(Tex.whiteui);
                inner.setColor(Color.black);
                button.clearChildren();
                button.add(inner).growX();
                inner.add(info.author + " - " + info.name).left().padLeft(10f).wrap().style(Styles.outlineLabel).growX();
                inner.button(Icon.trash, RStyles.clearLineNonei, () -> {
                    if(list.remove(info)){
                        playNext();
                    }
                    build();
                }).margin(3f).pad(2).padTop(6f).top().right();
                inner.button(Icon.download, RStyles.clearLineNonei, () -> download(info)).margin(3f).pad(2).padTop(6f).top().right();
                inner.button(Icon.link, RStyles.clearLineNonei, () -> apis.get(info.src).share(info)).margin(3f).pad(2).pad(6).top().right();
                button.row();
                button.update(() -> button.setChecked(Objects.equals(list.currentMusic.id, nowMusic.id)));
                cont.row();
            }
        }
    }

    public abstract class NetApi extends MusicApi{
        int searchPage = 1, allPage = 1;
        Table menu;
        String queryString = "";
        TextField searchUI;

        public abstract void getTips(String str, Cons<String[]> cb);

        @Override
        public void build(Table root){
            root.table(s -> {
                s.top();
                searchUI = s.field(queryString, res -> getTips(queryString = res, st -> Core.app.post(() -> loadSearchTips(st)))).growX().get();
                s.button(Icon.zoom, () -> {
                    searchPage = 1;
                    search();
                });
            }).fillX().padBottom(4);
            root.row();
            menu = root.table().growY().get();
        }

        public abstract void search(String name, int page, Cons<MusicSet> callback);

        private void search(){
            search(queryString, searchPage, s -> Core.app.post(() -> loadSearchResult(s)));
        }

        private void loadSearchTips(String[] tips){
            menu.clear();
            menu.pane(t -> {
                float width = Core.graphics.getWidth() / Scl.scl() * 0.9f;
                t.top();
                for(String str : tips){
                    t.button(str, RStyles.flatt, () -> {
                        searchUI.setText(queryString = str);
                        search();
                    }).width(width);
                    t.row();
                }
            });
        }

        private void loadSearchResult(MusicSet m){
            menu.clear();
            menu.pane(t -> {
                t.top();
                if(m.count > 0){
                    float width = Core.graphics.getWidth() / Scl.scl() * 0.9f;
                    t.clear();
                    for(byte i = 0; i < m.count; i++){
                        MusicInfo info = m.list[i];
                        Button[] buttons = {null};
                        Button button = buttons[0] = t.button(b -> {
                        }, () -> {
                            if(!buttons[0].childrenPressed()){
                                api.getInfoOrCall(info, MusicDialog.this::play);
                            }
                        }).width(width).pad(2f).get();
                        Table inner = new Table(Tex.whiteui);
                        inner.setColor(Color.black);
                        button.clearChildren();
                        button.add(inner).growX();
                        inner.add(info.author + " - " + info.name).left().padLeft(10f).wrap().style(Styles.outlineLabel).growX();
                        inner.button(Icon.download, RStyles.clearLineNonei, () -> download(info)).margin(3f).pad(2).padTop(6f).top().right();
                        inner.button(Icon.link, RStyles.clearLineNonei, () -> share(info)).margin(3f).pad(2).pad(6).top().right();
                        button.row();
                        t.row();
                    }
                    t.table(tt -> {
                        tt.button(Icon.left, () -> {
                            searchPage--;
                            search();
                        }).height(50f).width(150f).disabled(b -> searchPage <= 1);
                        tt.add(searchPage + "/" + allPage);
                        tt.button(Icon.right, () -> {
                            searchPage++;
                            search();
                        }).height(50f).width(150f).disabled(b -> searchPage >= allPage);
                    });
                }
            }).growY();
        }
    }

    private class KuGouWeb extends NetApi{
        String uuid;

        {
            name = "酷狗网页版";
            canUpload = false;
            thisId = 2;
            uuid = Core.settings.getString("kguuid");
            if(uuid == null){
                byte[] buf = new byte[8];
                new Rand().nextBytes(buf);
                StringBuilder sb = new StringBuilder();
                byte[] result = md5.digest(buf);
                for(byte b : result){
                    sb.append(String.format("%02x", b));
                }
                uuid = sb.toString();
                Core.settings.put("kguuid", sb.toString());
            }
        }

        @Override
        public void getMusicInfo(String id, Cons<MusicInfo> callback, boolean noTip, MusicInfo src){
            Http.HttpRequest req = Http.get("https://wwwapi.kugou.com/yy/index.php?r=play/getdata&encode_album_audio_id=" + id);
            req.header("Cookie", "kg_mid=" + uuid);
            req.submit(res -> {
                JsonValue j = new JsonReader().parse(res.getResultAsString());
                if(j.getByte("status") == 0){
                    Core.app.post(() -> Vars.ui.showErrorMessage("此歌曲无法播放:\nKuGou Error: (" + j.getLong("err_code") + ")"));
                    return;
                }
                JsonValue data = j.get("data");
                if(data.getString("play_url").contains("clip") && !noTip){
                    Core.app.post(() -> Vars.ui.showConfirm("此歌曲为vip歌曲 仅支持播放部分", () -> callback.get(new MusicInfo(){{
                        name = data.getString("song_name");
                        author = data.getString("author_name");
                        url = data.getString("play_url");
                        img = data.getString("img");
                        id = data.getString("encode_album_audio_id");
                        src = thisId;
                        length = data.getInt("timelength") / 1000;
                        lrc = LRCParser.parse(data.getString("lyrics"));
                    }})));
                }else{
                    callback.get(new MusicInfo(){{
                        name = data.getString("song_name");
                        author = data.getString("author_name");
                        url = data.getString("play_url");
                        img = data.getString("img");
                        id = data.getString("encode_album_audio_id");
                        src = thisId;
                        length = data.getInt("timelength") / 1000;
                        lrc = LRCParser.parse(data.getString("lyrics"));
                    }});
                }
            });
        }

        @Override
        public void search(String name, int page, Cons<MusicSet> callback){
            try{
                long timestamp = new Date().getTime();
                String data = "NVPh5oo715z5DIWAeQlhMDsWXXQV4hwtappid=1014bitrate=0clienttime=" + timestamp + "clientver=1000dfid=-filter=10inputtype=0iscorrection=1isfuzzy=0keyword=" + name + "mid=" + uuid + "page=" + page + "pagesize=10platform=WebFilterprivilege_filter=0srcappid=2919userid=0uuid=" + uuid + "NVPh5oo715z5DIWAeQlhMDsWXXQV4hwt";
                byte[] result = md5.digest(data.getBytes(E));
                StringBuilder sb = new StringBuilder();
                for(byte b : result){
                    sb.append(String.format("%02x", b));
                }
                Http.get("https://complexsearch.kugou.com/v2/search/song?appid=1014&bitrate=0&clienttime=" + timestamp + "&clientver=1000&dfid=-&filter=10&inputtype=0&iscorrection=1&isfuzzy=0&keyword=" + URLEncoder.encode(name, E) + "&mid=" + uuid + "&page=" + page + "&pagesize=10&platform=WebFilter&privilege_filter=0&srcappid=2919&userid=0&uuid=" + uuid + "&signature=" + sb, res -> {
                    JsonValue j = new JsonReader().parse(res.getResultAsString());
                    if(j.getLong("error_code") != 0){
                        Core.app.post(() -> Vars.ui.showErrorMessage("搜索出错:\nKuGou Error: (" + j.getLong("error_code") + ") " + j.getString("error_msg")));
                        return;
                    }
                    JsonValue lists = j.get("data").get("lists");
                    allPage = j.get("data").getInt("total") / 10 + 1;
                    MusicSet set = new MusicSet((byte)10);
                    for(byte i = 0; i < 10; i++){
                        JsonValue thisMusic = lists.get(i);
                        if(thisMusic == null){
                            break;
                        }
                        set.add(new MusicInfo(){{
                            name = thisMusic.getString("SongName");
                            author = thisMusic.getString("SingerName");
                            id = thisMusic.getString("EMixSongID");
                            src = thisId;
                        }});
                    }
                    callback.get(set);
                });
            }catch(Exception e){
                Core.app.post(() -> ui.showException("搜索出错!", e));
            }
        }

        @Override
        public void getTips(String str, Cons<String[]> cb){
            try{
                Http.get("https://searchtip.kugou.com/getSearchTip?MusicTipCount=10&MVTipCount=0&albumcount=0&keyword=" + URLEncoder.encode(str, E), r -> {
                    JsonValue j = new JsonReader().parse(r.getResultAsString());
                    if(j.getByte("status") != 1) return;
                    int count = j.get("data").get(0).getInt("RecordCount");
                    JsonValue all = j.get("data").get(0).get("RecordDatas");
                    String[] list = new String[count];
                    for(int i = 0; i < count; i++){
                        list[i] = all.get(i).getString("HintInfo");
                    }
                    cb.get(list);
                });
            }catch(Exception ignored){
            }
        }
    }

    private class NetEaseMusic extends NetApi{
        NetEastEncryptor encryptor = new NetEastEncryptor();

        {
            name = "网易云网页版";
            canUpload = false;
            thisId = 3;
        }

        @Override
        public void getMusicInfo(String id, Cons<MusicInfo> callback, boolean noTip, MusicInfo raw){
            Http.HttpRequest req = Http.post("https://music.163.com/weapi/song/enhance/player/url/v1?csrf_token=");
            try{
                encryptor.encryptRequest(req, "{\"ids\":\"[" + id + "]\",\"level\":\"standard\",\"encodeType\":\"mp3\",\"csrf_token\":\"\"}");
            }catch(Exception e){
                ui.showException("获取歌曲信息错误", e);
            }
            req.submit(r -> {
                JsonValue j = new JsonReader().parse(r.getResultAsString());
                if(j.getInt("code") != 200){
                    Core.app.post(() -> ui.showErrorMessage("网易云状态码错误:\n" + j.getInt("code")));
                }
                JsonValue data = j.get("data").get(0);
                if(data.getInt("code") != 200){
                    Core.app.post(() -> ui.showInfo("此歌曲为vip专属 无法播放"));
                    return;
                }
                Http.HttpRequest lr = Http.post("https://music.163.com/weapi/song/lyric?csrf_token=");
                encryptor.encryptRequest(lr, "{\"id\":" + data.getString("id") + ",\"lv\":-1,\"tv\":-1,\"csrf_token\":\"\"}");
                lr.submit(rr -> {
                    JsonValue lrcData = new JsonReader().parse(rr.getResultAsString());
                    if(lrcData.getInt("code") != 200 || !lrcData.get("lrc").has("lyric")){
                        if(raw == null) callback.get(new MusicInfo(){{
                            name = data.getString("name");
                            url = data.getString("url");
                            id = data.getString("id");
                            src = thisId;
                            length = data.getInt("time") / 1000;
                        }});
                        else callback.get(new MusicInfo(){{
                            name = raw.name;
                            author = raw.author;
                            url = data.getString("url");
                            id = data.getString("id");
                            src = thisId;
                            length = data.getInt("time") / 1000;
                        }});
                    }
                    if(raw == null) callback.get(new MusicInfo(){{
                        name = data.getString("name");
                        url = data.getString("url");
                        id = data.getString("id");
                        src = thisId;
                        lrc = LRCParser.parse(lrcData.get("lrc").getString("lyric"));
                        length = data.getInt("time") / 1000;
                    }});
                    else callback.get(new MusicInfo(){{
                        name = raw.name;
                        author = raw.author;
                        url = data.getString("url");
                        id = data.getString("id");
                        src = thisId;
                        lrc = LRCParser.parse(lrcData.get("lrc").getString("lyric"));
                        length = data.getInt("time") / 1000;
                    }});
                });
            });
        }

        @Override
        public void search(String name, int page, Cons<MusicSet> callback){
            try{
                Http.HttpRequest req = Http.post("https://music.163.com/weapi/cloudsearch/get/web?csrf_token=");
                encryptor.encryptRequest(req, "{\"s\":\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"type\":\"1\",\"offset\":\"0\",\"total\":\"true\",\"limit\":\"30\",\"csrf_token\":\"\"}");
                req.submit(res -> {
                    try{
                        JsonValue j = new JsonReader().parse(res.getResultAsString());
                        if(j.getInt("code") != 200){
                            Core.app.post(() -> Vars.ui.showErrorMessage("搜索出错:\n网易云 Error: (" + j.getInt("code") + ") "));
                            return;
                        }
                        JsonValue lists = j.get("result").get("songs");
                        allPage = 1;
                        MusicSet set = new MusicSet((byte)30);
                        for(byte i = 0; i < 30; i++){
                            JsonValue thisMusic = lists.get(i);
                            if(thisMusic == null){
                                break;
                            }
                            StringBuilder sb = new StringBuilder();
                            int l = thisMusic.get("ar").size;
                            JsonValue ar = thisMusic.get("ar");
                            for(int k = 0; k < l; k++){
                                sb.append(ar.get(k).getString("name")).append("/");
                            }
                            if(sb.length() != 0) sb.deleteCharAt(sb.length() - 1);
                            set.add(new MusicInfo(){{
                                name = thisMusic.getString("name");
                                author = sb.toString();
                                id = thisMusic.getString("id");
                                src = thisId;
                            }});
                        }
                        callback.get(set);
                    }catch(Exception e){
                        Core.app.post(() -> ui.showException("搜索出错!", e));
                    }
                });
            }catch(Exception e){
                ui.showException("搜索出错!", e);
            }
        }

        @Override
        public void getTips(String str, Cons<String[]> cb){
            try{
                Http.HttpRequest req = Http.post("https://music.163.com/weapi/search/suggest/web?csrf_token=");
                encryptor.encryptRequest(req, "{\"s\":\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"limit\":\"10\",\"csrf_token\":\"\"}");
                req.submit(r -> {
                    JsonValue j = new JsonReader().parse(r.getResultAsString());
                    if(j.getInt("code") != 200) return;
                    JsonValue all = j.get("result").get("songs");
                    int count = all.size;
                    String[] list = new String[count];
                    for(int i = 0; i < count; i++){
                        list[i] = all.get(i).getString("name");
                    }
                    cb.get(list);
                });
            }catch(Exception ignored){
            }
        }

        @Override
        public void loadList(String str, Cons<MusicList> cb){
            String[] all = str.split("\uf71d");
            MusicList list = new MusicList(thisId);
            for(String s : all){
                String[] args = s.split("\uf6aa");
                try{
                    list.add(new MusicInfo(){{
                        src = thisId;
                        id = args[0];
                        name = URLDecoder.decode(args[1], E);
                        author = URLDecoder.decode(args[2], E);
                    }});
                }catch(Exception ignored){
                }
            }
            cb.get(list);
        }

        @Override
        public String buildList(MusicList list){
            StringBuilder sb = new StringBuilder();
            sb.append(thisId).append("$");
            try{
                for(MusicInfo musicInfo : list.list){
                    sb.append(musicInfo.id).append("\uf6aa").append(URLEncoder.encode(musicInfo.name, E)).append("\uf6aa").append(URLEncoder.encode(musicInfo.author, E)).append("\uf71d");
                }
            }catch(Exception ignored){
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        @Override
        public void share(MusicInfo info){
            Vars.ui.showConfirm("分享", "确认分享到聊天框?", () -> getInfoOrCall(info, fullInfo -> {
                try{
                    Call.sendChatMessage(getPrefix("pink", "Music") + " " + fullInfo.src + "M" + fullInfo.id + "\uf6aa" + URLEncoder.encode(fullInfo.name, E) + "\uf6aa" + URLEncoder.encode(fullInfo.author, E));
                }catch(Exception ignored){
                }
            }));
        }

        @Override
        public void share(MusicList list){
            if(list.size() == 0) return;
            Vars.ui.showConfirm("分享", "确认分享到聊天框?", () -> {
                try{
                    Http.HttpRequest req = Http.post("https://pastebin.com/api/api_post.php", "api_dev_key=sdBDjI5mWBnHl9vBEDMNiYQ3IZe0LFEk&api_option=paste&api_paste_expire_date=10M&api_paste_code=" + URLEncoder.encode(buildList(list), E));
                    req.submit(r -> {
                        String code = r.getResultAsString();
                        Call.sendChatMessage(getPrefix("pink", "Music") + " $M" + code.substring(code.lastIndexOf('/') + 1));
                    });
                    req.error(e -> Core.app.post(() -> ui.showException("分享失败", e)));
                }catch(Exception e){
                    ui.showException("分享失败", e);
                }
            });
        }

        @Override
        public void getMusicInfo(String str, Cons<MusicInfo> callback){
            String[] n = str.split("\uf6aa");
            try{
                getMusicInfo(n[0], callback, new MusicInfo(){{
                    name = URLDecoder.decode(n[1], E);
                    author = URLDecoder.decode(n[2], E);
                }});
            }catch(Exception ignored){
            }
        }

    }

    static class NetEastEncryptor{
        IvParameterSpec iv = new IvParameterSpec(new byte[]{48, 49, 48, 50, 48, 51, 48, 52, 48, 53, 48, 54, 48, 55, 48, 56});
        BigInteger modulus = new BigInteger("1577947502671315022124768178003454981218727833333897474240115310"
        + "2536627753526253991370180629076647918947753359785498960680319425"
        + "3978660329941980786072432806427833685472618792592200595694346872"
        + "9513017705807651353492595901674905361380824696806385144165942166"
        + "29258349130257685001248172188325316586707301643237607");
        BigInteger pk = new BigInteger("65537");

        public void encryptRequest(Http.HttpRequest req, String str) throws Exception{
            req.header("content-type", "application/x-www-form-urlencoded");
            NetMusicData data = encryptParams(str);
            req.content("params=" + URLEncoder.encode(data.params, E) + "&encSecKey=" + URLEncoder.encode(data.encSecKey, E));
        }

        public NetMusicData encryptParams(String raw) throws Exception{
            String rnd = rndString();
            String enc = encrypt(raw, "0CoJUm6Qyw8W8jud");
            enc = encrypt(enc, rnd);
            NetMusicData o = new NetMusicData();
            o.params = enc;
            rnd = new StringBuilder(rnd).reverse().toString();
            Cipher cipher = Cipher.getInstance("RSA/ECB/nopadding");
            RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, pk);
            RSAPublicKey key = (RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(publicSpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(rnd.getBytes());
            StringBuilder sb = new StringBuilder();
            for(byte b : result){
                sb.append(String.format("%02x", b));
            }
            o.encSecKey = sb.toString();
            return o;
        }

        public String encrypt(String raw, String key) throws Exception{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] bytes = key.getBytes(E);
            SecretKeySpec s = new SecretKeySpec(bytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, s, iv);
            byte[] e = cipher.doFinal(raw.getBytes(E));
            return String.valueOf(Base64Coder.encode(e));
        }

        public String rndString(){
            String keys = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 16; i++){
                sb.append(keys.charAt((int)Math.floor(Math.random() * 62)));
            }
            return sb.toString();
        }

        static class NetMusicData{
            public String params, encSecKey;
        }
    }

    private class LRCTable extends Table{
        public LRCTable(){
            setColor(new Color(127, 127, 127, 255));
            margin(0f);
            touchable = Touchable.enabled;
            table(Styles.black3, t -> {
                t.color.set(new Color(127, 127, 127, 255));
                t.addListener(new HandCursorListener());
                t.margin(6f);
                t.touchable = Touchable.enabled;
                t.table(tt -> {
                    tt.label(() -> nowMusic.name == null ? "松鼠音乐" : (nowMusic.author + " - " + nowMusic.name));
                    tt.button(Icon.leftSmall, RStyles.clearLineNonei, MusicDialog.this::prev).margin(3f).padTop(6f).top().right().size(32);
                    tt.button(Icon.rightSmall, RStyles.clearLineNonei, MusicDialog.this::playNext).margin(3f).pad(2).padTop(6f).top().right().size(32);
                    tt.button(Icon.play, RStyles.clearLineNonei, MusicDialog.this::play).margin(3f).pad(2).padTop(6f).top().right().size(32);
                    tt.button(Icon.pause, RStyles.clearLineNonei, MusicDialog.this::pause).margin(3f).pad(2).padTop(6f).top().right().size(32);
                    tt.button(Icon.downloadSmall, RStyles.clearLineNonei, () -> {
                        if(nowMusic.url != null){
                            download(nowMusic);
                        }
                    }).margin(3f).pad(2).padTop(6f).top().right().size(32);
                    tt.label(() -> "音量:" + (byte)vol);
                    tt.button(Icon.upSmall, RStyles.clearLineNonei, () -> {
                        vol = Math.min(vol + 10, 100);
                        Core.settings.put("musicVolume", vol);
                        player.setVolume(vol / 100 * 2);
                        sounds.setVolume(vol / 100 * 2);
                    }).margin(3f).pad(2).padTop(6f).top().right().size(32);
                    tt.button(Icon.downSmall, RStyles.clearLineNonei, () -> {
                        vol = Math.max(vol - 10, 0);
                        Core.settings.put("musicVolume", vol);
                        player.setVolume(vol / 100 * 2);
                        sounds.setVolume(vol / 100 * 2);
                    }).margin(3f).pad(2).padTop(6f).top().right().size(32);
                    tt.button(Icon.refreshSmall, RStyles.clearLineNoneTogglei, () -> {
                        player.setLooping(!player.isLooping());
                        ui.announce("单曲循环已" + (player.isLooping() ? "开启" : "关闭"), 1f);
                    }).margin(3f).pad(2).padTop(6f).top().right().checked(b -> player.isLooping()).size(32);
                    tt.button(Icon.linkSmall, RStyles.clearLineNonei, () -> {
                        if(nowMusic.url != null){
                            apis.get(nowMusic.src).share(nowMusic);
                        }
                    }).margin(3f).pad(2).pad(6).top().right().size(32);
                    tt.button(Icon.homeSmall, RStyles.clearLineNonei, MusicDialog.this::show).margin(3f).padTop(6f).top().right().size(32);
                });
                t.add().growX();
                translation.set(Core.settings.getFloat("lrcX", 200f), Core.settings.getFloat("lrcY", 200f));
                t.addListener(new InputListener(){
                    float lastx, lasty;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                        lastx = v.x;
                        lasty = v.y;
                        toFront();
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                        translation.add(v.x - lastx, v.y - lasty);
                        Core.settings.put("lrcX", translation.x);
                        Core.settings.put("lrcY", translation.y);
                        lastx = v.x;
                        lasty = v.y;
                    }
                });
                t.row();
                t.table(tt -> {
                    t.label(() -> "").update(l -> {
                        l.setFontScale(fontScale);
                        l.setText(lrcColor + lrcLine1);
                    }).center();
                    t.row();
                    t.label(() -> "").update(l -> {
                        l.setFontScale(fontScale);
                        l.setText(nextLrcColor + lrcLine2);
                    }).center();
                });
            });
        }
    }

    private class SettingsDialog extends BaseDialog{
        public SettingsDialog(){
            super("松鼠音乐设置");
            CheckBox box = new CheckBox("显示歌词");
            box.update(() -> box.setChecked(Core.settings.getBool("showLRC")));
            box.changed(() -> {
                Core.settings.put("showLRC", !Core.settings.getBool("showLRC"));
                buildLRC();
            });
            cont.add(box).row();
            cont.add("歌词颜色（正在播放）");
            cont.row();
            cont.field(Core.settings.getString("lrcColor", "blue"), s -> {
                Core.settings.put("lrcColor", s);
                lrcColor = "[" + s + "]";
            }).size(128, 32);
            cont.row();
            cont.add("歌词颜色（下一行）");
            cont.row();
            cont.field(Core.settings.getString("nextLrcColor", "white"), s -> {
                Core.settings.put("nextLrcColor", s);
                nextLrcColor = "[" + s + "]";
            }).size(128, 32);
            cont.row();
            cont.add("歌词字号：");
            cont.slider(0.4f, 4, 0.2f, 1, fs -> {
                fontScale = fs;
                Core.settings.put("lrcFontScale", fs);
            }).size(512, 40);
            cont.label(() -> String.valueOf(fontScale));
            cont.row();
            cont.table(tt -> {
                tt.add("预览效果：");
                tt.row();
                tt.label(() -> "").update(l -> {
                    l.setFontScale(fontScale);
                    l.setText(lrcColor + lrcLine1);
                });
                tt.row();
                tt.label(() -> "").update(l -> {
                    l.setFontScale(fontScale);
                    l.setText(nextLrcColor + lrcLine2);
                });
            });
            cont.row();
            cont.button("重置歌词位置", () -> {
                Core.settings.put("lrcX", 200f);
                Core.settings.put("lrcY", 200f);
                buildLRC();
            }).size(256, 48);
            cont.row();
            cont.add("[pink]松鼠音乐v" + version + "\n[gold]松鼠制作\n[cyan]松鼠站:squi2rel.tk");
            addCloseButton();
        }
    }
}
