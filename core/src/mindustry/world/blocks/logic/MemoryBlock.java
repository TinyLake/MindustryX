package mindustry.world.blocks.logic;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.ui.Styles;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;

public class MemoryBlock extends Block{
    public int memoryCapacity = 32;

    boolean showInfo = false;
    int numPerRow = 10;
    int period = 15;

    Table infoTable = new Table();

    public MemoryBlock(String name){
        super(name);
        destructible = true;
        solid = true;
        group = BlockGroup.logic;
        drawDisabled = false;
        envEnabled = Env.any;
        canOverdrive = false;
        configurable = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.memoryCapacity, memoryCapacity, StatUnit.none);
    }

    public boolean accessible(){
        return !privileged || state.rules.editor;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }

    public class MemoryBuild extends Building{
        public double[] memory = new double[memoryCapacity];
        float counter = 0f;
        int deci = -1;

        //massive byte size means picking up causes sync issues
        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public boolean collide(Bullet other){
            return !privileged;
        }

        @Override
        public boolean displayable(){
            return accessible();
        }

        @Override
        public void damage(float damage){
            if(privileged) return;
            super.damage(damage);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(memory.length);
            for(double v : memory){
                write.d(v);
            }
        }

        @Override
        public void buildConfiguration(Table table){
            if(!Core.settings.getBool("showOtherTeamState") && !accessible()){
                //go away
                deselect();
                return;
            }

            rebuildInfo();
            table.add(infoTable);
        }

        private void rebuildInfo(){
            infoTable.clear();
            if(!showInfo){
                infoTable.button(Icon.pencil, Styles.cleari, () -> {
                    showInfo = !showInfo;
                    rebuildInfo();
                }).size(40);
                return;
            }
            infoTable.update(()->{
                counter+= Time.delta;
                if(counter>period){
                    counter=0;
                }
            });
            infoTable.setColor(Color.lightGray);
            infoTable.table(t->{
                t.button(Icon.pencil, Styles.cleari, () -> {
                    showInfo = !showInfo;
                    rebuildInfo();
                }).size(40);
                t.button(Icon.refreshSmall,Styles.cleari,()->{
                    rebuildInfo();
                    arcui.arcInfo("已更新内存元！");
                }).size(40);

                Label rowNum = t.add("每行 "+numPerRow).get();
                t.slider(2, 15,1, numPerRow, res -> {
                    numPerRow = (int)res;
                    rowNum.setText("每行 "+numPerRow);
                });

                Label deciL = t.add(deci==-1?"不约化":("约化 "+deci)).get();
                t.slider(-1, 15,1, deci, res -> {
                    deci = (int)res;
                    deciL.setText(deci==-1?"不约化":("约化 "+deci));
                });

                Label refresh = t.add("刷新 "+period).get();
                t.slider(1, 60,1, 20, res -> {
                    period = (int)res;
                    refresh.setText("刷新 "+period);
                });
            });
            infoTable.row();
            infoTable.pane(t->{
                int index = 0;
                for(double v : memory){
                    Label textR = t.add(index + " ").get();
                    int finalIndex = index;

                    t.table(tt->{
                        Label text = tt.add(showString(memory[finalIndex])).get();
                        tt.update(()->{
                            if(counter + Time.delta>period){
                                textR.setText((memory[finalIndex]==0?"[gray]":"") + finalIndex + " ");
                                text.setText(showString(memory[finalIndex]));
                            }
                        });
                        tt.touchable = Touchable.enabled;
                        tt.tapped(()->{
                            Core.app.setClipboardText(memory[finalIndex]+"");
                            arcui.arcInfo("[cyan]复制内存[white]\n " + memory[finalIndex]);
                        });
                    });
                    index+=1;
                    if(index % numPerRow==0) t.row();
                    else t.add(" " + ((index % numPerRow) % 2 == 0?"[cyan]":"[acid]") + "|[white] ");
                }
            }).maxWidth(1000f).maxHeight(500f);
        }

        public String showString(double number){
            if(number == 0) return "[gray]-";
            else if(deci == 0 || number == (int)number) return "[orange]" + (int)number + "";
            if(deci == -1) return "[orange]" + number + "";
            else return "[orange]" + String.format("%." + deci + "f", number) + "";
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            int amount = read.i();
            for(int i = 0; i < amount; i++){
                double val = read.d();
                if(i < memory.length) memory[i] = val;
            }
        }
    }
}
