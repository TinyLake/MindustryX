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
            if(Core.app.isDesktop()){
                return new DesktopImpl();
            }else if(Core.app.isAndroid()){
                return (LoaderPlatform)Class.forName("mindustryX.loader.AndroidImpl").getConstructor().newInstance();
            }
        }catch(Exception e){
            Log.err(e);
        }
        return null;
    }
    public Main(){
        if(System.getProperty("MDTX-loaded") == null){
            System.setProperty("MDTX-loaded", "true");
            LoaderPlatform impl = getLoaderPlatform();
            if(impl == null){
                loadError("Not support platform, skip.");
                return;
            }
            impl.withSafeClassloader("preload");
        }else{
            Log.infoTag("MindustryX", "Already inside MindustryX, cleanup outside");
            try{
                Class<?> mod = Core.class.getClassLoader().loadClass(Main.class.getName());
                Reflect.invoke(mod, "cleanup");
            }catch(Exception e){
                Log.err(e);
            }
        }
    }

    private static LoaderPlatform impl;
    private static boolean needClear = false;

    private static void loadError(String msg){
        Log.errTag("MindustryX", msg);
        Events.on(ClientLoadEvent.class, (e) -> Vars.ui.showErrorMessage("Exception when load MindustryX:\n" + msg));
    }

    @SuppressWarnings("unused")//invoke in safe classloader
    static void preload(){
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
            loadError("Detected ARC client, skip: " + Reflect.get(Version.class, "arcbuild"));
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

    @SuppressWarnings("unused")//reflect
    public static void cleanup(){
        if(!needClear) return;
        needClear = false;
        impl.cleanup();
        Log.info("END cleanup");
    }

    static void load(){
        ClassLoader classLoader = impl.createClassloader();
        Log.info("=========== Start mindustryX client ===============");
        needClear = true;
        Threads.daemon(() -> {
            try{
                Thread.sleep(5000);
                cleanup();
            }catch(InterruptedException e){
                throw new RuntimeException(e);
            }
        });
        Vars.finishLaunch();//mark a successful launch
        Log.logger = new NoopLogHandler();
        try{
            Thread.currentThread().setContextClassLoader(classLoader);
            impl.launch(classLoader);
            Thread.currentThread().interrupt();
        }catch(Exception e){
            e.printStackTrace(System.err);
            Vars.launchIDFile.writeString(e.toString());//mark failed
        }
    }
}
