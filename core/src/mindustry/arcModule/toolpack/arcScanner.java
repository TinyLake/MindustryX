package mindustry.arcModule.toolpack;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.arcModule.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.DrawUtilities.drawText;

public class arcScanner{

    /** 基础缩放倍率，最重要的参数 */
    private static final float ratio = 10f;
    private static final float unitSize = 0.1f;
    private static final float markerSize = 15f * tilesize;
    /** 范围倍率 */
    private static final int basicRadarCir = 25;
    private static final Table t = new Table(Styles.black3);
    public static boolean mobileRadar = false;
    /** 真实大小 */
    private static float rRatio;
    private static float rMarkerSize;
    /** 每多少范围一个雷达圈 */
    private static float radarCir = 25f;
    /** 默认扫描时间，仅用于特效 */
    private static float scanTime = 5;
    /** 当前扫描的百分比 */
    private static float scanRate = 0;
    /** 扫描线旋转倍率 */
    private static final float scanSpeed = -0.02f;
    /** 实际扫描范围，不是参数 */
    private static float curScanRange = 0;
    private static float expandRate = 1f;
    private static float time = 0;

    static{
        t.touchable = Touchable.disabled;
        t.margin(8f).add(">> 雷达扫描中 <<").color(ARCVars.getThemeColor()).style(Styles.outlineLabel).labelAlign(Align.center);
        t.visible = false;
        t.update(() -> t.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() * 0.1f, Align.center));
        t.pack();
        t.act(0.1f);
        t.update(() -> t.visible = t.visible && state.isPlaying());

        Core.scene.add(t);

        Events.on(EventType.WorldLoadEvent.class, event -> scanTime = Math.max(Mathf.dst(world.width(), world.height()) / 20f, 7.5f));
    }

    public static void drawScanner(){
        if(Core.settings.getInt("radarMode") == 0) return;
        float extendSpd = Core.settings.getInt("radarMode") * 0.2f;
        Draw.reset();

        if(mobile){
            if(extendSpd >= 6){
                t.visible = mobileRadar;
                scanRate = t.visible ? 1f : 0f;
            }else{
                if(mobileRadar){
                    t.visible = true;
                    if(scanRate < 1f) scanRate = Math.min(scanRate + 1 / 60f / scanTime * extendSpd, 1f);
                }else{
                    t.visible = false;
                    if(scanRate > 0f) scanRate = Math.max(scanRate - 3 / 60f / scanTime * extendSpd, 0f);
                }
            }
        }else{
            if(extendSpd >= 6){
                if(Core.input.keyDown(Binding.arcDetail) && Time.time - time > 60f){
                    time = Time.time;
                    t.visible = !t.visible;
                    scanRate = t.visible ? 1f : 0f;
                }
            }else{
                if(Core.input.keyDown(Binding.arcDetail)){
                    t.visible = true;
                    if(scanRate < 1f) scanRate = Math.min(scanRate + 1 / 60f / scanTime * extendSpd, 1f);
                }else{
                    t.visible = false;
                    if(scanRate > 0f) scanRate = Math.max(scanRate - 3 / 60f / scanTime * extendSpd, 0f);
                }
            }
        }

        if(scanRate <= 0) return;

        float playerToBorder = Math.max(Math.max(Math.max(Mathf.dst(player.tileX(), player.tileY()), Mathf.dst(world.width() - player.tileX(), player.tileY())), Mathf.dst(world.width() - player.tileX(), world.height() - player.tileY())), Mathf.dst(player.tileX(), world.height() - player.tileY()));
        float worldSize = Math.min(playerToBorder, (int)(Mathf.dst(world.width(), world.height()) / radarCir) * radarCir);

        float playerSize = Math.min(world.width(), world.height()) * tilesize * 0.03f;

        /** 整体缩放倍率，最重要的可调参数 */
        float sizeRate = Core.settings.getInt("radarSize") == 0 ? 1f : Core.settings.getInt("radarSize") * 0.1f / renderer.getScale();
        sizeRate *= Math.min(Core.scene.getHeight() / (world.height() * tilesize), Core.scene.getWidth() / (world.width() * tilesize)) * 2f;
        rRatio = ratio / sizeRate;
        float rUnitSize = unitSize * sizeRate;
        rMarkerSize = markerSize * sizeRate;


        expandRate = worldSize / basicRadarCir / 10 + 1;
        radarCir = (int)expandRate * basicRadarCir;  //地图越大，radar间隔越大。此处选择最多10圈
        curScanRange = worldSize * tilesize * scanRate;

        expandRate *= sizeRate;

        for(int i = 1; i < curScanRange / radarCir / tilesize + 1; i++){
            Draw.color(player.team().color, 0.45f);
            Lines.stroke(expandRate * 0.75f);
            Lines.circle(player.x, player.y, (radarCir * i * tilesize) / rRatio);
            float cirRatio = (radarCir * i * tilesize) / rRatio + 2f;
            drawText(i * (int)radarCir + "", 0.2f / Scl.scl(1f) * expandRate, player.x, player.y + cirRatio, ARCVars.getThemeColor(), Align.center);
            drawText(i * (int)radarCir + "", 0.2f / Scl.scl(1f) * expandRate, player.x + cirRatio * Mathf.cos(Mathf.PI * 7 / 6), player.y + cirRatio * Mathf.sin(Mathf.PI * 7 / 6), ARCVars.getThemeColor(), Align.center);
            drawText(i * (int)radarCir + "", 0.2f / Scl.scl(1f) * expandRate, player.x + cirRatio * Mathf.cos(Mathf.PI * 11 / 6), player.y + cirRatio * Mathf.sin(Mathf.PI * 11 / 6), ARCVars.getThemeColor(), Align.center);
        }

        if(scanRate < 1f){
            Draw.color(player.team().color, 0.8f);
            Lines.stroke(expandRate);
            Lines.circle(player.x, player.y, curScanRange / rRatio);
            Draw.color(player.team().color, 0.1f);
            Fill.circle(player.x, player.y, curScanRange / rRatio);
        }else{
            curScanRange = (int)(curScanRange / radarCir / tilesize + 1) * radarCir * tilesize;

            Draw.color(player.team().color, 0.1f);
            Fill.circle(player.x, player.y, curScanRange / rRatio);

            Draw.color(player.team().color, 0.6f);
            float curve = Mathf.curve(Time.time % 360f, 120f, 360f);
            Lines.stroke(expandRate * 1.5f);
            Lines.circle(player.x, player.y, curScanRange / rRatio);
            Lines.stroke(expandRate * 1.5f);
            Lines.circle(player.x, player.y, curScanRange * Interp.pow3Out.apply(curve) / rRatio);
            Lines.stroke(expandRate * 1.5f);

            Draw.color(player.team().color, 0.1f);
            Fill.rect(player.x - player.x / rRatio + world.width() * tilesize / rRatio / 2, player.y - player.y / rRatio + world.height() * tilesize / rRatio / 2, world.width() * tilesize / rRatio, world.height() * tilesize / rRatio);
            Draw.color(player.team().color, 0.85f);
            Lines.rect(player.x - player.x / rRatio, player.y - player.y / rRatio, world.width() * tilesize / rRatio, world.height() * tilesize / rRatio);
        }

        Draw.color(player.team().color, 0.8f);
        Lines.line(player.x, player.y, player.x + curScanRange * Mathf.cos(Time.time * scanSpeed) / rRatio, player.y + curScanRange * Mathf.sin(Time.time * scanSpeed) / rRatio);
        Draw.reset();

        // 出怪点
        if(spawner.countSpawns() < 25 && !state.rules.pvp){
            for(Tile tile : spawner.getSpawns()){
                if(scanRate < 1f && Mathf.dst(tile.worldx() - player.x, tile.worldy() - player.y) > curScanRange)
                    continue;

                Draw.color(state.rules.waveTeam.color, 1f);
                arcDrawNearby(Icon.units.getRegion(), tile, Math.max(6 * expandRate, state.rules.dropZoneRadius / rRatio / 2), state.rules.waveTeam.color);

                float curve = Mathf.curve(Time.time % 200f, 60f, 200f);
                Draw.color(state.rules.waveTeam.color, 1f);
                Lines.stroke(expandRate);
                Lines.circle(transX(tile.worldx()), transY(tile.worldy()), state.rules.dropZoneRadius * Interp.pow3Out.apply(curve) / rRatio);
                Draw.color(state.rules.waveTeam.color, 0.5f);
                Lines.stroke(expandRate * 0.8f);
                Lines.dashCircle(transX(tile.worldx()), transY(tile.worldy()), state.rules.dropZoneRadius / rRatio);
            }
        }
        //绘制核心
        for(Team team : Team.all){
            for(CoreBlock.CoreBuild core : team.cores()){
                if(state.rules.pvp && core.inFogTo(player.team())) continue;
                if(scanRate < 1f && Mathf.dst(core.x - player.x, core.y - player.y) > curScanRange) continue;
                Draw.color(core.team.color, 1f);
                Draw.rect(core.block.fullIcon, transX(core.tile.worldx()), transY(core.tile.worldy()), 4 * expandRate, 4 * expandRate);

            }
        }
        //绘制搜索的方块
        for(Building build : ui.hudfrag.quickToolTable.advanceBuildTool.buildingSeq){
            if(scanRate < 1f && Mathf.dst(build.x - player.x, build.y - player.y) > curScanRange) continue;
            Draw.color(build.team.color, 1f);
            Draw.rect(build.block.fullIcon, transX(build.tile.worldx()), transY(build.tile.worldy()), 4 * expandRate, 4 * expandRate);
        }
        //绘制单位
        for(Unit unit : Groups.unit){
            if(scanRate < 1f && Mathf.dst(unit.x - player.x, unit.y - player.y) > curScanRange) continue;
            Draw.color(unit.team.color, 0.6f);
            Fill.circle(transX(unit.x), transY(unit.y), unit.hitSize * rUnitSize);
        }
        //绘制玩家
        for(Player unit : Groups.player){
            if(player.dead() || player.unit().health <= 0) continue;
            if(scanRate < 1f && Mathf.dst(unit.x - player.x, unit.y - player.y) > curScanRange) continue;

            Draw.color(unit.team().color, 0.9f);

            float angle = unit.unit().rotation * Mathf.degreesToRadians;
            Fill.tri(transX(unit.x + Mathf.cos(angle) * playerSize), transY(unit.y + Mathf.sin(angle) * playerSize),
            transX(unit.x + Mathf.cos(angle + Mathf.PI * 2 / 3) * playerSize * 0.75f), transY(unit.y + Mathf.sin(angle + Mathf.PI * 2 / 3) * playerSize * 0.75f),
            transX(unit.x + Mathf.cos(angle + Mathf.PI * 4 / 3) * playerSize * 0.75f), transY(unit.y + Mathf.sin(angle + Mathf.PI * 4 / 3) * playerSize * 0.75f));
        }
        //绘制arc标记
        if(Marker.markList.size > 0){
            Marker.markList.each(a -> {
                if((Time.time - a.time) > Marker.retainTime) return;
                Draw.color(a.markType.color);
                Lines.stroke(expandRate * (1 - (Time.time % 180 + 30) / 210));

                Lines.circle(transX(a.markPos.x), transY(a.markPos.y), rMarkerSize / rRatio * (Time.time % 180) / 180);
                Lines.stroke(expandRate);
                Lines.circle(transX(a.markPos.x), transY(a.markPos.y), rMarkerSize / rRatio);
                Lines.arc(transX(a.markPos.x), transY(a.markPos.y), (rMarkerSize - expandRate) / rRatio, 1 - (Time.time - a.time) / Marker.retainTime);
                Draw.reset();
            });
        }

    }

    /**
     * 以玩家为中心，绘制一个环绕的图标圈
     */

    public static void arcDrawNearbyRot(TextureRegion region, Tile tile, float size, boolean rotate, Color color){
        float range = Mathf.dst(tile.worldy() - player.y, tile.worldx() - player.x);
        if(range > curScanRange) return;

        float rot = (float)Math.atan2(tile.worldy() - player.y, tile.worldx() - player.x);
        float nx = player.x + Mathf.cos(rot) * range / rRatio;
        float ny = player.y + Mathf.sin(rot) * range / rRatio;
        if(rotate) Draw.rect(region, nx, ny, size, size, (float)Math.toDegrees(rot));
        else Draw.rect(region, nx, ny, size, size);
        drawText((int)(range / 8f) + "", 0.2f / Scl.scl(1f) * expandRate, nx, ny + size / 2, color, Align.center);
    }

    public static void arcDrawNearby(TextureRegion region, Tile tile, float size, Color color){
        float range = Mathf.dst(tile.worldy() - player.y, tile.worldx() - player.x);
        if(range > curScanRange) return;
        float nx = player.x + (tile.worldx() - player.x) / rRatio;
        float ny = player.y + (tile.worldy() - player.y) / rRatio;
        Draw.rect(region, nx, ny, size, size);
    }

    private static float transX(float x){
        return player.x + (x - player.x) / rRatio;
    }

    private static float transY(float y){
        return player.y + (y - player.y) / rRatio;
    }

}
