package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustryX.events.*;
import mindustryX.events.HealthChangedEvent.*;

import java.util.*;

import static arc.util.Tmp.*;

/**
 * 玩家子弹伤害跳字
 * Create by 2024/6/5
 */
public class DamagePopup{
    private static final Pool<Popup> popupPool = Pools.get(Popup.class, Popup::new);

    /** 所有的跳字 */
    private static final Seq<Popup> popups = new Seq<>();
    private static final ObjectMap<Sized, ObjectMap<DamageType, Popup>> mappedPopup = new ObjectMap<>();

    // 跳字初始缩放限制
    public static final float minScale = 1f / 4 / Scl.scl(1);
    public static final float maxScale = 1f / 2 / Scl.scl(1);

    // 无持续攻击的消退时间
    public static float popupLifetime = 60f;

    // 设置
    public static boolean enable;
    public static boolean playerPopupOnly, healPopup;
    public static float popupMinHealth;

    public static void init(){
        Events.on(HealthChangedEvent.class, e -> {
            if(enable && e.entity instanceof Sized entitySized && shouldPopup(e.source, e.entity, e.type)){
                popup(e.source, entitySized, e.type, e.amount);
            }
        });

        Events.run(Trigger.update, () -> {
            updateSettings();

            if(Vars.state.isPaused()) return;
            if(popups.isEmpty()) return;

            Iterator<Popup> iterator = popups.iterator();

            while(iterator.hasNext()){
                Popup popup = iterator.next();

                if(popup.dead()){
                    if(popup.damaged != null && superpose(popup.type)){
                        mappedPopup.get(popup.damaged, ObjectMap::new).remove(popup.type);
                    }

                    iterator.remove();

                    popupPool.free(popup);
                }else{
                    popup.update();
                }
            }
        });

        Events.run(Trigger.draw, () -> {
            if(popups.isEmpty()) return;

            Rect cameraBounds = Core.camera.bounds(r1).grow(4 * Vars.tilesize);

            for(Popup popup : popups){
                if(cameraBounds.contains(popup.getX(), popup.getY())){
                    popup.draw();
                }
            }
        });

        Events.on(ResetEvent.class, e -> {
            clearPopup();
        });
    }

    private static void updateSettings(){
        enable = Core.settings.getBool("damagePopup");
        healPopup = Core.settings.getBool("healPopup");
        playerPopupOnly = Core.settings.getBool("playerPopupOnly");
        popupMinHealth = Core.settings.getInt("popupMinHealth");

//        if(enable != enableSetting){
//            enable = enableSetting;
//
//            // 关闭后保留已有跳字
//            // if(!enable) clearPopup();
//        }
    }

    public static void clearPopup(){
        popupPool.freeAll(popups);
        popups.clear();

        for(ObjectMap<DamageType, Popup> map : mappedPopup.values()){
            map.clear();
        }
        mappedPopup.clear();
    }

    private static Entityc getSourceOwner(Ownerc source){
        Entityc current = source.owner();

        while(current instanceof Ownerc o && o.owner() != null){
            current = o.owner();
        }

        return current;
    }

    private static boolean shouldPopup(Sized source, Healthc damaged, DamageType type){
        if(damaged.maxHealth() < popupMinHealth){
            return false;
        }

        if(type == DamageType.heal && !healPopup) return false;
        if(source == null) return true;

        // 视角外的跳字
        Rect cameraBounds = Core.camera.bounds(r1).grow(4 * Vars.tilesize);
        if(!cameraBounds.contains(damaged.getX(), damaged.getY())){
            return false;
        }

        if(!playerPopupOnly) return true;

        if(!(source instanceof Ownerc sourceOwner)) return true;

        Entityc owner = getSourceOwner(sourceOwner);
        Unit playerUnit = Vars.player.unit();

        return owner == playerUnit
        || (playerUnit instanceof BlockUnitUnit blockUnit && owner == blockUnit.tile())
        || (Vars.control.input.commandMode &&
        (owner instanceof Unit unitOwner && Vars.control.input.selectedUnits.contains(unitOwner)
        || (owner instanceof Building buildOwner && Vars.control.input.commandBuildings.contains(buildOwner))));
    }

    private static void popup(Sized source, Sized damaged, DamageType type, float amount){
        if(Mathf.equal(amount, 0f)) return;

        float hitSize = damaged.hitSize();

        float offsetX, offsetY;
        float rotation = source != null
        ? damaged.angleTo(source) + Mathf.random(35f)
        : 90 + Mathf.range(35f);

        float scale = Mathf.clamp(hitSize / 64 / Scl.scl(1), minScale, maxScale);
        float offsetLength = hitSize * Mathf.random(0.4f, 0.7f);

        if(superpose(type)){
            offsetX = Angles.trnsx(rotation, hitSize * Mathf.random(0.2f, 0.4f));
            offsetY = Angles.trnsy(rotation, hitSize * Mathf.random(0.2f, 0.4f));
        }else{
            offsetX = Angles.trnsx(rotation, hitSize * Mathf.random(0.3f, 0.4f));
            offsetY = Angles.trnsy(rotation, hitSize * Mathf.random(0.3f, 0.4f));

            scale *= 0.65f; // 堆叠的跳字有更大的效果
        }

        if(superpose(type)){
            ObjectMap<DamageType, Popup> map = mappedPopup.get(damaged, ObjectMap::new);
            Popup popup = map.get(type);

            if(popup == null){
                popup = popupPool.obtain().set(damaged, type, offsetX, offsetY, popupLifetime, Math.abs(amount), 1f, scale, rotation, offsetLength);
                map.put(type, popup);
                popups.add(popup);
            }else{
                popup.superposeAmount(Math.abs(amount));
            }
        }else{
            Popup popup = popupPool.obtain().set(damaged, type, offsetX, offsetY, popupLifetime, Math.abs(amount), 1f, scale, rotation, offsetLength);
            popups.add(popup);
        }
    }

    private static boolean superpose(DamageType type){
        return type == DamageType.normal || type == DamageType.heal;
    }

    private static class Popup implements Poolable, Position{
        public static float maxAmountEffect = 5_000;
        public static int maxCountEffect = 50;
        public static float amountEffect = 3f;
        public static float countEffect = 2f;
        public static float fontScaleEffectScl = 8f;
        public static float splashTime = 15f;

        // data
        public DamageType type;
        public Font font = Fonts.outline;
        public Sized damaged;
        public float originX, originY;
        public float offsetX, offsetY;
        public float lifetime;
        public float alpha;
        public float scale;
        public float offsetLength;
        public float rotation; // deg

        public float amount;
        public int count;

        private float timer;
        private float floatTimer;
        private float splashTimer;

        public Popup set(Sized damaged, DamageType type, float offsetX, float offsetY, float lifetime, float amount, float alpha, float scale, float rotation, float offsetLength){
            this.damaged = damaged;

            this.type = type;

            this.originX = damaged.getX();
            this.originY = damaged.getY();

            this.offsetX = offsetX;
            this.offsetY = offsetY;

            this.lifetime = lifetime;
            this.amount = amount;
            this.alpha = alpha;
            this.scale = scale;
            this.rotation = rotation;
            this.offsetLength = offsetLength;

            return this;
        }

        public void draw(){
            float fin = timer / lifetime;

            float alphaScaleEase = Bezier.quadratic(v1, fin,
            v2.set(1f, 1f),
            v3.set(0f, 1f),
            v4.set(0f, 0.8f),
            v5.set(0f, 0.5f)).y;

            float alpha = this.alpha * alphaScaleEase;
            float scale = this.scale * alphaScaleEase * Math.max(effect() / fontScaleEffectScl, 1);

            float fx = getX() + offsetX, fy = getY() + offsetY;

            if(!superpose(type)){
                float positionEase = Bezier.quadratic(v1, fin,
                v2.set(0f, 0f),
                v3.set(0.19f, 1f),
                v4.set(0.22f, 1f),
                v5.set(1f, 1f)).y;

                float offsetLength = this.offsetLength * positionEase;

                fx += Angles.trnsx(rotation, offsetLength);
                fy += Angles.trnsy(rotation, offsetLength);
            }

            c1.set(type.color).a(alpha).lerp(Color.white, splashTimer / splashTime * 0.75f);

            String text = (type.icon != null ? type.icon : "") + Strings.autoFixed(amount, 1);
            Draw.z(Layer.overlayUI);
            font.draw(text, fx, fy, c1, scale, false, Align.center);
            Draw.reset();
        }

        public boolean dead(){
            return timer >= lifetime;
        }

        public void update(){
            if(floatTimer > 0){
                floatTimer = Math.max(0, floatTimer - Time.delta);
            }else{
                timer += Time.delta;
            }

            if(splashTimer > 0){
                splashTimer = Math.max(0, splashTimer - Time.delta);
            }
        }

        protected float effect(){
            float damageEffect = Popup.amountEffect * Math.min(amount / maxAmountEffect, 1);
            float countEffect = Popup.countEffect * Math.min(count / maxCountEffect, 1);
            return 1f + damageEffect + countEffect;
        }

        public void superposeAmount(float amount){
            this.amount += amount;
            count++;

            floatTimer = lifetime;
            splashTimer = splashTime;
        }

        @Override
        public void reset(){
            damaged = null;

            type = DamageType.normal;

            offsetX = 0;
            offsetY = 0;

            lifetime = 0;
            alpha = 0;
            scale = 0;
            offsetLength = 0;
            rotation = 0;

            amount = 0f;
            count = 0;
            timer = 0f;
        }

        @Override
        public float getX(){
            return !superpose(type) ? originX : damaged.getX();
        }

        @Override
        public float getY(){
            return !superpose(type) ? originY : damaged.getY();
        }
    }
}
