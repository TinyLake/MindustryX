package mindustryX.features.ui;

import arc.*;
import arc.flabel.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.text.*;
import java.util.*;

/**
 * @author minri2
 * Create by 2024/4/12
 */
public class ModsRecommendDialog extends BaseDialog{
    private static final TextureRegion defaultModIcon = ((TextureRegionDrawable)Tex.nomap).getRegion();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static Color
    lightBlue = Color.valueOf("#a5dee5"),
    pink = Color.valueOf("#ffcfdf");

    private ObjectMap<String, TextureRegion> textureCache;
    private RecommendMeta meta;
    private boolean fetchModList;

    public ModsRecommendDialog(){
        super("");

        setup();

        shown(this::rebuild);
        addCloseButton();

        Events.run(Trigger.importMod, () -> Core.app.post(this::rebuild));
    }

    private void setup(){
        titleTable.clearChildren();
    }

    private void rebuild(){
        if(textureCache == null){
            textureCache = Reflect.get(Vars.ui.mods, "textureCache");
        }

        if(meta == null){
            String json = Core.files.internal("recommendMods.json").readString();

            meta = JsonIO.json.fromJson(RecommendMeta.class, json);
        }

        if(!fetchModList){
            setLoading(cont);
            Vars.ui.mods.getModList(0, modListings -> {
                // ???
                if(modListings == null){
                    setLoadFailed(cont);
                    return;
                }

                for(RecommendModMeta modMeta : meta.modRecommend){
                    modMeta.listing = modListings.find(modListing -> modMeta.repo.equals(modListing.repo));
                }

                fetchModList = true;
                rebuildCont();
            });

            return;
        }

        rebuildCont();
    }

    private void rebuildCont(){
        float width = Math.min(Core.graphics.getWidth() / Scl.scl(1.05f), 556f);

        cont.top().clearChildren();

        cont.add(new Card(Pal.lightishGray, Card.grayOuterDark, info -> {
            info.top();
            info.defaults().expandX().center();

            info.add("@mods.recommend").pad(12f).row();
            info.add(Core.bundle.format("mods.recommend.lastUpdated", meta.lastUpdated)).pad(6f).row();
            info.add("@mods.recommend.info").pad(6f);

            for(Element child : info.getChildren()){
                if(child instanceof Label label){
                    label.setColor(Pal.accent);
                    label.setStyle(Styles.outlineLabel);
                }
            }
        })).fillX();

        cont.row();

        cont.pane(Styles.noBarPane, t -> {
            t.background(Tex.whiteui).setColor(Pal.lightishGray);

            for(RecommendModMeta modMeta : meta.modRecommend){
                if(modMeta.listing == null){
                    Log.warn("Recommend Mod '@' not found in github.", modMeta.repo);
                    continue;
                }

                t.table(Tex.whiteui, card -> setupModCard(card, modMeta)).color(Pal.darkerGray).width(width).pad(12f).with(card -> {
                    if(installed(modMeta)){
                        card.addAction(Actions.color(Pal.accent, 1.5f));
                    }
                });

                Card.cardShadow(t);

                t.row();
            }
        }).scrollX(false);
    }

    private void setupModCard(Table table, RecommendModMeta modMeta){
        table.defaults().growX();

        ModListing modListing = modMeta.listing;

        table.table(title -> {
            title.add(new Card(Pal.gray, Card.grayOuterDark, info -> {
                info.top();
                info.defaults().padTop(2f).expandX().left();

                addInfo(info, "name", modListing.name).color(Pal.accent).pad(8f);
                addInfo(info, "author", modListing.author).color(pink).padTop(4f);
                addInfo(info, "minGameVersion", modListing.minGameVersion).color(lightBlue);
                addInfo(info, "lastUpdated", getLastUpdatedTime(modListing)).color(lightBlue);
                addInfo(info, "stars", modListing.stars).color(lightBlue);

                for(Element child : info.getChildren()){
                    if(child instanceof Label label){
                        label.setStyle(Styles.outlineLabel);
                    }
                }
            })).pad(4f).padRight(12f).grow();

            title.add(new BorderImage(){{
                border(Pal.darkestGray);
            }}).size(128f).pad(4f).with(image -> getModIcon(modMeta.repo, image::setDrawable));
        });

        table.row();

        table.add(new Card(Pal.gray, Card.grayOuterDark, body -> {
            body.add(modMeta.reason).pad(4f).grow().wrap();

            body.addChild(new Table(buttons -> {
                buttons.setFillParent(true);
                buttons.right().bottom();
                buttons.defaults().size(32f);

                buttons.button(Icon.download, Styles.cleari, 24f, () -> Vars.ui.mods.githubImportMod(modListing.repo, modListing.hasJava));
            }));
        })).minHeight(48f).pad(8f);
    }

    private Cell<?> addInfo(Table table, String bundle, Object value){
        Cell<?> cell = table.add(Core.bundle.format("mods.recommend.mod." + bundle, value)).color(pink);

        table.row();

        return cell;
    }

    private boolean installed(RecommendModMeta modMeta){
        return Vars.mods.list().find(mod -> modMeta.repo.equals(mod.getRepo())) != null;
    }

    private String getLastUpdatedTime(ModListing listing){
        try{
            Date date = dateFormat.parse(listing.lastUpdated);
            return DateFormat.getInstance().format(date);
        }catch(ParseException e){
            return "Unknown";
        }
    }

    private void getModIcon(String repo, Cons<TextureRegion> callback){
        TextureRegion cache = textureCache.get(repo);

        if(cache != null){
            callback.get(cache);
            return;
        }

        Http.get("https://raw.githubusercontent.com/Anuken/MindustryMods/master/icons/" + repo.replace("/", "_"), res -> {
            Pixmap pix = new Pixmap(res.getResult());
            Core.app.post(() -> {
                try{
                    Texture texture = new Texture(pix);
                    texture.setFilter(TextureFilter.linear);
                    TextureRegion region = new TextureRegion(texture);
                    textureCache.put(repo, region);
                    pix.dispose();

                    callback.get(region);
                }catch(Exception e){
                    Log.err(e);

                    textureCache.put(repo, defaultModIcon);
                    callback.get(defaultModIcon);
                }
            });
        }, err -> {
            textureCache.put(repo, defaultModIcon);
            callback.get(defaultModIcon);
        });
    }

    private static void setLoading(Table table){
        table.clearChildren();
        table.add(new FLabel("@alphaLoading")).style(Styles.outlineLabel).expand().center();
    }

    private static void setLoadFailed(Table table){
        table.clearChildren();
        table.add(new FLabel("@alphaLoadFailed")).style(Styles.outlineLabel).expand().center();
    }

    private static class RecommendMeta{
        public String lastUpdated;
        public Seq<RecommendModMeta> modRecommend;
    }

    private static class RecommendModMeta{
        public String repo;
        public String reason;
        public ModListing listing;
    }
}
