package mindustryX.features;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustryX.features.ArcWaveSpawner.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.toolpack.ArcScanMode
public class ArcSpawnerShow{
    private static final Table spawnerTable = new Table();
    private static final Table flyerTable = new Table();

    static{
        spawnerTable.touchable = flyerTable.touchable = Touchable.disabled;
        ui.hudGroup.addChild(spawnerTable);
        ui.hudGroup.addChild(flyerTable);
    }

    public static void update(boolean enabled){
        spawnerTable.clear();
        flyerTable.clear();
        if(!enabled || !state.isPlaying() || state.wave < 1) return;
        WaveInfo thisWave = ArcWaveSpawner.getOrInit(state.wave - 1);
        for(Tile tile : spawner.getSpawns()){
            if(Mathf.dst(tile.worldx(), tile.worldy(), Core.input.mouseWorldX(), Core.input.mouseWorldY()) < state.rules.dropZoneRadius){
                float curve = Mathf.curve(Time.time % 240f, 120f, 240f);
                Draw.z(Layer.effect - 2f);
                Draw.color(state.rules.waveTeam.color);
                Lines.stroke(4f);
                //flyer
                float flyerAngle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
                float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
                float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(flyerAngle, trns), 0, world.width() * tilesize);
                float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(flyerAngle, trns), 0, world.height() * tilesize);

                if(ArcWaveSpawner.hasFlyer){
                    Lines.line(tile.worldx(), tile.worldy(), spawnX, spawnY);
                    Tmp.v1.set(spawnX - tile.worldx(), spawnY - tile.worldy());
                    Tmp.v1.setLength(Tmp.v1.len() * curve);
                    Fill.circle(tile.worldx() + Tmp.v1.x, tile.worldy() + Tmp.v1.y, 8f);

                    Vec2 v = Core.camera.project(spawnX, spawnY);
                    flyerTable.setPosition(v.x, v.y);
                    flyerTable.table(Styles.black3, tt -> {
                        tt.add(FormatDefault.duration(state.wavetime / 60, false)).row();
                        tt.add(thisWave.proTable(false, tile.pos(), group -> group.type.flying));
                        tt.row();
                        tt.add(thisWave.unitTable(tile.pos(), group -> group.type.flying)).maxWidth(mobile ? 400f : 750f).growX();
                    });
                }
                //ground

                if(curve > 0)
                    Lines.circle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius * Interp.pow3Out.apply(curve));
                Lines.circle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                Lines.arc(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius - 3f, state.wavetime / state.rules.waveSpacing, 90f);
                float angle = Mathf.pi / 2 + state.wavetime / state.rules.waveSpacing * 2 * Mathf.pi;
                Draw.color(state.rules.waveTeam.color);
                Fill.circle(tile.worldx() + state.rules.dropZoneRadius * Mathf.cos(angle), tile.worldy() + state.rules.dropZoneRadius * Mathf.sin(angle), 8f);

                Vec2 v = Core.camera.project(tile.worldx(), tile.worldy());
                spawnerTable.setPosition(v.x, v.y);
                spawnerTable.table(Styles.black3, tt -> {
                    tt.add(FormatDefault.duration(state.wavetime / 60, false)).row();
                    tt.add(thisWave.proTable(false, tile.pos(), group -> !group.type.flying));
                    tt.row();
                    tt.add(thisWave.unitTable(tile.pos(), group -> !group.type.flying)).maxWidth(mobile ? 400f : 750f).growX();
                });
                return;
            }
        }
    }
}
