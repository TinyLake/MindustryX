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
import mindustry.gen.Icon;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.VarsX;
import mindustryX.features.ui.comp.*;

import java.text.*;
import java.util.*;

/**
 * @author minri2
 * Create by 2024/4/12
 */
public class ModsRecommendDialog extends BaseDialog{
    private static final TextureRegion defaultModIcon = ((TextureRegionDrawable)Tex.nomap).getRegion();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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
            try{
                textureCache = Reflect.get(Vars.ui.mods.browser, "textureCache");
            }catch(Exception e){
                textureCache = new ObjectMap<>();
                Log.err(e);
            }
        }

        if(meta == null){
            String json = Core.files.internal("recommendMods.json").readString();

            meta = JsonIO.json.fromJson(RecommendMeta.class, json);
        }

        if(!fetchModList){
            setLoading(cont);
            Vars.ui.mods.browser.getModList(modListings -> {
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

        cont.table(info -> {
            info.top();
            info.defaults().center();

            info.image().color(Pal.darkerGray).padRight(16f).height(4f).growX();
            info.add(VarsX.bundle.modsRecommendTitle());
            info.image().color(Pal.darkerGray).padLeft(16f).height(4f).growX().row();

            info.table(bottom -> {
                bottom.add(VarsX.bundle.modsRecommendInfo()).color(Pal.lightishGray).padRight(8f);
                bottom.add(VarsX.bundle.modsRecommendLastUpdated(meta.lastUpdated)).color(Pal.lightishGray);
            }).padTop(6f).colspan(3);
        }).width(width);

        cont.row();

        cont.pane(Styles.noBarPane, table -> {
            for(RecommendModMeta modMeta : meta.modRecommend){
                if(modMeta.listing == null){
                    Log.warn("Recommend Mod '@' not found in github.", modMeta.repo);
                    continue;
                }

                Table card = table.table(Tex.whiteui, t -> setupModCard(t, modMeta)).color(Pal.darkestGray).width(width).pad(12f).get();
                if(installed(modMeta)){
                    card.addAction(Actions.color(Pal.gray, 1.5f));
                }

                Card.cardShadow(table, 6f, Pal.darkerGray);

                table.row();
            }
        }).scrollX(false).padTop(6f);
    }

    private void setupModCard(Table table, RecommendModMeta modMeta){
        table.defaults().growX();

        ModListing modListing = modMeta.listing;

        table.table(title -> {
            title.table(info -> {
                info.top();
                info.defaults().padTop(4f).expandX().left();

                info.add(VarsX.bundle.modsRecommendModName(modListing.name)).padTop(12f).row();
                info.add(VarsX.bundle.modsRecommendModAuthor(modListing.author)).color(Pal.lightishGray).padTop(8f).row();
                info.add(VarsX.bundle.modsRecommendModMinGameVersion(modListing.minGameVersion)).color(Pal.lightishGray).row();
                info.add(VarsX.bundle.modsRecommendModLastUpdated(getLastUpdatedTime(modListing))).color(Pal.lightishGray).row();
                info.add(VarsX.bundle.modsRecommendModStars(String.valueOf(modListing.stars))).color(Pal.lightishGray).row();
            }).pad(4f).padRight(12f).grow();

            title.add(new BorderImage(){{
                border(Pal.darkerGray);
            }}).size(128f).pad(4f).with(image -> getModIcon(modMeta.repo, image::setDrawable));
        });

        table.row();

        table.table(body -> {
            body.add(modMeta.reason).pad(4f).grow().wrap();

            body.addChild(new Table(buttons -> {
                buttons.setFillParent(true);
                buttons.right().bottom();
                buttons.defaults().size(32f);

                buttons.button(Icon.download, Styles.cleari, 24f, () -> Vars.ui.mods.githubImportMod(modListing.repo, modListing.hasJava, true));
            }));
        }).minHeight(48f).pad(8f);
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
