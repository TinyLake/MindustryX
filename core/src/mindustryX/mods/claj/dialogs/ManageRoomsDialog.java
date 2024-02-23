package mindustryX.mods.claj.dialogs;

import arc.*;
import arc.graphics.Color;
import arc.net.Client;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustryX.mods.claj.*;

import java.io.IOException;

public class ManageRoomsDialog extends BaseDialog {
    static String serverIP = null;
    static int serverPort = 0;

    private Table list = null;
    private TextField field = null;
    private boolean valid = false;
    private boolean flip = false;
    private final Seq<String> clajURLs = Seq.with("new.xem8k5.top:1050");

    public ManageRoomsDialog() {
        super("管理claj房间");
        addCloseButton();

        cont.defaults().width(Vars.mobile ? 550f : 750f);

        cont.table(list -> {
            list.defaults().growX().padBottom(8f);
            list.update(() -> list.getCells().retainAll(cell -> cell.get() != null)); // remove closed rooms
            this.list = list;
        }).row();

        cont.table(url -> {
            url.field(clajURLs.first(), this::setURL).maxTextLength(100).valid(this::validURL).with(f -> field = f).growX();
            url.button(Icon.downOpen, Styles.clearNonei, () -> flip = !flip).size(48f).padLeft(8f);
        }).row();

        cont.collapser(list -> clajURLs.each(url -> list.button(url, Styles.cleart, () -> setURL(url)).height(32f).growX().row()), true, () -> flip).row();

        cont.button("新建房间并生成claj代码", () -> {
            try {
                list.add(new Room()).row();
            } catch (Exception e) {
                Vars.ui.showErrorMessage(e.getMessage());
            }
        }).disabled(b -> list.getChildren().size >= 4 || !valid).row();

        cont.labelWrap("允许你的朋友通过claj代码联机").labelAlign(2, 8).padTop(16f).width(400f).get().getStyle().fontColor = Color.lightGray;

        setURL(clajURLs.first());
    }

    // region URL
    private void setURL(String url) {
        field.setText(url);

        var semicolon = url.indexOf(':');
        serverIP = url.substring(0, semicolon);
        serverPort = Strings.parseInt(url.substring(semicolon + 1));
    }

    private boolean validURL(String url) {
        return valid = url.contains(":") && Strings.canParseInt(url.substring(url.indexOf(':') + 1));
    }

    // endregion
    static class Room extends Table {
        private final Client client;
        private String link = null;

        Room() throws IOException {
            client = ClajIntegration.createRoom(serverIP, serverPort, link -> this.link = link, this::close);

            table(Tex.underline, cont -> cont.label(() -> link)).growX().left().fontScale(.7f).ellipsis(true).growX();

            table(btns -> {
                btns.defaults().size(48f).padLeft(8f);
                btns.button(Icon.copy, Styles.clearNonei, () -> {
                    Core.app.setClipboardText(link);
                    Vars.ui.showInfoFade("@copied");
                }).disabled(_it -> link == null);
                btns.button(Icon.cancel, Styles.clearNonei, this::close);
            });
        }

        private void close() {
            client.close();
            remove();
        }
    }
}