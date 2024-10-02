package mindustryX.features;

import arc.files.*;
import arc.struct.*;
import mindustry.core.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustryX.*;
import mindustryX.mods.claj.*;

import static arc.Core.files;
import static mindustry.Vars.modDirectory;

public class InternalMods{
    public static Seq<LoadedMod> load(){
        Seq<LoadedMod> mods = new Seq<>();
        mods.add(internalMod(meta("claj", "Claj联机", "1.1", "[#0096FF]xzxADIxzx cong重写 WayZer合并进MDTX"), new Claj()));
        mods.add(internalMod(meta("Kotlin", "Kotlin语言标准库", "1.9.20", "Jetbrains")));
        if(!VarsX.isLoader)
            mods.add(internalMod(meta("MindustryX", "MindustryX", VarsX.version, "")));
        return mods;
    }

    private static ModMeta meta(String id, String displayName, String version, String author){
        ModMeta meta = new ModMeta();
        meta.name = id;
        meta.displayName = "[内置]" + displayName;
        meta.version = version;
        meta.author = author;
        meta.minGameVersion = Version.buildString();
        meta.hidden = true;
        meta.cleanup();
        return meta;
    }

    private static LoadedMod internalMod(ModMeta meta, Mod main){
        Fi file = modDirectory.child("internal-" + meta.name + ".jar");
        Fi root = files.internal("/mindustryX/mods/" + meta.name);
        return new LoadedMod(file, root, main, InternalMods.class.getClassLoader(), meta);
    }

    private static LoadedMod internalMod(ModMeta meta){
        return internalMod(meta, new Mod(){
        });
    }
}
