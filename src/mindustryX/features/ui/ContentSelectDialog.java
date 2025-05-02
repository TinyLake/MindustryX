package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

/**
 * @author minri2
 * Create by 2024/2/17, Remake WayZer 2025/4/19
 */
public class ContentSelectDialog extends BaseDialog{
    private String query = "";
    private final GridTable contentTable = new GridTable();

    public ContentSelectDialog(){
        super("@contentSelector");

        cont.table(queryTable -> {
            queryTable.image(Icon.zoom).size(64f);

            TextField field = queryTable.field(query, text -> query = text).pad(8f).growX().get();

            queryTable.button(Icon.cancel, Styles.clearNonei, () -> {
                query = "";
                field.setText(query);
            }).size(64f);

            queryTable.update(() -> {
                if(Core.scene.getKeyboardFocus()==this)
                    Core.scene.setKeyboardFocus(field);
                if(Core.input.keyTap(Binding.chat) && Core.scene.getKeyboardFocus() == field){
                    var first = contentTable.firstElement();
                    if(first != null) first.fireClick();
                }
            });
        }).growX().row();

        contentTable.defaults().width(300);
        contentTable.top();
        cont.pane(contentTable).scrollX(false).grow();
        addCloseButton();
    }

    public Button addContent(Drawable icon, String name, String localizedName, Runnable action){
        return contentTable.button(table -> {
            table.image(icon).scaling(Scaling.fit).size(48f).pad(8f).expandX().left();

            table.table(infoTable -> {
                infoTable.defaults().width(width * 0.7f).pad(4f);

                infoTable.add(localizedName).ellipsis(true).labelAlign(Align.right);
                infoTable.row();
                infoTable.add(name).ellipsis(true).labelAlign(Align.right).color(Pal.lightishGray);
            }).fillX();
        }, action).pad(8f).width(width)
        .visible(() -> Strings.matches(query, name) || Strings.matches(query, localizedName)).get();
    }

    public Button addContent(UnlockableContent content, Runnable action){
        return addContent(new TextureRegionDrawable(content.uiIcon), content.name, content.localizedName, action);
    }

    public Button addNull(Runnable action){
        return addContent(Icon.none, "none", "@settings.resetKey", action);
    }

    public <T extends UnlockableContent> void addContents(Iterable<T> contents, @Nullable T current, @Nullable Cons<T> cons){
        for(var content : contents){
            addContent(content, () -> {
                if(cons != null) cons.get(content);
            }).setChecked(content == current);
        }
    }

    public static <T extends UnlockableContent> void once(Seq<T> contents, Cons<T> consumer){
        once(contents, null, consumer);
    }

    public static <T extends UnlockableContent> void once(Seq<T> contents, @Nullable T current, Cons<T> consumer){
        var dialog = new ContentSelectDialog();
        dialog.addContents(contents, current, (it) -> {
            consumer.get(it);
            dialog.hide();
        });
        dialog.show();
    }
}
