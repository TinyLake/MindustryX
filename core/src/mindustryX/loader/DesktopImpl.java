package mindustryX.loader;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.game.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class DesktopImpl implements LoaderPlatform{
    @Override
    public void withSafeClassloader(String method){
        URL file = ((URLClassLoader)Main.class.getClassLoader()).getURLs()[0];
        ClassLoader parent = Core.class.getClassLoader();
        try(var classLoader = new URLClassLoader(new URL[]{file}, parent)){
            Reflect.invoke(classLoader.loadClass(Main.class.getName()), method);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanup(){
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
    }

    @Override
    public ClassLoader createClassloader(){
        URL file = ((URLClassLoader)Main.class.getClassLoader()).getURLs()[0];
        ClassLoader parent = Core.class.getClassLoader();
        return new URLClassLoader(new URL[]{file}, parent){
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
                return new CompoundURLEnumeration(
                //self first
                findResources(name), parent.getResources(name)
                );
            }
        };
    }

    @Override
    public void launch(ClassLoader loader) throws Exception{
        Reflect.invoke(loader.loadClass("mindustry.desktop.DesktopLauncher"), "main", new Object[]{new String[]{}}, String[].class);
        System.exit(0);
    }
}
