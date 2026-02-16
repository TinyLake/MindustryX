package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.toolpack.arcWaveSpawner
public class ArcWaveSpawner{
    public static boolean hasFlyer = true;
    private static final Seq<WaveInfo> arcWaveCache = new Seq<>();
    private static Seq<SpawnGroup> groups = state.rules.spawns;

    static{
        Events.on(EventType.WorldLoadEvent.class, event -> reload(state.rules.spawns));
    }

    public static void reload(Seq<SpawnGroup> groups){
        ArcWaveSpawner.groups = groups;
        hasFlyer = groups.contains(sg -> sg.type.flying);
        arcWaveCache.clear();
    }

    public static WaveInfo getOrInit(int wave){
        wave = Math.min(wave, calWinWaveClamped());
        while(arcWaveCache.size <= wave) arcWaveCache.add(new WaveInfo(arcWaveCache.size));
        return arcWaveCache.get(wave);
    }

    public static int calWinWave(){
        if(state.rules.winWave >= 1) return state.rules.winWave;
        int maxwave = 1;
        for(SpawnGroup group : groups){
            maxwave = Math.max(maxwave, group.end);
        }
        return maxwave;
    }

    public static int calWinWaveClamped(){
        return Math.min(calWinWave(), 10000);
    }

    public static Color unitTypeColor(UnitType type){
        if(type.naval) return Color.cyan;
        if(type.allowLegStep) return Color.magenta;
        if(type.flying) return Color.acid;
        if(type.hovering) return Color.sky;
        return Pal.stat;
    }

    /**
     * 单一波次详情
     */
    public static class WaveInfo{
        public final int wave;//begin from 0
        public final Seq<WaveGroup> groups = new Seq<>();

        public int amount = 0;
        public float health = 0, effHealth = 0, dps = 0;

        WaveInfo(int wave){
            this.wave = wave;
            for(SpawnGroup group : ArcWaveSpawner.groups){
                int amount = group.getSpawned(wave);
                if(amount == 0) continue;
                groups.add(new WaveGroup(wave, group));
            }
            initProperty();
        }

        private void initProperty(){
            groups.each(group -> {
                amount += group.amountT;
                health += group.healthT;
                effHealth += group.effHealthT;
                dps += group.dpsT;
            });
        }

        public Table proTable(boolean doesRow, int spawn, Boolf<SpawnGroup> filter){
            int amount = 0;
            float health = effHealth = dps = 0;
            for(var group : groups){
                if(spawn != -1 && group.group.spawn != -1 && group.group.spawn != spawn) continue;
                if(!filter.get(group.group)) continue;
                amount += group.amountT;
                health += group.healthT;
                effHealth += group.effHealthT;
                dps += group.dpsT;
            }

            if(amount == 0) return new Table(t -> t.add(arc.Core.bundle.get("mdtx.ui.there_are_no_enemies_in_this_wave"))); // 原文本:该波次没有敌人
            Table t = new Table();
            t.add("\uE86D").width(50f);
            t.add("[accent]" + amount).growX().padRight(50f);
            if(doesRow) t.row();
            t.add("\uE813").width(50f);
            t.add("[accent]" + UI.formatAmount((long)health)).growX().padRight(50f);
            if(doesRow) t.row();
            if(effHealth != health){
                t.add("\uE810").width(50f);
                t.add("[accent]" + UI.formatAmount((long)effHealth)).growX().padRight(50f);
                if(doesRow) t.row();
            }
            t.add("\uE86E").width(50f);
            t.add("[accent]" + UI.formatAmount((long)dps)).growX();
            return t;
        }

        public Table unitTable(int spawn, Boolf<SpawnGroup> pre){
            return unitTable(spawn, pre, 10);
        }

        public Table unitTable(int spawn, Boolf<SpawnGroup> pre, int perCol){
            int[] count = new int[1];
            return new Table(t -> groups.each(waveGroup -> (spawn == -1 || waveGroup.group.spawn == -1 || waveGroup.group.spawn == spawn) && pre.get(waveGroup.group), wg -> {
                count[0]++;
                if(count[0] % perCol == 0) t.row();
                t.table(tt -> {
                    tt.table(ttt -> {
                        ttt.image(wg.group.type.uiIcon).size(30);
                        ttt.add("" + wg.amount).color(unitTypeColor(wg.group.type)).fillX();
                    }).row();
                    StringBuilder groupInfo = new StringBuilder();
                    if(wg.shield > 0f)
                        groupInfo.append(FormatDefault.format(wg.shield));
                    groupInfo.append("\n[]");
                    if(wg.group.spawn != -1 && spawn == -1) groupInfo.append("*");
                    if(wg.group.effect != null && wg.group.effect != StatusEffects.none)
                        groupInfo.append(wg.group.effect.emoji());
                    if(wg.group.items != null && wg.group.items.amount > 0)
                        groupInfo.append(wg.group.items.item.emoji());
                    if(wg.group.payloads != null && wg.group.payloads.size > 0)
                        groupInfo.append("\uE87B");
                    tt.add(groupInfo.toString()).fill();
                }).height(80f).width(70f);

            }));
        }

    }

    /**
     * 一种更为详细的spawnGroup
     */
    public static class WaveGroup{
        public final int wave;
        public final SpawnGroup group;
        public final int amount;
        public final float shield, health, effHealth, dps;
        public final int amountT;
        public final float healthT, effHealthT, dpsT;

        public WaveGroup(int wave, SpawnGroup group){
            this.wave = wave;
            this.group = group;
            this.amount = group.getSpawned(wave);
            this.shield = group.getShield(wave);   //盾
            this.health = (group.type.health + shield) * amount;   //盾+血
            this.effHealth = health * (group.effect != null ? group.effect.healthMultiplier : 1f); //血*效果
            this.dps = group.type.estimateDps() * amount * (group.effect != null ? group.effect.damageMultiplier * group.effect.reloadMultiplier : 1f);

            int multiplier = group.spawn != -1 || spawner.countSpawns() < 2 ? 1 : spawner.countSpawns();
            this.amountT = amount * multiplier;
            this.healthT = health * multiplier;
            this.effHealthT = effHealth * multiplier;
            this.dpsT = dps * multiplier;
        }
    }
}
