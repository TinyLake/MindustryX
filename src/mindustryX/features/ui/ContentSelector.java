package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.ui.Styles.cleari;

/**
 * @author minri2
 * Create by 2024/2/17
 */
public class ContentSelector extends BaseDialog{
    private static final float width = 300f;
    private static final ImageButtonStyle buttonStyle = new ImageButtonStyle(cleari){{
        up = getColoredRegion(Color.valueOf("#767676"));
        down = over = getColoredRegion(Pal.accent);
    }};

    private final Seq<UnlockableContent> contents = new Seq<>();
    private Boolf<UnlockableContent> selectable, consumer;

    private String query = "";
    private final Table contentTable;

    private static Drawable getColoredRegion(Color color){
        return ((TextureRegionDrawable)Tex.whiteui).tint(color);
    }

    public ContentSelector(){
        super("@contentSelector");

        contentTable = new Table();

        setup();

        resized(this::rebuild);
        shown(this::rebuild);
    }

    private void setup(){
        cont.table(queryTable -> {
            queryTable.image(Icon.zoom).size(64f);

            TextField field = queryTable.field(query, text -> {
                query = text;
                rebuild();
            }).pad(8f).growX().get();

            if(Core.app.isDesktop()){
                Core.scene.setKeyboardFocus(field);
            }

            queryTable.button(Icon.cancel, Styles.clearNonei, () -> {
                query = "";
                field.setText(query);
                rebuild();
            }).size(64f);
        }).growX();

        cont.row();

        cont.pane(contentTable).scrollX(false).grow();

        addCloseButton();
    }

    private void rebuild(){
        contentTable.clearChildren();

        contentTable.top();

        float selectorWidth = Core.scene.getWidth() * 0.8f;
        int rows = (int)(selectorWidth / width / Scl.scl());

        int index = 0;
        for(UnlockableContent content : contents){
            if(!selectable.get(content)) continue;
            if(!query.isEmpty() && (!Strings.matches(query, content.name) || !Strings.matches(query, content.localizedName))) continue;

            contentTable.button(table -> {
                setupContentTable(table, content);
            }, buttonStyle, () -> {
                if(consumer.get(content)){
                    hide();
                }
            }).pad(8f).width(width);

            if(++index % rows == 0){
                this.contentTable.row();
            }
        }
    }

    private void setupContentTable(Table table, UnlockableContent content){
        table.image(content.uiIcon).scaling(Scaling.fit).size(48f).pad(8f).expandX().left();

        table.table(infoTable -> {
            infoTable.defaults().width(width * 0.7f).pad(4f);

            infoTable.add(content.localizedName).ellipsis(true).labelAlign(Align.right);
            infoTable.row();
            infoTable.add(content.name).ellipsis(true).labelAlign(Align.right).color(Pal.lightishGray);
        }).fillX();

        table.image().width(4f).color(Color.darkGray).growY().right();
        table.row();
        Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
        horizontalLine.colspan(table.getColumns());
    }

    public <T extends UnlockableContent> void  select(Seq<T> contents, Boolf<T> selectable, Boolf<T> consumer){
        this.contents.set(contents);

        this.selectable = (Boolf<UnlockableContent>)selectable;
        this.consumer = (Boolf<UnlockableContent>)consumer;

        show();

        query = "";
    }
}
