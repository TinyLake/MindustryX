package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.Styles;
import mindustry.input.*;
import mindustry.world.meta.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.arcModule.RFuncs.getPrefix;

public class ContentInfoDialog extends BaseDialog{

    public ContentInfoDialog(){
        super("@info.title");

        addCloseButton();

        keyDown(key -> {
            if(key == keybinds.get(Binding.block_info).key){
                Core.app.post(this::hide);
            }
        });
    }

    public void show(UnlockableContent content){
        cont.clear();

        Table table = new Table();
        table.margin(10);

        //initialize stats if they haven't been yet
        content.checkStats();

        table.table(title1 -> {
            title1.image(content.uiIcon).size(iconXLarge).scaling(Scaling.fit).get().clicked(() -> Core.app.setClipboardText(content.emoji()));
            int logicId = content.getLogicId();
            title1.add("[accent]" + content.localizedName + "\n[gray]" + content.name + (logicId != -1 ? " <#" + logicId +">": "")).padLeft(5);
        });

        table.row();

        if(content.description != null){
            var any = content.stats.toMap().size > 0;

            if(any){
                table.add("@category.purpose").color(Pal.accent).fillX().padTop(10);
                table.row();
            }

            table.add("[lightgray]" + content.displayDescription()).wrap().fillX().padLeft(any ? 10 : 0).width(500f).padTop(any ? 0 : 10).left();
            table.row();

            if(!content.stats.useCategories && any){
                table.add("@category.general").fillX().color(Pal.accent);
                table.row();
            }
        }

        Stats stats = content.stats;

        for(StatCat cat : stats.toMap().keys()){
            OrderedMap<Stat, Seq<StatValue>> map = stats.toMap().get(cat);

            if(map.size == 0) continue;

            if(stats.useCategories){
                table.add("@category." + cat.name).color(Pal.accent).fillX();
                table.row();
            }

            for(Stat stat : map.keys()){
                table.table(inset -> {
                    inset.left();
                    inset.add("[lightgray]" + stat.localized() + ":[] ").left().top();
                    Seq<StatValue> arr = map.get(stat);
                    for(StatValue value : arr){
                        value.display(inset);
                        inset.add().size(10f);
                    }

                }).fillX().padLeft(10);
                table.row();
            }
        }

        if(content.details != null){
            //table.add("[gray]" + (content.unlocked() || !content.hideDetails ? content.details : Iconc.lock + " " + Core.bundle.get("unlock.incampaign"))).pad(6).padTop(20).width(400f).wrap().fillX();
            table.add("[gray]" + content.details ).pad(6).padTop(20).width(400f).wrap().fillX();
            table.row();
        }

        content.displayExtra(table);

        table.table(t -> {
            t.row();
            t.table(tt->{
                tt.button(content.emoji(), Styles.cleart, () -> Core.app.setClipboardText(content.emoji())).width(60f).tooltip(content.emoji());
                tt.button(Icon.info, Styles.clearNonei, () -> Core.app.setClipboardText(content.name)).width(50f).tooltip(content.name);
                tt.button(Icon.book, Styles.clearNonei, () -> Core.app.setClipboardText(content.description)).width(50f).tooltip(content.description);
            });

            t.row();
            t.table(tt->{
                tt.add("♐");
                tt.button("简",  Styles.cleart, () -> {
                    String message = arcItemInfo(content,false);
                    int seperator = 145;
                    for (int i=0; i < message.length()/(float)seperator;i++){
                        Call.sendChatMessage(message.substring(i*seperator,Math.min(message.length(),(i+1)*seperator)));
                    }
                }).width(50f).disabled(!Core.settings.getBool("arcShareWaveInfo"));
                tt.button("详",  Styles.cleart, () -> {
                    String message = arcItemInfo(content,true);
                    int seperator = 145;
                    for (int i=0; i < message.length()/(float)seperator;i++){
                        Call.sendChatMessage(message.substring(i*seperator,Math.min(message.length(),(i+1)*seperator)));
                    }
                }).width(50f).disabled(!Core.settings.getBool("arcShareWaveInfo"));


            });

        }).fillX().padLeft(10);

        ScrollPane pane = new ScrollPane(table);
        cont.add(pane);

        show();
    }

    private String arcItemInfo(UnlockableContent content, boolean description) {
        StringBuilder builder = getPrefix("sky", "Content");
        builder.append("标记了" + content.localizedName + content.emoji());
        builder.append("(" + content.name + ")");
        if (content.description != null && description) {
            builder.append("。介绍: " + content.description);
        }
        return builder.toString();
    }

}
