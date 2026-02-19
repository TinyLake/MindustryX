package mindustryX.features;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import kotlin.collections.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.CanvasBlock.*;

import static mindustryX.features.func.FuncX.*;
import static mindustry.Vars.*;

//move from mindustry.arcModule.toolpack.picToMindustry
public class PicToMindustry{
    static final int[] palette;
    static final int canvasSize;
    static final float[] scaleList = {0.02f, 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.4f, 0.5f, 0.65f, 0.8f, 1f, 1.25f, 1.5f, 2f, 3f, 5f};
    static final String[] disFunList = {uiBasic(), uiSquared(), "LAB"}; // 原文本:基础对比 | 平方对比

    static{
        CanvasBlock canva = (CanvasBlock)Blocks.canvas;
        palette = canva.palette;
        canvasSize = canva.canvasSize;
    }

    static Table tTable = new Table();
    static float scale = 1f;
    static int colorDisFun = 0;
    static Pixmap oriImage;
    static Fi originFile;


    public static void show(){
        Dialog pt = new BaseDialog(uiArcImageConverter()); // 原文本:arc-图片转换器
        pt.cont.table(t -> {
            t.add(uiSelectAndImportPictures()).padBottom(20f).row(); // 原文本:选择并导入图片，可将其转成画板、像素画或是逻辑画
            t.button(uiSelectImagePng(), () -> Vars.platform.showFileChooser(true, "png", file -> { // 原文本:选择图片[white](png)
                if(oriImage != null){
                    oriImage.dispose();
                    oriImage = null;
                }
                try{
                    originFile = file;
                    byte[] bytes = file.readBytes();
                    oriImage = new Pixmap(bytes);
                    if(oriImage.width > 500 || oriImage.height > 500)
                        UIExt.announce(uiWarnImageTooLarge(), (float)5); // 原文本:[orange]警告：图片可能过大，请尝试压缩图片
                }catch(Throwable e){
                    UIExt.announce(uiFailedReadImage(e)); // 原文本:读取图片失败，请尝试更换图片\n{0}
                }
                rebuilt();
            })).size(240, 50).padBottom(20f).row();
            t.check(uiAutomaticallySaveAsBlueprint(), Core.settings.getBool("autoSavePTM"), ta -> Core.settings.put("autoSavePTM", ta)); // 原文本:自动保存为蓝图
        }).padBottom(20f).row();
        pt.cont.table(t -> {
            t.add(uiZoomZoom()); // 原文本:缩放: 
            Label zoom = t.add(String.valueOf(scale)).padRight(20f).get();
            t.slider(0, scaleList.length - 1, 1, 11, s -> {
                scale = scaleList[(int)s];
                zoom.setText(Strings.fixed(scale, 2));
                rebuilt();
            }).width(200f);
        }).padBottom(20f).visible(() -> oriImage != null).row();
        pt.cont.table(t -> {
            t.add(uiHueMode()); // 原文本:色调函数:
            Label zoom = t.add(disFunList[0]).padRight(20f).get();
            t.slider(0, disFunList.length - 1, 1, 0, s -> {
                colorDisFun = (int)s;
                zoom.setText(disFunList[colorDisFun]);
            }).width(200f);
        }).padBottom(20f).visible(() -> oriImage != null).row();
        pt.cont.add(tTable);
        pt.cont.row();
        pt.cont.button(uiLabelWithEmoji(uiLogicArtWebsite(), Blocks.logicDisplay.emoji()), () -> UIExt.openURI("https://buibiu.github.io/imageToMLogicPage/#/")).width(200f); // 原文本:逻辑画网站 {0}
        pt.addCloseButton();
        pt.show();
    }


    private static String formatNumber(int number){
        if(number >= 500) return "[red]" + number + "[]";
        else if(number >= 200) return "[orange]" + number + "[]";
        else return String.valueOf(number);
    }

    private static void rebuilt(){
        tTable.clear();
        if(oriImage == null) return;
        int scaledW = (int)(oriImage.getWidth() * scale), scaledH = (int)(oriImage.getHeight() * scale);
        tTable.table(t -> {
            t.add(uiPath()).color(Pal.accent).padRight(25f).padBottom(10f); // 原文本:路径
            t.button("\uE874", () -> Core.app.setClipboardText(originFile.absolutePath()));
            t.add(originFile.absolutePath()).padBottom(10f).row();

            t.add(uiName()).color(Pal.accent).padRight(25f).padBottom(10f); // 原文本:名称
            t.button("\uE874", () -> Core.app.setClipboardText(originFile.name()));
            t.add(originFile.name()).padBottom(10f).row();

            t.add(uiOriginalSize()).color(Pal.accent).padRight(25f); // 原文本:原始大小
            t.add(formatNumber(oriImage.width) + "\uE815" + formatNumber(oriImage.height)).row();

            t.add(uiScaledSize()).color(Pal.accent).padRight(25f); // 原文本:缩放后大小
            t.add(formatNumber(scaledW) + "\uE815" + formatNumber(scaledH));
        }).padBottom(20f).row();
        tTable.table(t -> {
            t.table(tt -> {
                int w = Mathf.ceil(scaledW * 1f / canvasSize), h = Mathf.ceil(scaledH * 1f / canvasSize);
                tt.button(uiLabelWithEmoji(uiCanvas(), Blocks.canvas.emoji()), Styles.cleart, () -> { // 原文本:画板 {0}
                    Pixmap image = Pixmaps.scale(oriImage, w * canvasSize, h * canvasSize, false);
                    image.replace((pixel) -> ArraysKt.minByOrThrow(palette, (it) -> diff_rbg(it, pixel)));
                    Schematic schem = canvasGenerator(image, w, h);
                    image.dispose();
                    saveSchem(schem, Blocks.canvas.emoji());
                }).size(100, 50);
                tt.add(uiSize() + w + "\uE815" + h); // 原文本:大小：
            });
            t.row();
            t.table(tt -> {
                int w = Mathf.ceil(scaledW * 1f / canvasSize), h = Mathf.ceil(scaledH * 1f / canvasSize);
                tt.button(uiLabelWithEmoji(uiArtboard(), Blocks.canvas.emoji()), Styles.cleart, () -> { // 原文本:画板++ {0}
                    Pixmap image = Pixmaps.scale(oriImage, w * canvasSize, h * canvasSize, false);
                    mapPalettePlus(image);
                    Schematic schem = canvasGenerator(image, w, h);
                    image.dispose();
                    saveSchem(schem, Blocks.canvas.emoji());
                }).size(100, 50);
                tt.add(uiSize() + w + "\uE815" + h); // 原文本:大小：
            }).row();
            t.table(tt -> {
                tt.button(uiLabelWithEmoji(uiPixelArt(), Blocks.sorter.emoji()), Styles.cleart, () -> { // 原文本:像素画 {0}
                    Pixmap image = Pixmaps.scale(oriImage, scale);
                    Schematic schem = sorterGenerator(image);
                    image.dispose();
                    saveSchem(schem, Blocks.sorter.emoji());
                }).size(100, 50);
                tt.add(uiSize() + formatNumber(scaledW) + "\uE815" + formatNumber(scaledH)); // 原文本:大小：
            }).row();
        });
    }

    private static float diff_rbg(int a, int b){
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

    private static Schematic canvasGenerator(Pixmap image, int w, int h){
        Seq<Schematic.Stile> tiles = new Seq<>();
        CanvasBuild build = (CanvasBuild)Blocks.canvas.newBuilding();
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                // get max 12x12 region of the image
                Pixmap region = image.crop(x * canvasSize, (h - y - 1) * canvasSize, canvasSize, canvasSize);
                // convert pixel data of the region
                byte[] bytes = build.packPixmap(region);
                Schematic.Stile stile = new Schematic.Stile(Blocks.canvas, x * 2, y * 2, bytes, (byte)0);
                tiles.add(stile);
            }
        }
        StringMap tags = new StringMap();
        tags.put("name", originFile.name());
        return new Schematic(tiles, tags, w * 2, h * 2);
    }

    private static void saveSchem(Schematic schem, String l){
        schem.labels.add(l);
        if(Core.settings.getBool("autoSavePTM")){
            Vars.schematics.add(schem);
            String text = uiSavedBlueprint(originFile.name()); // 原文本:已保存蓝图：{0}
            UIExt.announce(text, (float)10);
        }
        if(state.isGame()){
            Vars.ui.schematics.hide();
            Vars.control.input.useSchematic(schem);
        }
    }

    private static Schematic sorterGenerator(Pixmap image){
        Seq<Schematic.Stile> tiles = new Seq<>();
        for(int y = 0; y < image.height; y++){
            for(int x = 0; x < image.width; x++){
                var pixel = image.get(x, y);
                if(pixel == 0) continue;
                var closest = CollectionsKt.minByOrThrow(content.items(), (it) -> diff_rbg(it.color.rgba(), pixel));
                Schematic.Stile stile = new Schematic.Stile(Blocks.sorter, x, image.height - y - 1, closest, (byte)0);
                tiles.add(stile);
            }
        }
        StringMap tags = new StringMap();
        tags.put("name", originFile.name());
        return new Schematic(tiles, tags, image.width, image.height);
    }

    private static int trans(RGB c1, RGB c2, int mul){
        return c1.add(c2.cpy().mul(mul).mv(4)).rgba();
    }

    private static void mapPalettePlus(Pixmap image){
        for(int y = 0; y < image.height; y++){
            for(int x = 0; x < image.width; x++){
                RGB pix = new RGB(image.get(x, y));
                int nearest = ArraysKt.minByOrThrow(palette, (it) -> new RGB(it).sub(pix).pow());
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
