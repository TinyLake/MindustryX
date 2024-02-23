package mindustryX.mods.claj.dialogs;

import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustryX.mods.claj.*;

import java.io.IOException;
import java.util.Objects;

public class JoinViaClajDialog extends BaseDialog{
    private String lastLink = "请输入您的claj代码";

    private Boolean valid = false;
    private String output = null;

    public JoinViaClajDialog(){
        super("通过claj加入游戏");
        cont.table(table -> {
            table.add("房间代码：").padRight(5f).left();
            table.field(lastLink, this::setLink).size(550f, 54f).maxTextLength(100).valid(this::setLink);
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
                ClajIntegration.joinRoom(link.ip, link.port, link.key, () -> {
                    Vars.ui.join.hide();
                    hide();
                });

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

    private Link parseLink(String link) throws IOException{
        var link1 = link;
        link1 = link1.trim();
        if(!link1.startsWith("CLaJ")) throw new IOException("无效的claj代码：无CLaJ前缀");

        var hash = link1.indexOf('#');
        if(hash != 42 + 4) throw new IOException("无效的claj代码：长度错误");

        var semicolon = link1.indexOf(':');
        if(semicolon == -1) throw new IOException("无效的claj代码：服务器地址格式不正确");

        int port;
        try{
            port = Integer.parseInt(link1.substring(semicolon + 1));
        }catch(Throwable ignored){
            throw new IOException("无效的claj代码：找不到服务器端口");
        }

        return new Link(link1.substring(0, hash), link1.substring(hash + 1, semicolon), port);
    }

    public static final class Link{
        private final String key;
        private final String ip;
        private final int port;

        public Link(String key, String ip, int port){
            this.key = key;
            this.ip = ip;
            this.port = port;
        }
    }
}