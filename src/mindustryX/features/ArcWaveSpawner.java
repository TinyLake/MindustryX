package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.toolpack.arcWaveSpawner
public class ArcWaveSpawner{
    public static boolean hasFlyer = true;
    public static final float flyerSpawnerRadius = 5f * tilesize;
    public static final Seq<waveInfo> arcWave = new Seq<>();

    static{
        Events.on(EventType.WorldLoadEvent.class, event -> {
            hasFlyer = false;
            for(SpawnGroup sg : state.rules.spawns){
                if(sg.type.flying){
                    hasFlyer = true;
                    break;
                }
            }
            arcWave.clear();
            for(int i = 0; i <= calWinWave(); i++){
                arcWave.add(new waveInfo(i));
            }
        });
    }

    public static void drawFlyerSpawner(){
        if(hasFlyer && Core.settings.getBool("showFlyerSpawn") && spawner.countSpawns() < 20){
            for(Tile tile : spawner.getSpawns()){
                float angle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
                float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
                float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(angle, trns), 0, world.width() * tilesize);
                float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(angle, trns), 0, world.height() * tilesize);
                if(Core.settings.getBool("showFlyerSpawnLine")){
                    Draw.color(Color.red, 0.5f);
                    Lines.line(tile.worldx(), tile.worldy(), spawnX, spawnY);
                }
                Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
                Draw.alpha(0.8f);
                arcDashCircling(spawnX, spawnY, flyerSpawnerRadius, 0.1f);

                Draw.color();
                Draw.alpha(0.5f);
                Draw.rect(UnitTypes.zenith.fullIcon, spawnX, spawnY);
            }
        }
    }

    public static waveInfo getOrInit(int wave){
        while(arcWave.size <= wave) arcWave.add(new waveInfo(wave));
        return arcWave.get(wave);
    }

    public static int calWinWave(){
        if(state.rules.winWave >= 1) return state.rules.winWave;
        int maxwave = 0;
        for(SpawnGroup group : state.rules.spawns){
            if(group.end > 99999) continue;
            maxwave = Math.max(maxwave, group.end);
        }
        if(maxwave == 0 && state.rules.waveSpacing > 10f * Time.toSeconds) maxwave = (int)(120 * Time.toMinutes / state.rules.waveSpacing);
        return Math.min(maxwave + 1, 1000);
    }

    public static void arcDashCircling(float x, float y, float radius, float speed){
        arcDashCircle(x, y, radius, Time.time * speed);
    }

    public static void arcDashCircle(float x, float y, float radius, float rotation){
        float scaleFactor = 0.6f;
        int sides = 10 + (int)(radius * scaleFactor);
        if(sides % 2 == 1) sides++;

        for(int i = 0; i < sides; i += 2){
            var v = Tmp.v1;
            v.set(radius, 0).rotate(360f / sides * i + 90 + rotation);
            float x1 = v.x, y1 = v.y;
            v.set(radius, 0).rotate(360f / sides * (i + 1) + 90 + rotation);
            float x2 = v.x, y2 = v.y;
            Lines.line(x + x1, y + y1, x + x2, y + y2);
        }
    }

    /**
     * 单一波次详情
     */
    public static class waveInfo{
        public final int waveIndex;
        public final Seq<waveGroup> groups = new Seq<>();

        public int amount = 0, amountL = 0;

        public float health = 0, effHealth = 0, dps = 0;
        /**
         * 临时数据记录
         */
        public long healthL = 0, effHealthL = 0, dpsL = 0;

        waveInfo(int waveIndex){
            this.waveIndex = waveIndex;
            for(SpawnGroup group : state.rules.spawns){
                int amount = group.getSpawned(waveIndex);
                if(amount == 0) continue;
                groups.add(new waveGroup(waveIndex, group));
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

        public void specLoc(int spawn, Boolf<SpawnGroup> pre){
            amountL = 0;
            healthL = 0;
            effHealthL = 0;
            dpsL = 0;
            groups.each(waveGroup -> (spawn == -1 || waveGroup.group.spawn == -1 || waveGroup.group.spawn == spawn) && pre.get(waveGroup.group),
            group -> {
                amountL += group.amountT;
                healthL += group.healthT;
                effHealthL += group.effHealthT;
                dpsL += group.dpsT;
            });
        }

        public Table proTable(boolean doesRow){
            if(amountL == 0) return new Table(t -> t.add("该波次没有敌人"));
            return new Table(t -> {
                t.add("\uE86D").width(50f);
                t.add("[accent]" + amountL).growX().padRight(50f);
                if(doesRow) t.row();
                t.add("\uE813").width(50f);
                t.add("[accent]" + UI.formatAmount(healthL)).growX().padRight(50f);
                if(doesRow) t.row();
                if(effHealthL != healthL){
                    t.add("\uE810").width(50f);
                    t.add("[accent]" + UI.formatAmount(effHealthL)).growX().padRight(50f);
                    if(doesRow) t.row();
                }
                t.add("\uE86E").width(50f);
                t.add("[accent]" + UI.formatAmount(dpsL)).growX();
            });
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
                        ttt.add(wg.group.type.typeColor() + wg.amount).fillX();
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
    public static class waveGroup{
        public final int waveIndex;
        public final SpawnGroup group;
        public final int amount;
        public final int amountT;
        public final float shield;
        public final float health;
        public float effHealth;
        public float dps;
        public final float healthT;
        public final float effHealthT;
        public final float dpsT;

        public waveGroup(int waveIndex, SpawnGroup group){
            this.waveIndex = waveIndex;
            this.group = group;
            this.amount = group.getSpawned(waveIndex);
            this.shield = group.getShield(waveIndex);   //盾
            this.health = (group.type.health + shield) * amount;   //盾+血
            this.dps = group.type.estimateDps() * amount;
            this.effHealth = health;
            if(group.effect != null){
                this.effHealth *= group.effect.healthMultiplier;
                this.dps *= group.effect.damageMultiplier * group.effect.reloadMultiplier;
            }

            int multiplier = group.spawn != -1 || spawner.countSpawns() < 2 ? 1 : spawner.countSpawns();
            this.amountT = amount * multiplier;
            this.healthT = health * multiplier;
            this.effHealthT = effHealth * multiplier;
            this.dpsT = dps * multiplier;
        }
    }
}