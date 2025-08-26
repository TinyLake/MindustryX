package mindustryX.mods.claj.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.mods.claj.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class JoinViaClajDialog extends BaseDialog{
    private String lastLink = "请输入您的claj代码";

    private Boolean valid = false;
    private String output = null;

    public JoinViaClajDialog(){
        super("通过claj加入游戏");
        cont.table(table -> {
            table.add("房间代码：").padRight(5f).left();
            TextField tf = table.field(lastLink, this::setLink).size(550f, 54f).maxTextLength(100).valid(this::setLink).get();
            tf.setProgrammaticChangeEvents(true);

            table.defaults().size(48f).padLeft(8f);
            table.button(Icon.paste, Styles.clearNonei, () -> tf.setText(Core.app.getClipboardText()));
        }).row();

        cont.label(() -> output).width(550f).left();

        buttons.defaults().size(140f, 60f).pad(4f);
        buttons.button("@cancel", this::hide);
        buttons.button("@ok", () -> {
            try{
                if(Vars.player.name.trim().isEmpty()){
                    Vars.ui.showInfo("@noname");
                    return;
                }

                var link = parseLink(lastLink);
                if(link.v2){
                    ProtocolV2.INSTANCE.join(link.ip, link.port, Long.parseLong(link.key), () -> {
                        Vars.ui.join.hide();
                        hide();
                    });
                }else{
                    ProtocolV1.INSTANCE.join(link.ip, link.port, link.key, () -> {
                        Vars.ui.join.hide();
                        hide();
                    });
                }

                Vars.ui.loadfrag.show("@connecting");
                Vars.ui.loadfrag.setButton(() -> {
                    Vars.ui.loadfrag.hide();
                    Vars.netClient.disconnectQuietly();
                });
            }catch(Throwable e){
                Vars.ui.showErrorMessage(e.getMessage());
            }
        }).disabled(b -> lastLink.isEmpty() || Vars.net.active());
    }

    private boolean setLink(String link){
        if(Objects.equals(lastLink, link)) return valid;

        try{
            parseLink(link);

            output = "[lime]代码格式正确, 点击下方按钮尝试连接！";
            valid = true;
        }catch(Throwable e){
            output = e.getMessage();
            valid = false;
        }

        lastLink = link;
        return valid;
    }

    private Link parseLink(String link) throws Exception{
        link = link.trim();
        if(link.startsWith("claj://")) return parseLinkV2(link);
        if(!link.startsWith("CLaJ")) throw new IOException("无效的claj代码：无CLaJ前缀");

        var hash = link.indexOf('#');
        if(hash != 42 + 4) throw new IOException("无效的claj代码：长度错误");

        var semicolon = link.indexOf(':');
        if(semicolon == -1) throw new IOException("无效的claj代码：服务器地址格式不正确");

        int port;
        try{
            port = Integer.parseInt(link.substring(semicolon + 1));
        }catch(Throwable ignored){
            throw new IOException("无效的claj代码：找不到服务器端口");
        }

        return new Link(link.substring(0, hash), link.substring(hash + 1, semicolon), port, false);
    }

    private Link parseLinkV2(String link) throws Exception{
        var uri = new URI(link);
        long room = ByteBuffer.wrap(Base64Coder.decode(uri.getPath().substring(1), Base64Coder.urlsafeMap)).getLong();
        return new Link(String.valueOf(room), uri.getHost(), uri.getPort(), true);
    }

    public static final class Link{
        private final String key;
        private final String ip;
        private final int port;
        private final boolean v2;

        public Link(String key, String ip, int port, boolean v2){
            this.key = key;
            this.ip = ip;
            this.port = port;
            this.v2 = v2;
        }
    }
}