package mindustryX;

import arc.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustryX.features.*;

import java.util.*;

import static mindustry.Vars.ui;

public class Hooks implements ApplicationListener{
    /** invoke before `Vars.init`. Note that may be executed from `Vars.loadAsync` */
    public static void beforeInit(){
        Log.infoTag("MindustryX", "Hooks.beforeInit");
        registerBundle();
        SettingsV2.INSTANCE.init();
        DebugUtil.init();//this is safe, and better at beforeInit,
        BindingExt.init();
        GithubAcceleration.INSTANCE.init();
        Events.on(ClientLoadEvent.class, (e) -> MetricCollector.INSTANCE.onLaunch());
        //deprecated Java 8
        if(!OS.isAndroid && Strings.parseInt(OS.javaVersion.split("\\.")[0]) < 17){
            Log.warn(VarsX.bundle.javaWarnLog(OS.javaVersion));
            Events.on(ClientLoadEvent.class, (e) -> ui.showInfo(VarsX.bundle.javaWarnDialog(OS.javaVersion)));
        }
    }

    /** invoke after loading, just before `Mod::init` */
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        LogicExt.init();
        if(!Vars.headless){
            if(AutoUpdate.INSTANCE.getActive())
                AutoUpdate.INSTANCE.checkUpdate();
            RenderExt.init();
            TimeControl.init();
            UIExt.init();
            ReplayController.init();
            ArcOld.colorizeContent();
            DamagePopup.init();
        }
        if(Vars.headless || Core.settings.getBool("console")){
            Vars.mods.getScripts().runConsole("X=Packages.mindustryX.features");
        }
    }

    public static @Nullable String onHandleSendMessage(String message, @Nullable Player sender){
        if(message == null) return null;
        if(Vars.ui != null){
            ShareFeature.resolve(message, sender);

            if(sender != null){
                StringBuilder builder = new StringBuilder();
                if(Vars.state.rules.pvp){
                    builder.append("[#").append(sender.team().color).append("]");
                    builder.append(sender.team() == Vars.player.team() ? Iconc.players + " " : Iconc.units);
                    builder.append("[]");
                }
                builder.append(sender.dead() ? Iconc.alphaaaa : sender.unit().type.emoji());
                builder.append(" ").append(message);
                message = builder.toString();
            }
        }
        return message;
    }

    @Override
    public void update(){
        if(!Vars.headless){
            updateTitle();
            BindingExt.pollKeys();
            if(BindingExt.oreAdsorption.keyDown()) ArcOld.doOreAdsorption();
            Vars.maxSchematicSize = VarsX.maxSchematicSize.get() > 256 ? Integer.MAX_VALUE : VarsX.maxSchematicSize.get();

            ArcOld.updatePlayer();
        }
    }

    private static void registerBundle(){
        //MDTX: bundle overwrite
        try{
            I18NBundle originBundle = Core.bundle;
            Fi handle = Core.files.internal("bundles/bundle-mdtx");
            Core.bundle = I18NBundle.createBundle(handle, Locale.getDefault());
            Reflect.set(Core.bundle, "locale", originBundle.getLocale());
            Log.info("MDTX: bundle has been loaded.");
            var rootBundle = Core.bundle;
            while(rootBundle.getParent() != null){
                rootBundle = rootBundle.getParent();
            }
            Reflect.set(rootBundle, "parent", originBundle);
        }catch(Throwable e){
            Log.err(e);
        }
    }

    private static String lastTitle;

    private void updateTitle(){
        if(Core.graphics == null) return;
        var mod = Vars.mods.orderedMods();
        var title = VarsX.bundle.windowTitle(VarsX.version, mod.count(Mods.LoadedMod::enabled), mod.size, Core.graphics.getWidth(), Core.graphics.getHeight());
        if(!title.equals(lastTitle)){
            lastTitle = title;
            Core.graphics.setTitle(title);
        }
    }
}
