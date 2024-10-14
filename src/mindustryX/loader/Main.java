package mindustryX.loader;

import arc.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;

import java.util.*;

/** @author WayZer */
@SuppressWarnings("unused")
public class Main extends Mod{
    static LoaderPlatform getLoaderPlatform(){
        try{
            //FIXME 新版本ARC改动了Application接口，导致无法直接调用。
            var intf = Core.class.getClassLoader().loadClass("arc.Application");
            if(Reflect.invoke(intf, Core.app, "isDesktop", new Object[]{})){
                return new DesktopImpl();
            }else if(Reflect.invoke(intf, Core.app, "isAndroid", new Object[]{})){
                return (LoaderPlatform)Class.forName("mindustryX.loader.AndroidImpl").getConstructor().newInstance();
            }
        }catch(Exception e){
            Log.err(e);
        }
        return null;
    }

    public Main(){
        //When import mod, the constructor will be invoked.
        if(System.getProperty("MDTX-loaded") == null){
            System.setProperty("MDTX-loaded", "true");
            LoaderPlatform impl = getLoaderPlatform();
            if(impl == null){
                loadError("Not support platform, skip.");
                return;
            }
            impl.withSafeClassloader("preload");
        }else{
            Log.infoTag("MindustryX", "Already inside MindustryX.");
        }
    }

    private static LoaderPlatform impl;

    private static void loadError(String msg){
        Log.errTag("MindustryX", msg);
        Events.on(ClientLoadEvent.class, (e) -> Vars.ui.showErrorMessage("Exception when load MindustryX:\n" + msg));
    }

    @SuppressWarnings("unused")//invoke in safe classloader
    static void preload(){
        if(Vars.clientLoaded) return;
        impl = getLoaderPlatform();
        if(!checkVersion()) return;
        Core.app.post(Main::load);
        try{
            Thread.sleep(9999999999999999L);
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    private static boolean checkVersion(){
        if(!Version.type.equals("official") || !Version.combined().startsWith(Version.modifier)){
            loadError("Not official version, skip: get " + Version.combined());
            return false;
        }

        try{
            loadError("Detected ARC client, skip: " + Reflect.get(Version.class, "arcBuild"));
            return false;
        }catch(Exception e){/*ignore*/}

        try{
            loadError("Detected FOO client, skip: " + Reflect.get(Version.class, "clientVersion"));
            return false;
        }catch(Exception e){/*ignore*/}

        ModMeta meta = null;
        @SuppressWarnings("unchecked")
        var metas = ((ObjectMap<Class<?>, ModMeta>)Reflect.get(Vars.mods, "metas"));
        for(Entry<Class<?>, ModMeta> entry : metas.entries()){
            if(entry.key.getName().equals(Main.class.getName())){//the class is not the same one.
                meta = entry.value;
                break;
            }
        }
        Objects.requireNonNull(meta, "Can't get mod meta");
        String version = meta.minGameVersion;
        if(!Version.buildString().equals(version)){
            loadError("Version not match, skip. (expect " + version + ", get " + Version.buildString() + ")");
            return false;
        }
        return true;
    }

    static void load(){
        ClassLoader classLoader = impl.createClassloader();
        impl.beforeLaunch();
        Vars.finishLaunch();//mark a successful launch
        Log.info("=========== Start mindustryX client ===============");
        Log.logger = new NoopLogHandler();
        try{
            Thread.currentThread().setContextClassLoader(classLoader);
            Reflect.set(classLoader.loadClass("mindustryX.VarsX"), "isLoader", true);
            impl.launch(classLoader);
            Thread.currentThread().interrupt();
        }catch(Exception e){
            e.printStackTrace(System.err);
            Vars.launchIDFile.writeString(e.toString());//mark failed
        }
    }
}
