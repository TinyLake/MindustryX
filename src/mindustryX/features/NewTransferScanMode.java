package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;
import mindustryX.features.func.*;

import static mindustry.Vars.*;

/**
 * 新的物流扫描模式 - 支持物品和液体传输的可视化
 * New transport scanning mode - supports both item and liquid transport visualization
 */
public class NewTransferScanMode {
    static final int maxDepth = 100;
    private static final Color itemInputColor = Color.valueOf("ff8000");
    private static final Color itemOutputColor = Color.valueOf("80ff00");
    private static final Color liquidInputColor = Color.valueOf("4080ff");
    private static final Color liquidOutputColor = Color.valueOf("00ffff");
    
    public static void draw() {
        Draw.z(Layer.overlayUI + 0.01f);
        
        // 显示鼠标位置信息
        Vec2 pos = Core.input.mouseWorld();
        String text = Strings.format("@,@\n距离: @",
            (int)(pos.x / tilesize), (int)(pos.y / tilesize), (int)(player.dst(pos) / tilesize));
        FuncX.drawText(pos, text);
        
        Draw.z(Layer.overlayUI);
        
        // 获取鼠标悬停的建筑
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile == null || hoverTile.build == null || !hoverTile.build.isDiscovered(player.team())) {
            return;
        }
        
        Building target = hoverTile.build;
        
        // 绘制物品传输网络
        if(target.block.hasItems) {
            drawTransportNetwork(target, TransportType.ITEM);
        }
        
        // 绘制液体传输网络  
        if(target.block.hasLiquids) {
            drawTransportNetwork(target, TransportType.LIQUID);
        }
    }
    
    private static void drawTransportNetwork(Building target, TransportType type) {
        Seq<Connection> inputs = new Seq<>();
        Seq<Connection> outputs = new Seq<>();
        
        // 获取输入连接
        findConnections(target, type, true, inputs, new ObjectSet<>(), 0);
        
        // 获取输出连接
        findConnections(target, type, false, outputs, new ObjectSet<>(), 0);
        
        // 绘制连接
        Color inputColor = type == TransportType.ITEM ? itemInputColor : liquidInputColor;
        Color outputColor = type == TransportType.ITEM ? itemOutputColor : liquidOutputColor;
        
        drawConnections(inputs, inputColor, false);
        drawConnections(outputs, outputColor, true);
    }
    
    private static void findConnections(Building building, TransportType type, boolean findInputs, 
                                       Seq<Connection> connections, ObjectSet<Building> visited, int depth) {
        if(depth > maxDepth || visited.contains(building)) return;
        visited.add(building);
        
        Seq<Building> nextBuildings = findInputs ? 
            possibleInputFrom(building, type) : 
            getOutputTo(building, type);
            
        for(Building next : nextBuildings) {
            if(next != null && !visited.contains(next)) {
                connections.add(new Connection(building, next, isEndpoint(next, type)));
                if(!isEndpoint(next, type)) {
                    findConnections(next, type, findInputs, connections, visited, depth + 1);
                }
            }
        }
    }
    
    /**
     * 获取建筑可能的输出目标
     * Get possible output targets for a building
     */
    public static Seq<Building> getOutputTo(Building building, TransportType type) {
        Seq<Building> outputs = new Seq<>();
        
        if(building == null) return outputs;
        
        // 处理特殊建筑的输出
        if(handleSpecialOutputs(building, type, outputs)) {
            return outputs;
        }
        
        // 检查相邻建筑
        for(Building nearby : building.proximity) {
            if(canTransferTo(building, nearby, type)) {
                outputs.add(nearby);
            }
        }
        
        return outputs;
    }
    
    /**
     * 获取建筑可能的输入来源
     * Get possible input sources for a building
     */
    public static Seq<Building> possibleInputFrom(Building building, TransportType type) {
        Seq<Building> inputs = new Seq<>();
        
        if(building == null) return inputs;
        
        // 处理特殊建筑的输入
        if(handleSpecialInputs(building, type, inputs)) {
            return inputs;
        }
        
        // 检查相邻建筑
        for(Building nearby : building.proximity) {
            if(canTransferTo(nearby, building, type)) {
                inputs.add(nearby);
            }
        }
        
        return inputs;
    }
    
    private static boolean handleSpecialOutputs(Building building, TransportType type, Seq<Building> outputs) {
        // 质量驱动器
        if(building instanceof MassDriver.MassDriverBuild massDriver && type == TransportType.ITEM) {
            if(massDriver.linkValid()) {
                outputs.add(world.build(massDriver.link));
                return true;
            }
        }
        
        // 物品桥
        if(building instanceof ItemBridge.ItemBridgeBuild bridge && type == TransportType.ITEM) {
            if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                outputs.add(world.build(bridge.link));
                return true;
            }
        }
        
        // 液体桥
        if(building instanceof LiquidBridge.LiquidBridgeBuild bridge && type == TransportType.LIQUID) {
            if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                outputs.add(world.build(bridge.link));
                return true;
            }
        }
        
        // 导管桥
        if(building instanceof DirectionBridge.DirectionBridgeBuild dirBridge && type == TransportType.ITEM) {
            DirectionBridge.DirectionBridgeBuild link = dirBridge.findLink();
            if(link != null) {
                outputs.add(link);
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean handleSpecialInputs(Building building, TransportType type, Seq<Building> inputs) {
        // 物品桥接收
        if(building instanceof ItemBridge.ItemBridgeBuild bridge && type == TransportType.ITEM) {
            bridge.incoming.each(pos -> {
                Building source = world.tile(pos).build;
                if(source != null) inputs.add(source);
            });
        }
        
        // 液体桥接收
        if(building instanceof LiquidBridge.LiquidBridgeBuild bridge && type == TransportType.LIQUID) {
            bridge.incoming.each(pos -> {
                Building source = world.tile(pos).build;
                if(source != null) inputs.add(source);
            });
        }
        
        // 导管桥接收
        if(building instanceof DirectionBridge.DirectionBridgeBuild dirBridge && type == TransportType.ITEM) {
            for(Building occupied : dirBridge.occupied) {
                if(occupied != null) inputs.add(occupied);
            }
        }
        
        return false;
    }
    
    private static boolean canTransferTo(Building from, Building to, TransportType type) {
        if(from == null || to == null) return false;
        
        return canOutput(from, to, type) && canInput(to, from, type);
    }
    
    private static boolean canOutput(Building from, Building to, TransportType type) {
        if(type == TransportType.ITEM) {
            return canOutputItems(from, to);
        } else {
            return canOutputLiquids(from, to);
        }
    }
    
    private static boolean canInput(Building to, Building from, TransportType type) {
        if(type == TransportType.ITEM) {
            return canInputItems(to, from);
        } else {
            return canInputLiquids(to, from);
        }
    }
    
    private static boolean canOutputItems(Building from, Building to) {
        // 传送带类
        if(from instanceof Conveyor.ConveyorBuild || from instanceof Duct.DuctBuild) {
            return to == from.front();
        }
        
        // 塑钢传送带
        if(from instanceof StackConveyor.StackConveyorBuild stackConveyor) {
            if(stackConveyor.state == 2 && ((StackConveyor)stackConveyor.block).outputRouter) {
                return to != from.back();
            }
            return to == from.front();
        }
        
        // 路由器和交叉器
        if(from instanceof Router.RouterBuild || from instanceof Junction.JunctionBuild || 
           from instanceof DuctRouter.DuctRouterBuild || from instanceof DuctJunction.DuctJunctionBuild) {
            return true;
        }
        
        // 分拣器
        if(from instanceof Sorter.SorterBuild) {
            int relativeDir = from.relativeTo(to);
            return relativeDir == from.rotation || relativeDir != (from.rotation + 2) % 4;
        }
        
        // 溢流门
        if(from instanceof OverflowGate.OverflowGateBuild || from instanceof OverflowDuct.OverflowDuctBuild) {
            return from.relativeTo(to) != (from.rotation + 2) % 4;
        }
        
        // 定向卸载器
        if(from instanceof DirectionalUnloader.DirectionalUnloaderBuild) {
            return to == from.front();
        }
        
        // 生产建筑和存储建筑
        if(isProducerOrStorage(from.block)) {
            return from.canDump(to, null);
        }
        
        return false;
    }
    
    private static boolean canInputItems(Building to, Building from) {
        // 装甲传送带
        if(to instanceof ArmoredConveyor.ArmoredConveyorBuild) {
            return from != to.front() && (from instanceof Conveyor.ConveyorBuild || from == to.back());
        }
        
        // 装甲导管
        if(to instanceof Duct.DuctBuild ductBuild && ((Duct)ductBuild.block).armored) {
            return from != to.front() && (from.block.isDuct || from == to.back());
        }
        
        // 普通传送带和导管
        if(to instanceof Conveyor.ConveyorBuild || to instanceof Duct.DuctBuild) {
            return from != to.front();
        }
        
        // 塑钢传送带
        if(to instanceof StackConveyor.StackConveyorBuild stackConveyor) {
            return switch(stackConveyor.state) {
                case 2 -> from == to.back() && from instanceof StackConveyor.StackConveyorBuild;
                case 1 -> from != to.front();
                default -> from instanceof StackConveyor.StackConveyorBuild;
            };
        }
        
        // 交叉器和路由器
        if(to instanceof Junction.JunctionBuild || to instanceof DuctJunction.DuctJunctionBuild ||
           to instanceof Router.RouterBuild || to instanceof DuctRouter.DuctRouterBuild) {
            return true;
        }
        
        // 分拣器
        if(to instanceof Sorter.SorterBuild) {
            return from.relativeTo(to) != to.rotation;
        }
        
        // 溢流门
        if(to instanceof OverflowGate.OverflowGateBuild || to instanceof OverflowDuct.OverflowDuctBuild) {
            return from.relativeTo(to) != to.rotation;
        }
        
        // 物品桥
        if(to instanceof ItemBridge.ItemBridgeBuild bridge) {
            return from == bridge.back() || bridge.linked(from);
        }
        
        // 定向卸载器不接受物品输入
        if(to instanceof DirectionalUnloader.DirectionalUnloaderBuild) {
            return false;
        }
        
        // 消费建筑
        if(isConsumerOrStorage(to.block)) {
            return true;
        }
        
        return false;
    }
    
    private static boolean canOutputLiquids(Building from, Building to) {
        // 导管
        if(from instanceof Conduit.ConduitBuild) {
            return to == from.front();
        }
        
        // 液体路由器
        if(from instanceof LiquidRouter.LiquidRouterBuild) {
            return true;
        }
        
        // 液体桥不向相邻建筑输出（只通过链接）
        if(from instanceof LiquidBridge.LiquidBridgeBuild bridge) {
            if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                return false; // 有链接时不向相邻输出
            }
            return from.canDump(to, null);
        }
        
        // 生产建筑
        if(isProducerOrStorage(from.block)) {
            return from.canDump(to, null);
        }
        
        return false;
    }
    
    private static boolean canInputLiquids(Building to, Building from) {
        // 导管
        if(to instanceof Conduit.ConduitBuild) {
            return from != to.front();
        }
        
        // 液体路由器
        if(to instanceof LiquidRouter.LiquidRouterBuild) {
            return true;
        }
        
        // 液体桥
        if(to instanceof LiquidBridge.LiquidBridgeBuild bridge) {
            return from == bridge.back() || bridge.linked(from);
        }
        
        // 消费建筑
        if(isConsumerOrStorage(to.block) && to.block.hasLiquids) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isEndpoint(Building building, TransportType type) {
        Block block = building.block;
        
        // 运输建筑不是端点
        if(block.group == BlockGroup.transportation) {
            return false;
        }
        
        // 生产、加工、存储建筑是端点
        return block.group == BlockGroup.production || 
               block.group == BlockGroup.crafting || 
               block.group == BlockGroup.storage ||
               (type == TransportType.ITEM ? block.hasItems : block.hasLiquids);
    }
    
    private static boolean isProducerOrStorage(Block block) {
        return block.group == BlockGroup.production || 
               block.group == BlockGroup.crafting || 
               block.group == BlockGroup.storage;
    }
    
    private static boolean isConsumerOrStorage(Block block) {
        return block.group == BlockGroup.production || 
               block.group == BlockGroup.crafting || 
               block.group == BlockGroup.storage ||
               block.group == BlockGroup.turret ||
               block.group == BlockGroup.power;
    }
    
    private static void drawConnections(Seq<Connection> connections, Color color, boolean isOutput) {
        // 绘制连接线
        for(Connection conn : connections) {
            if(!conn.isEndpoint) {
                drawConnection(conn.from, conn.to, color, isOutput);
            } else {
                // 端点用不同的颜色高亮
                Color endpointColor = color.cpy().mul(1.2f);
                Drawf.selected(conn.to, Tmp.c1.set(endpointColor).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));
            }
        }
        
        // 绘制方向指示器
        for(Connection conn : connections) {
            if(!conn.isEndpoint) {
                drawDirectionIndicator(conn.from, conn.to, color, isOutput);
            }
        }
    }
    
    private static void drawConnection(Building from, Building to, Color color, boolean isOutput) {
        float x1 = from.tile.drawx(), y1 = from.tile.drawy();
        float x2 = to.tile.drawx(), y2 = to.tile.drawy();
        
        Draw.color(Tmp.c1.set(color).a(Mathf.absin(4f, 1f) * 0.4f + 0.6f));
        Lines.stroke(1.5f);
        Lines.line(x1, y1, x2, y2);
        Draw.reset();
    }
    
    private static void drawDirectionIndicator(Building from, Building to, Color color, boolean isOutput) {
        float x1 = from.tile.drawx(), y1 = from.tile.drawy();
        float x2 = to.tile.drawx(), y2 = to.tile.drawy();
        float dst = Mathf.dst(x1, y1, x2, y2);
        
        if(dst > tilesize) {
            Draw.color(color);
            
            // 起点圆点
            Fill.circle(x1, y1, 1.8f);
            
            // 方向箭头
            Vec2 fromPos = Tmp.v1.set(x1, y1);
            Vec2 toPos = Tmp.v2.set(x2, y2);
            
            if(!isOutput) {
                // 输入方向相反
                fromPos.set(x2, y2);
                toPos.set(x1, y1);
            }
            
            Vec2 midPoint = Tmp.v3.set(fromPos).lerp(toPos, 0.5f);
            float angle = fromPos.angleTo(toPos);
            
            Fill.poly(midPoint.x, midPoint.y, 3, 3f, angle);
            Draw.reset();
        }
    }
    
    public enum TransportType {
        ITEM, LIQUID
    }
    
    private static class Connection {
        public final Building from;
        public final Building to;
        public final boolean isEndpoint;
        
        public Connection(Building from, Building to, boolean isEndpoint) {
            this.from = from;
            this.to = to;
            this.isEndpoint = isEndpoint;
        }
    }
}