package mindustryX.features.draw;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustryX.features.func.*;

import static mindustry.Vars.state;

public class ConstructSelect{
    private static final int samplePeriod = 10;
    private static final Interval timer = new Interval();

    private static final float alpha = 0.5f;
    private static float smoothSpeed = 0f;

    private static int lastId;
    private static float lastProgress;

    public static void draw(ConstructBuild constructBuild){
        if(constructBuild.team.core() == null){
            return;
        }

        if(lastId != constructBuild.id){
            lastId = constructBuild.id;
            lastProgress = 0f;
            smoothSpeed = 0f;
            timer.clear();
        }

        float scl = constructBuild.block.size / 4f;
        float buildHitSize = constructBuild.hitSize();

        Block current = constructBuild.current;
        float progress = constructBuild.progress;

        // 显示建造进度
        var pos = Tmp.v1.set(constructBuild).add(0, buildHitSize / 2f);//顶部

        if(!state.isPaused()){
            if(timer.get(samplePeriod)){
                float rawSpeed = (progress - lastProgress) / samplePeriod;
                smoothSpeed = Mathf.lerp(smoothSpeed, rawSpeed, alpha);
                lastProgress = progress;
            }
        }
        float speed = smoothSpeed;

        String timeStr = "";
        if(!Mathf.zero(speed)){
            float leftTicks = (speed > 0 ? 1 - progress : progress) / Math.abs(speed);
            timeStr = leftTicks < 600 ? "(" + Strings.autoFixed(leftTicks / 60, 1) + "s" + ")"
            : "(" + UI.formatTime(leftTicks) + ")";
        }
        FuncX.drawText(pos, Strings.fixed(progress * 100, 2) + "%" + timeStr, scl, Pal.accent, Align.bottom);

        // 显示物品需求
        StringBuilder requirements = new StringBuilder();
        for(int i = 0; i < current.requirements.length; i++){
            ItemStack stack = current.requirements[i];
            float consumeAmount = state.rules.buildCostMultiplier * stack.amount;
            int coreAmount = constructBuild.team.core().items.get(stack.item);

            int investItem = (int)(progress * consumeAmount);
            int needItem = (int)(consumeAmount) - investItem;
            boolean hasItem = coreAmount >= needItem;

            if(i != 0) requirements.append('\n');
            requirements.append(stack.item.emoji()).append(hasItem ? "[#ffd37f]" : "[#e55454]").append(investItem).append("/").append(needItem).append("/").append(UI.formatAmount(coreAmount)).append("[]");
        }
        pos.set(constructBuild).add(-buildHitSize / 2f, -buildHitSize / 2f);//左下角
        FuncX.drawText(pos, requirements.toString(), scl, Color.white, Align.topLeft);
    }
}