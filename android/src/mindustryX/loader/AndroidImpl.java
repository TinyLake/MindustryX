package mindustryX.loader;

import android.app.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import arc.*;
import arc.Files.*;
import arc.backend.android.*;
import arc.files.*;
import arc.func.*;
import arc.util.*;
import dalvik.system.*;
import mindustry.*;
import mindustry.android.*;

import java.io.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class AndroidImpl implements LoaderPlatform{
    private AndroidLauncher app = (AndroidLauncher)Core.app;

    private Object[] getDexElements(ClassLoader classLoader){
        Object pathList = Reflect.get(BaseDexClassLoader.class, classLoader, "pathList");
        return Reflect.get(pathList, "dexElements");
    }

    private File findFirstJar(){
        return Reflect.get(getDexElements(Main.class.getClassLoader())[0], "path");
    }

    @Override
    public void withSafeClassloader(String method){
        try{
            ClassLoader classLoader = new DexClassLoader(findFirstJar().getPath(), app.getFilesDir().getPath(), null, Core.class.getClassLoader());
            Reflect.invoke(classLoader.loadClass(Main.class.getName()), method);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClassLoader createClassloader(){
        ClassLoader parent = Core.class.getClassLoader();
        File apk = Reflect.get(getDexElements(parent)[0], "path");
//        List<File> nativeLibrary = Reflect.get(parentPathList, "nativeLibraryDirectories");
        assert parent != null;
        ClassLoader classLoader = new DexClassLoader(findFirstJar().getPath() + File.pathSeparator + apk.getPath(), app.getFilesDir().getPath(), null, parent){
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
                if(!overwrite(name))
                    return super.loadClass(name, resolve);
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

                return loadedClass;
            }

            private Boolean overwrite(String name){
//                if(name.contains("ExternalSynthetic")) return false;
                if(name.startsWith("arc.func")) return false;
                return name.startsWith("mindustry") || name.startsWith("arc.") || name.startsWith("rhino.");
            }

            @Override
            public String findLibrary(String name){
                //TODO Soloud的native会查找class，而且不支持classloader。
                String file = super.findLibrary(name);
                //Android的jni方法不能跨classloader，且同一个so库加载多次
                if(file != null){
                    Fi newFile = new Fi(app.getFilesDir()).child(name + ".so");
                    new Fi(file).copyTo(newFile);
                    file = newFile.path();
                }
                return file;
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
        Object pathList = Reflect.get(BaseDexClassLoader.class, classLoader, "pathList");
        Object parentPathList = Reflect.get(BaseDexClassLoader.class, parent, "pathList");
        Reflect.set(pathList, "nativeLibraryDirectories", Reflect.get(parentPathList, "nativeLibraryDirectories"));
        Reflect.set(pathList, "nativeLibraryPathElements", Reflect.get(parentPathList, "nativeLibraryPathElements"));
        return classLoader;
    }

    private static Object[] makeDexElements(Object dexPathList, ArrayList<File> files, File optimizedDirectory){
        if(Build.VERSION.SDK_INT >= 23){
            ArrayList<IOException> suppressedExceptions = new ArrayList<>();
            return Reflect.invoke(dexPathList.getClass(), "makePathElements", new Object[]{files, optimizedDirectory, suppressedExceptions}, List.class, File.class, List.class);
        }else{
            return Reflect.invoke(dexPathList.getClass(), "makeDexElements", new Object[]{files, optimizedDirectory}, ArrayList.class, File.class);
        }
    }

    @Override
    public void launch(ClassLoader loader) throws Exception{

        Class<?> cls = loader.loadClass(AndroidImpl.class.getName());
        app.handler.post(() -> {
            ((AndroidInput)Core.input).onPause();
//            Reflect.invoke(Core.graphics, "pause");
            Core.graphics.dispose();
            Core.audio.dispose();
            app.setContentView(Reflect.invoke(cls, "bootStrapAsView",
            new Object[]{app, app.getListeners().get(0)}, Activity.class, Object.class));
        });
    }

    public static View bootStrapAsView(Activity activity, Object platform){
        AndroidApplication newApp = new AndroidLauncher();
        copyFields(activity, newApp);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.hideStatusBar = true;
        View view = newApp.initializeForView(new ClientLauncher(){

            @Override
            public void hide(){
                activity.moveTaskToBack(true);
            }

            @Override
            public rhino.Context getScriptContext(){
                return AndroidRhinoContext.enter(activity.getCacheDir());
            }

            @Override
            public void shareFile(Fi file){
            }

            @Override
            public ClassLoader loadJar(Fi jar, ClassLoader parent) throws Exception{
                return new DexClassLoader(jar.file().getPath(), activity.getFilesDir().getPath(), null, parent){
                    @Override
                    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
                        //check for loaded state
                        Class<?> loadedClass = findLoadedClass(name);
                        if(loadedClass == null){
                            try{
                                //try to load own class first
                                loadedClass = findClass(name);
                            }catch(ClassNotFoundException | NoClassDefFoundError e){
                                //use parent if not found
                                return parent.loadClass(name);
                            }
                        }

                        if(resolve){
                            resolveClass(loadedClass);
                        }
                        return loadedClass;
                    }
                };
            }

            @Override
            public void showFileChooser(boolean open, String title, String extension, Cons<Fi> cons){
                showFileChooser(open, title, cons, extension);
            }

            @Override
            public void showMultiFileChooser(Cons<Fi> cons, String... extensions){
                showFileChooser(true, "@open", cons, extensions);
            }

            void showFileChooser(boolean open, String title, Cons<Fi> cons, String... extensions){
                Cons<Object> consProxy = (fi) -> {
                    Class<?> cls = fi.getClass();
                    if(cls.isAnonymousClass()){
                        cons.get(new Fi((File)Reflect.get(fi, "file")){
                            @Override
                            public InputStream read(){
                                return Reflect.invoke(fi, "read");
                            }

                            @Override
                            public OutputStream write(boolean append){
                                return Reflect.invoke(fi, "write", new Object[]{append}, boolean.class);
                            }
                        });
                    }else if(cls.getSimpleName().equals("Fi")){
                        cons.get(new Fi((File)Reflect.get(fi, "file")));
                    }else{
                        Vars.ui.showErrorMessage("Not Implement showFileChooser");
                    }
                };
                try{
                    Method m = platform.getClass().getDeclaredMethod("showFileChooser", boolean.class, String.class, Cons.class, String[].class);
                    m.setAccessible(true);
                    m.invoke(platform, open, title, consProxy, extensions);
                }catch(Exception e){
                    Log.err(e);
                }
            }

            @Override
            public void beginForceLandscape(){
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void endForceLandscape(){
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        }, config);
        ClassLoader loader = Main.class.getClassLoader();
        assert loader != null;
        Files origin = Core.files;
        Core.files = (Files)Proxy.newProxyInstance(Main.class.getClassLoader(), new Class[]{Files.class}, (proxy, method, args) -> {
            if(method.getName().equals("internal") || (method.getName().equals("get") && args[1] == FileType.internal)){
                String path = (String)args[0];
                if(loader.getResource(path) != null){
                    return origin.classpath(path);
                }
            }
            return method.invoke(origin, args);
        });
        try{
            //new external folder
            Fi data = Core.files.absolute(newApp.getExternalFilesDir(null).getAbsolutePath());
            Core.settings.setDataDirectory(data);
        }catch(Exception e){
            //print log but don't crash
            Log.err(e);
        }
        return view;
    }

    private static void copyFields(Object a, Object b){
        Class<?> ca = a.getClass(), cb = b.getClass();
        while(ca != Object.class){
            if(ca == cb){
                for(Field f : ca.getDeclaredFields()){
                    f.setAccessible(true);
                    Reflect.set(b, f, Reflect.get(a, f));
                }
            }
            ca = ca.getSuperclass();
            cb = cb.getSuperclass();
        }
    }

    @Override
    public void cleanup(){
    }
}
