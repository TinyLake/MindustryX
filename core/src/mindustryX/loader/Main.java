package mindustryX.loader;

import arc.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;

import java.io.*;
import java.net.*;
import java.util.*;

@SuppressWarnings("unused")
public class Main extends Mod{
    private static boolean needClear = false;
    public Main(){
        if(System.getProperty("MDTX-loaded") == null){
            System.setProperty("MDTX-loaded", "true");
            withSafeClassloader("preload");
        }else{
            Log.infoTag("MindustryX", "Already inside MindustryX, cleanup outside");
            Class<?> mod = Reflect.get(Core.class.getClassLoader(), "outsideMod");
            Reflect.invoke(mod, "cleanup");
        }
    }

    @SuppressWarnings("SameParameterValue")
    static void withSafeClassloader(String method){
        URL file = ((URLClassLoader)Main.class.getClassLoader()).getURLs()[0];
        ClassLoader parent = Core.class.getClassLoader();
        try(var classLoader = new URLClassLoader(new URL[]{file}, parent)){
            Reflect.invoke(classLoader.loadClass(Main.class.getName()), method);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")//in safe classloader
    static void preload(){
        if(!checkVersion()) return;
        Core.app.post(Main::load);
        try{
            Thread.sleep(9999999999999999L);
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    private static boolean checkVersion(){
        if(!Core.app.isDesktop()){
            Log.infoTag("MindustryX", "Only support desktop, skip.");
            return false;
        }
        if(!Version.type.equals("official")){
            Log.infoTag("MindustryX", "Not official version, skip: get " + Version.type);
            return false;
        }

        try{
            Log.infoTag("MindustryX", "Detected ARC client, skip: "
            + Reflect.get(Version.class, "arcbuild"));
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
            Log.infoTag("MindustryX", "Version not match, skip. (expect " + version + ", get " + Version.buildString() + ")");
            return false;
        }
        return true;
    }

    @SuppressWarnings("unused")//reflect
    public static void cleanup(){
        if(!needClear) return;
        needClear = false;
        //fix steam
        //noinspection unchecked
        Seq<?> listeners = ((ObjectMap<Object, Seq<?>>)Reflect.get(Events.class, "events")).get(EventType.DisposeEvent.class);
        if(listeners != null) listeners.clear();

        for(ApplicationListener l : Core.app.getListeners()){
            l.pause();
            try{
                l.dispose();
            }catch(Throwable e){
                Log.err("Cleanup", e);
            }
        }
        Core.app.dispose();
        try{
            Class<?> sdl = Class.forName("arc.backend.sdl.jni.SDL");
            Reflect.invoke(sdl, "SDL_DestroyWindow", new Object[]{Reflect.get(Core.app, "window")}, long.class);
//            Reflect.invoke(sdl, "SDL_Quit");
        }catch(Throwable e){
            throw new RuntimeException(e);
        }
        Log.info("END cleanup");
    }

    static void load(){
        URL file = ((URLClassLoader)Main.class.getClassLoader()).getURLs()[0];
        ClassLoader parent = Core.class.getClassLoader();
        ClassLoader classLoader = createClassLoader(file, parent);

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
        Log.logger = new NoopLogHandler();
        try{
            Thread.currentThread().setContextClassLoader(classLoader);
            Reflect.invoke(classLoader.loadClass("mindustry.desktop.DesktopLauncher"), "main", new Object[]{new String[]{}}, String[].class);
        }catch(Exception e){
            e.printStackTrace();
        }
        System.exit(0);
    }

    static ClassLoader createClassLoader(URL file, ClassLoader parent){
        return new MyURLClassLoader(file, parent);
    }

    private static class MyURLClassLoader extends URLClassLoader{
        @SuppressWarnings("unused")//reflect
        public Class<?> outsideMod = Main.class;
        private final ClassLoader parent;

        public MyURLClassLoader(URL file, ClassLoader parent){
            super(new URL[]{file}, parent);
            this.parent = parent;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
            synchronized(getClassLoadingLock(name)){
                //check for loaded state
                Class<?> loadedClass = findLoadedClass(name);
                if(loadedClass == null){
                    try{
                        //try to load own class first
                        loadedClass = findClass(name);
                    }catch(ClassNotFoundException e){
                        //use parent if not found
                        return parent.loadClass(name);
                    }
                }

                if(resolve){
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException{
            try{
                return super.findClass(name);
            }catch(ClassNotFoundException e){
                if(overwrite(name)){
                    InputStream res = parent.getResourceAsStream(name.replace('.', '/').concat(".class"));
                    if(res != null){
                        try{
                            byte[] bs = Streams.copyBytes(res);
                            return defineClass(name, bs, 0, bs.length);
                        }catch(IOException | ClassFormatError e2){
                            e.addSuppressed(e2);
                        }finally{
                            Streams.close(res);
                        }
                    }
                }
                throw e;
            }
        }

        private Boolean overwrite(String name){
            if(name.startsWith("arc.backend.sdl.jni")) return false;
            return name.startsWith("mindustry") || name.startsWith("arc");
        }

        @Override
        public URL getResource(String name){
            if(name.equals("MindustryX.hjson"))
                return findResource("mod.hjson");
            if(name.equals("mod.hjson")) return null;
            //self first
            URL url = findResource(name);
            if(url == null)
                url = parent.getResource(name);
            return url;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException{
            return new CompoundEnumeration<URL>(new Enumeration[]{
            //self first
            findResources(name), parent.getResources(name)
            });
        }

        final static class CompoundEnumeration<E> implements Enumeration<E>{
            private final Enumeration<E>[] enums;
            private int index;

            public CompoundEnumeration(Enumeration<E>[] enums){
                this.enums = enums;
            }

            private boolean next(){
                while(index < enums.length){
                    if(enums[index] != null && enums[index].hasMoreElements()){
                        return true;
                    }
                    index++;
                }
                return false;
            }

            public boolean hasMoreElements(){
                return next();
            }

            public E nextElement(){
                if(!next()){
                    throw new NoSuchElementException();
                }
                return enums[index].nextElement();
            }
        }
    }
}
