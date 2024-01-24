package mindustry.arcModule.toolpack;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.CanvasBlock.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.content.Blocks.*;

public class picToMindustry{

    static Pixmap oriImage, image, Cimage;
    static Integer closest = null;
    static Table tTable;
    static Fi originFile;

    static int[] palette;
    static int canvasSize;

    static float scale = 1f;
    static float[] scaleList = {0.02f, 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.4f, 0.5f, 0.65f, 0.8f, 1f, 1.25f, 1.5f, 2f, 3f, 5f};
    static int colorDisFun = 0;
    static String[] disFunList = {"基础对比", "平方对比", "LAB"};

    static{
        CanvasBlock canva = (CanvasBlock)Blocks.canvas;
        palette = canva.palette;
        canvasSize = canva.canvasSize;
    }

    public static void show(){
        ptDialog().show();
    }

    public static Dialog ptDialog(){
        Dialog pt = new BaseDialog("arc-图片转换器");
        pt.cont.table(t -> {
            t.add("选择并导入图片，可将其转成画板、像素画或是逻辑画").padBottom(20f).row();
            t.button("[cyan]选择图片[white](png)", () -> Vars.platform.showFileChooser(false, "png", file -> {
                try{
                    originFile = file;
                    byte[] bytes = file.readBytes();
                    oriImage = new Pixmap(bytes);
                    rebuilt();
                    if(oriImage.width > 500 || oriImage.height > 500)
                        arcui.arcInfo("[orange]警告：图片可能过大，请尝试压缩图片", 5);
                }catch(Throwable e){
                    arcui.arcInfo("读取图片失败，请尝试更换图片\n" + e);
                }
            })).size(240, 50).padBottom(20f).row();
            t.check("自动保存为蓝图", Core.settings.getBool("autoSavePTM"), ta -> Core.settings.put("autoSavePTM", ta));
        }).padBottom(20f).row();
        pt.cont.table(t -> {
            t.add("缩放: \uE815 ");
            Label zoom = t.add(String.valueOf(scale)).padRight(20f).get();
            t.slider(0, scaleList.length - 1, 1, 11, s -> {
                scale = scaleList[(int)s];
                zoom.setText(Strings.fixed(scale, 2));
                rebuilt();
            }).width(200f);
        }).padBottom(20f).visible(() -> oriImage != null).row();
        pt.cont.table(t -> {
            t.add("色调函数: ");
            Label zoom = t.add(disFunList[0]).padRight(20f).get();
            t.slider(0, disFunList.length - 1, 1, 0, s -> {
                colorDisFun = (int)s;
                zoom.setText(disFunList[colorDisFun]);
            }).width(200f);
        }).padBottom(20f).visible(() -> oriImage != null).row();
        pt.cont.table(a -> tTable = a);
        pt.cont.row();
        pt.cont.button("逻辑画网站 " + Blocks.logicDisplay.emoji(), () -> {
            String imageUrl = "https://buibiu.github.io/imageToMLogicPage/#/";
            if(!Core.app.openURI(imageUrl)){
                ui.showErrorMessage("打开失败，网址已复制到粘贴板\n请自行在阅览器打开");
                Core.app.setClipboardText(imageUrl);
            }
        }).width(200f);
        pt.addCloseButton();
        return pt;
    }

    private static String formatNumber(int number){
        return formatNumber(number, 1f);
    }

    private static String formatNumber(int number, float alert){
        if(number >= 500 * alert) return "[red]" + number + "[]";
        else if(number >= 200 * alert) return "[orange]" + number + "[]";
        else return String.valueOf(number);
    }

    private static void rebuilt(){
        image = Pixmaps.scale(oriImage, scale);
        tTable.clear();
        tTable.table(t -> {
            t.add("路径").color(ARCVars.getThemeColor()).padRight(25f).padBottom(10f);
            t.button("\uE874", () -> Core.app.setClipboardText(originFile.absolutePath()));
            t.add(originFile.absolutePath()).padBottom(10f).row();

            t.add("名称").color(ARCVars.getThemeColor()).padRight(25f).padBottom(10f);
            t.button("\uE874", () -> Core.app.setClipboardText(originFile.name()));
            t.add(originFile.name()).padBottom(10f).row();

            t.add("原始大小").color(ARCVars.getThemeColor()).padRight(25f);
            t.add(formatNumber(oriImage.width) + "\uE815" + formatNumber(oriImage.height));
        }).padBottom(20f).row();
        tTable.table(t -> {
            t.table(tt -> {
                tt.button("画板 " + canvas.emoji(), Styles.cleart, () -> {
                    Cimage = image.copy();
                    create_rbg(palette);
                    canvasGenerator();
                }).size(100, 50);
                tt.add("大小：" + formatNumber(image.width / canvasSize, 0.5f) + "\uE815" + formatNumber(image.height / canvasSize + 1, 0.5f));
            });
            t.row();
            t.table(tt -> {
                tt.button("画板++ " + canvas.emoji(), Styles.cleart, () -> {
                    Cimage = image.copy();
                    canvasPlus(Cimage);
                    canvasGenerator();
                }).size(100, 50);
                tt.add("大小：" + formatNumber(image.width / canvasSize, 0.5f) + "\uE815" + formatNumber(image.height / canvasSize + 1, 0.5f));
            }).row();
            t.table(tt -> {
                tt.button("像素画 " + Blocks.sorter.emoji(), Styles.cleart, () -> {
                    Cimage = image.copy();
                    sorterGenerator();
                }).size(100, 50);
                tt.add("大小：" + formatNumber(image.width) + "\uE815" + formatNumber(image.height));
            }).row();
        });
    }

    private static float diff_rbg(Integer a, Integer b){
        int ar = a >> 24 & 0xFF,
        ag = a >> 16 & 0xFF,
        ab = a >> 8 & 0xFF;
        // get in
        int br = b >> 24 & 0xFF,
        bg = b >> 16 & 0xFF,
        bb = b >> 8 & 0xFF;
        int dr = Math.abs(ar - br),
        dg = Math.abs(ag - bg),
        db = Math.abs(ab - bb);
        switch(colorDisFun){
            case 1 -> {
                return dr * dr + dg * dg + db * db;
            }
            case 2 -> {
                float Rmean = (ar + br) / 2f;
                return (float)Math.sqrt((2 + Rmean / 256) * (dr * dr) + 4 * (dg * dg) + (2 + (255 - Rmean) / 256) * (db * db));
            }
            default -> {
                return dr + dg + db;
            }
        }
    }

    private static void create_rbg(int[] colorBar){
        for(int x = 0; x < image.width; x++){
            for(int y = 0; y < image.height; y++){
                Integer pixel = image.get(x, y);
                float egg = 1000;
                for(int other : colorBar){
                    float h = diff_rbg(pixel, other);
                    if(h < egg){
                        closest = other;
                        egg = h;
                    }
                }
                Cimage.set(x, y, closest);
            }
        }
    }

    private static void canvasGenerator(){
        int width = Cimage.width / canvasSize, height = Cimage.height / canvasSize + 1;
        Seq<Schematic.Stile> tiles = new Seq<>();
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                // add canvas to the schematic
                CanvasBuild build = (CanvasBuild)canvas.newBuilding();
                // get max 12x12 region of the image
                Pixmap region = Cimage.crop(x * canvasSize, (height - y - 1) * canvasSize, canvasSize, canvasSize);
                // convert pixel data of the region
                byte[] bytes = build.packPixmap(region);
                Schematic.Stile stile = new Schematic.Stile(canvas, x * 2, y * 2, bytes, (byte)0);
                tiles.add(stile);
            }
        }
        StringMap tags = new StringMap();
        tags.put("name", originFile.name());
        Schematic schem = new Schematic(tiles, tags, width * 2, height * 2);
        saveSchem(schem, canvas.emoji());
    }

    private static void saveSchem(Schematic schem, String l){
        schem.labels.add(l);
        if(Core.settings.getBool("autoSavePTM")){
            Vars.schematics.add(schem);
            arcui.arcInfo("已保存蓝图：" + originFile.name(), 10);
        }
        if(state.isGame()){
            Vars.ui.schematics.hide();
            Vars.control.input.useSchematic(schem);
        }
    }

    private static void sorterGenerator(){
        Seq<Schematic.Stile> tiles = new Seq<>();
        for(int y = 0; y < image.height; y++){
            for(int x = 0; x < image.width; x++){
                if(image.get(x, y) == 0) continue;
                Sorter.SorterBuild build = (Sorter.SorterBuild)sorter.newBuilding();
                final float[] closestItem = {99999};
                int finalX = x;
                int finalY = y;
                content.items().each(t -> {
                    float dst = diff_rbg(t.color.rgba(), image.get(finalX, finalY));
                    if(dst > closestItem[0]) return;
                    build.sortItem = t;
                    closestItem[0] = dst;
                });
                Schematic.Stile stile = new Schematic.Stile(sorter, x, image.height - y - 1, build.config(), (byte)0);
                tiles.add(stile);
            }
        }
        StringMap tags = new StringMap();
        tags.put("name", originFile.name());
        Schematic schem = new Schematic(tiles, tags, image.width, image.height);
        saveSchem(schem, sorter.emoji());
    }

    private static int trans(RGB c1, RGB c2, int mul){
        return c1.add(c2.cpy().mul(mul).mv(4)).rgba();
    }

    private static void canvasPlus(Pixmap image){
        for(int y = 0; y < image.height; y++){
            for(int x = 0; x < image.width; x++){
                RGB pix = new RGB(image.get(x, y));
                int nearest = findNearestColor(pix);
                image.set(x, y, nearest);
                pix.sub(new RGB(nearest));
                if(x + 1 < image.width){
                    image.set(x + 1, y, trans(new RGB(image.get(x + 1, y)), pix, 7));
                }
                if(y + 1 < image.height){
                    if(x - 1 > 0){
                        image.set(x - 1, y + 1, trans(new RGB(image.get(x - 1, y + 1)), pix, 3));
                    }
                    image.set(x, y + 1, trans(new RGB(image.get(x, y + 1)), pix, 5));
                    if(x + 1 < image.width){
                        image.set(x + 1, y + 1, trans(new RGB(image.get(x + 1, y + 1)), pix, 1));
                    }
                }
            }
        }
    }

    private static int findNearestColor(RGB color){
        int max = 255 * 255 + 255 * 255 + 255 * 255 + 1;
        int output = 0;
        for(int i : palette){
            int delta = color.cpy().sub(new RGB(i)).pow();
            if(delta < max){
                max = delta;
                output = i;
            }
        }
        return output;
    }

    private static class RGB{
        int r, g, b;

        RGB(int r, int g, int b){
            this.r = r;
            this.g = g;
            this.b = b;
        }

        RGB(int rgba){
            this(rgba >> 24 & 0xff, rgba >> 16 & 0xff, rgba >> 8 & 0xff);
        }

        public RGB sub(RGB c){
            r = r - c.r;
            g = g - c.g;
            b = b - c.b;
            return this;
        }

        public RGB add(RGB c){
            r = Math.max(Math.min(c.r + r, 255), 0);
            g = Math.max(Math.min(c.g + g, 255), 0);
            b = Math.max(Math.min(c.b + b, 255), 0);
            return this;
        }

        public RGB mul(int m){
            r *= m;
            g *= m;
            b *= m;
            return this;
        }

        public RGB mv(int s){
            r >>= s;
            g >>= s;
            b >>= s;
            return this;
        }

        public int pow(){
            return r * r + g * g + b * b;
        }

        public int rgba(){
            return r << 24 | g << 16 | b << 8 | 0xff;
        }

        public RGB cpy(){
            return new RGB(r, g, b);
        }
    }
}
