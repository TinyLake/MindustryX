package mindustry.ui;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.*;

import static mindustry.Vars.*;

public class Minimap extends Table{
    private final Cell<?> mapCell;

    public Minimap(){
        background(Tex.pane);
        float margin = 5f;
        this.touchable = Touchable.enabled;

        mapCell = add(new Element(){
            @Override
            public void act(float delta){
                var size = Core.settings.getInt("minimapSize");
                mapCell.size(size);
                setSize(Scl.scl(size));
                setPosition(Scl.scl(margin), Scl.scl(margin));

                super.act(delta);
            }

            @Override
            protected void sizeChanged(){
                super.sizeChanged();
                mapCell.getTable().invalidate();
            }

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;
                if(!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    Draw.alpha(parentAlpha);
                    renderer.minimap.drawEntities(x, y, width, height, 3f / renderer.minimap.getZoom(), false);
                }

                clipEnd();
            }
        }).size(140f);

        margin(margin);

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                renderer.minimap.zoomBy(amounty);
                return true;
            }
        });

        addListener(new ClickListener(){
            {
                tapSquareSize = Scl.scl(11f);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(inTapSquare()){
                    super.touchUp(event, x, y, pointer, button);
                }else{
                    pressed = false;
                    pressedPointer = -1;
                    pressedButton = null;
                    cancelled = false;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(!inTapSquare(x, y)){
                    invalidateTapSquare();
                }
                super.touchDragged(event, x, y, pointer);

                if(mobile){
                    float max = Math.min(world.width(), world.height()) / 16f / 2f;
                    renderer.minimap.setZoom(1f + y / height * (max - 1f));
                }
            }

            @Override
            public void clicked(InputEvent event, float x, float y){
                float sz = 16 * renderer.minimap.getZoom();
                float dx = Mathf.clamp(Core.camera.position.x / 8, sz, world.width() - sz);
                float dy = Mathf.clamp(Core.camera.position.y / 8, sz, world.height() - sz);

                float ptilex = sz * 2 / width, ptiley = sz * 2 / height;
                Vec2 pos = Tmp.v1.set(dx, dy).sub(sz, sz).add(x * ptilex, y * ptiley).scl(8);

                if(control.input instanceof DesktopInput input){
                    input.panning = true;
                }

                Core.camera.position.set(pos);
            }
        });

        update(() -> {

            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(this)){
                requestScroll();
            }else if(hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }
}
