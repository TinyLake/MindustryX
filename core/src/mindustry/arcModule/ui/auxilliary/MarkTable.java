package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import mindustry.arcModule.*;
import mindustry.arcModule.Marker.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.fragments.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ui.RStyles.*;

public class MarkTable extends BaseToolsTable{
    public MarkType markType = Marker.mark;
    public Element mobileHitter = new Element();

    public MarkTable(){
        super(Icon.effect);

        mobileHitter.addListener(new ElementGestureListener(20, 0.4f, Marker.heatTime / 60f, 0.15f){
            @Override
            public boolean longPress(Element actor, float x, float y){
                Marker.mark(markType, Core.input.mouseWorld());

                mobileHitter.remove();

                return true;
            }

            @Override
            public void fling(InputEvent event, float velocityX, float velocityY, KeyCode button){
                mobileHitter.remove();
                ui.announce("[yellow]你已退出标记模式");
            }
        });

        mobileHitter.fillParent = true;

        if(!mobile){
            update(() -> {
                if(Core.input.keyTap(Binding.point) && !Core.scene.hasField()){
                    Marker.mark(markType, Core.input.mouseWorld());
                }
            });
        }
    }

    @Override
    protected void setup(){
        if(mobile){
            button("♐ >", clearLineNonet, () -> {
                ui.hudGroup.addChild(mobileHitter);
                ui.announce("[cyan]你已进入标记模式,长按屏幕可进行一次标记(外划可以退出).");
            }).height(40).width(70f).tooltip("开启手机标记");
        }

        for(MarkType type : Marker.markTypes){
            button(type.tinyName(), clearLineNoneTogglet, () -> markType = type)
            .checked(b -> markType == type).size(40).tooltip(type.describe);
        }

        button("T", clearLineNoneTogglet, () -> ui.chatfrag.nextMode())
        .checked(b -> ui.chatfrag.mode == ChatFragment.ChatMode.team).size(40).tooltip("前缀添加/t");
    }
}
