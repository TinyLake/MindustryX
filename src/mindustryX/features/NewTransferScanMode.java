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
 * 分离收集、逻辑判断和渲染三个部分
 * New transport scanning mode - supports both item and liquid transport visualization
 * Separated into collection, logic, and rendering components
 */
public class NewTransferScanMode {
    static final int maxDepth = 100;
    private static final Color itemInputColor = Color.valueOf("ff8000");
    private static final Color itemOutputColor = Color.valueOf("80ff00");
    private static final Color liquidInputColor = Color.valueOf("4080ff");
    private static final Color liquidOutputColor = Color.valueOf("00ffff");
    
    // 收集器实例 - Collection instance
    private static final TransportCollector collector = new TransportCollector();
    
    // 逻辑判断实例 - Logic instance
    private static final TransportLogic logic = new TransportLogic();
    
    // 渲染器实例 - Renderer instance
    private static final TransportRenderer renderer = new TransportRenderer();
    
    /**
     * 主入口 - 渲染函数
     * Main entry point - rendering function
     */
    public static void draw() {
        renderer.draw();
    }
    
    // ===== 收集器接口 Collection Interface =====
    
    /**
     * 收集器 - 负责收集建筑的连接信息
     * Collector - responsible for collecting building connection information
     */
    private static class TransportCollector {
        
        /**
         * 收集指定建筑的连接信息
         * Collect connections for the specified building
         */
        public Seq<Connection> collect(Building building, TransportType type, boolean findInputs) {
            Seq<Connection> connections = new Seq<>();
            ObjectSet<Building> visited = new ObjectSet<>();
            collectRecursive(building, type, findInputs, connections, visited, 0);
            return connections;
        }
        
        private void collectRecursive(Building building, TransportType type, boolean findInputs, 
                                     Seq<Connection> connections, ObjectSet<Building> visited, int depth) {
            if(depth > maxDepth || visited.contains(building)) return;
            visited.add(building);
            
            Seq<Building> nextBuildings = findInputs ? 
                getDirectInputSources(building, type) : 
                getDirectOutputTargets(building, type);
                
            for(Building next : nextBuildings) {
                if(next != null && !visited.contains(next)) {
                    boolean isEndpoint = logic.isEndpoint(next, type);
                    connections.add(new Connection(building, next, isEndpoint));
                    if(!isEndpoint) {
                        collectRecursive(next, type, findInputs, connections, visited, depth + 1);
                    }
                }
            }
        }
        
        /**
         * 获取直接输出目标（不包含逻辑判断）
         * Get direct output targets (without logic judgment)
         */
        private Seq<Building> getDirectOutputTargets(Building building, TransportType type) {
            Seq<Building> outputs = new Seq<>();
            
            if(building == null) return outputs;
            
            // 处理特殊建筑的直接连接
            if(handleSpecialConnections(building, type, outputs, false)) {
                return outputs;
            }
            
            // 收集相邻建筑
            for(Building nearby : building.proximity) {
                outputs.add(nearby);
            }
            
            return outputs;
        }
        
        /**
         * 获取直接输入来源（不包含逻辑判断）
         * Get direct input sources (without logic judgment)
         */
        private Seq<Building> getDirectInputSources(Building building, TransportType type) {
            Seq<Building> inputs = new Seq<>();
            
            if(building == null) return inputs;
            
            // 处理特殊建筑的直接连接
            if(handleSpecialConnections(building, type, inputs, true)) {
                return inputs;
            }
            
            // 收集相邻建筑
            for(Building nearby : building.proximity) {
                inputs.add(nearby);
            }
            
            return inputs;
        }
        
        /**
         * 处理特殊建筑的连接（桥梁等）
         * Handle special building connections (bridges, etc.)
         */
        private boolean handleSpecialConnections(Building building, TransportType type, Seq<Building> result, boolean isInput) {
            // 质量驱动器
            if(building instanceof MassDriver.MassDriverBuild massDriver && type == TransportType.ITEM) {
                if(isInput) {
                    // 质量驱动器可以从其他质量驱动器接收
                    for(Building other : massDriver.proximity) {
                        if(other instanceof MassDriver.MassDriverBuild otherMd && otherMd.linkValid() && 
                           world.build(otherMd.link) == building) {
                            result.add(other);
                        }
                    }
                } else if(massDriver.linkValid()) {
                    result.add(world.build(massDriver.link));
                }
                return true;
            }
            
            // 物品桥
            if(building instanceof ItemBridge.ItemBridgeBuild bridge && type == TransportType.ITEM) {
                if(isInput) {
                    bridge.incoming.each(pos -> {
                        Building source = world.tile(pos).build;
                        if(source != null) result.add(source);
                    });
                } else if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                    result.add(world.build(bridge.link));
                }
                return true;
            }
            
            // 液体桥
            if(building instanceof LiquidBridge.LiquidBridgeBuild bridge && type == TransportType.LIQUID) {
                if(isInput) {
                    bridge.incoming.each(pos -> {
                        Building source = world.tile(pos).build;
                        if(source != null) result.add(source);
                    });
                } else if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                    result.add(world.build(bridge.link));
                }
                return true;
            }
            
            // 导管桥
            if(building instanceof DirectionBridge.DirectionBridgeBuild dirBridge && type == TransportType.ITEM) {
                if(isInput) {
                    for(Building occupied : dirBridge.occupied) {
                        if(occupied != null) result.add(occupied);
                    }
                } else {
                    DirectionBridge.DirectionBridgeBuild link = dirBridge.findLink();
                    if(link != null) {
                        result.add(link);
                    }
                }
                return true;
            }
            
            return false;
        }
    }
    
    // ===== 逻辑判断接口 Logic Interface =====
    
    /**
     * 逻辑判断器 - 负责传输逻辑的判断
     * Logic Judge - responsible for transport logic decisions
     */
    private static class TransportLogic {
        
        /**
         * 判断建筑是否可以从指定来源接收物品
         * Check if building can receive items from specified source
         */
        public boolean canInput(Building self, Building from, Item item) {
            if(self == null || from == null) return false;
            return canInputItems(self, from);
        }
        
        /**
         * 判断建筑是否可以从指定来源接收液体
         * Check if building can receive liquids from specified source
         */
        public boolean canInput(Building self, Building from, Liquid liquid) {
            if(self == null || from == null) return false;
            return canInputLiquids(self, from);
        }
        
        /**
         * 判断建筑是否可以向指定目标输出物品
         * Check if building can output items to specified target
         */
        public boolean canOutput(Building self, Building to, Item item) {
            if(self == null || to == null) return false;
            return canOutputItems(self, to) && canInputItems(to, self);
        }
        
        /**
         * 判断建筑是否可以向指定目标输出液体
         * Check if building can output liquids to specified target
         */
        public boolean canOutput(Building self, Building to, Liquid liquid) {
            if(self == null || to == null) return false;
            return canOutputLiquids(self, to) && canInputLiquids(to, self);
        }
        
        /**
         * 判断建筑是否为端点
         * Check if building is an endpoint
         */
        public boolean isEndpoint(Building building, TransportType type) {
            if(building == null) return false;
            
            Block block = building.block;
            
            // 传输建筑不是端点
            if(isTransportBlock(block)) {
                return false;
            }
            
            // 具有存储功能或生产功能的建筑是端点
            return hasStorage(block) || hasProduction(block) || hasConsumption(block);
        }
        
        private boolean isTransportBlock(Block block) {
            // 不依赖BlockGroup，直接检查具体类型
            return block instanceof Conveyor || block instanceof Duct || 
                   block instanceof Router || block instanceof Junction ||
                   block instanceof Sorter || block instanceof OverflowGate ||
                   block instanceof ItemBridge || block instanceof MassDriver ||
                   block instanceof DirectionBridge || block instanceof StackConveyor ||
                   block instanceof DuctRouter || block instanceof DuctJunction ||
                   block instanceof OverflowDuct || block instanceof DirectionalUnloader ||
                   block instanceof Conduit || block instanceof LiquidRouter ||
                   block instanceof LiquidBridge;
        }
        
        private boolean hasStorage(Block block) {
            return block instanceof StorageBlock || block.hasItems || block.hasLiquids;
        }
        
        private boolean hasProduction(Block block) {
            return block instanceof GenericCrafter || block instanceof Drill ||
                   block instanceof Pump || block instanceof SolidPump ||
                   block instanceof Fracker || block instanceof UnitFactory;
        }
        
        private boolean hasConsumption(Block block) {
            return block.consumes.any() || block instanceof Turret;
        }
        
        private boolean canOutputItems(Building from, Building to) {
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
            
            // 桥梁有特殊处理
            if(from instanceof ItemBridge.ItemBridgeBuild bridge) {
                // 有链接时只通过链接输出
                if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                    return false;
                }
            }
            
            // 生产建筑和存储建筑
            if(hasProduction(from.block) || hasStorage(from.block)) {
                return from.canDump(to, null);
            }
            
            return false;
        }
        
        private boolean canInputItems(Building to, Building from) {
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
            if(hasConsumption(to.block) || hasStorage(to.block)) {
                return true;
            }
            
            return false;
        }
        
        private boolean canOutputLiquids(Building from, Building to) {
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
            if(hasProduction(from.block) || hasStorage(from.block)) {
                return from.canDump(to, null);
            }
            
            return false;
        }
        
        private boolean canInputLiquids(Building to, Building from) {
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
            if((hasConsumption(to.block) || hasStorage(to.block)) && to.block.hasLiquids) {
                return true;
            }
            
            return false;
        }
    }
    
    // ===== 渲染器接口 Renderer Interface =====
    
    /**
     * 渲染器 - 负责所有绘制功能
     * Renderer - responsible for all drawing functions
     */
    private static class TransportRenderer {
        
        /**
         * 主绘制函数
         * Main drawing function
         */
        public void draw() {
            drawMouseInfo();
            drawTransportNetworks();
        }
        
        /**
         * 绘制鼠标信息
         * Draw mouse information
         */
        private void drawMouseInfo() {
            Draw.z(Layer.overlayUI + 0.01f);
            
            Vec2 pos = Core.input.mouseWorld();
            String text = Strings.format("@,@\n距离: @",
                (int)(pos.x / tilesize), (int)(pos.y / tilesize), (int)(player.dst(pos) / tilesize));
            FuncX.drawText(pos, text);
        }
        
        /**
         * 绘制传输网络
         * Draw transport networks
         */
        private void drawTransportNetworks() {
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
        
        /**
         * 绘制指定类型的传输网络
         * Draw transport network of specified type
         */
        public void drawTransportNetwork(Building target, TransportType type) {
            // 使用收集器收集连接
            Seq<Connection> inputs = collector.collect(target, type, true);
            Seq<Connection> outputs = collector.collect(target, type, false);
            
            // 过滤有效连接
            Seq<Connection> validInputs = new Seq<>();
            Seq<Connection> validOutputs = new Seq<>();
            
            for(Connection conn : inputs) {
                if(type == TransportType.ITEM ? 
                   logic.canOutput(conn.from, conn.to, null) : 
                   logic.canOutput(conn.from, conn.to, (Liquid)null)) {
                    validInputs.add(conn);
                }
            }
            
            for(Connection conn : outputs) {
                if(type == TransportType.ITEM ? 
                   logic.canOutput(conn.from, conn.to, null) : 
                   logic.canOutput(conn.from, conn.to, (Liquid)null)) {
                    validOutputs.add(conn);
                }
            }
            
            // 绘制连接
            Color inputColor = type == TransportType.ITEM ? itemInputColor : liquidInputColor;
            Color outputColor = type == TransportType.ITEM ? itemOutputColor : liquidOutputColor;
            
            drawConnections(validInputs, inputColor, false);
            drawConnections(validOutputs, outputColor, true);
        }
        
        /**
         * 绘制连接集合
         * Draw connection collection
         */
        public void drawConnections(Seq<Connection> connections, Color color, boolean isOutput) {
            // 绘制连接线
            for(Connection conn : connections) {
                if(!conn.isEndpoint) {
                    drawConnection(conn.from, conn.to, color, isOutput);
                } else {
                    // 端点用不同的颜色高亮
                    Color endpointColor = color.cpy().mul(1.2f);
                    drawSelectedBuilding(conn.to, endpointColor);
                }
            }
            
            // 绘制方向指示器
            for(Connection conn : connections) {
                if(!conn.isEndpoint) {
                    drawDirectionIndicator(conn.from, conn.to, color, isOutput);
                }
            }
        }
        
        /**
         * 绘制单个连接
         * Draw single connection
         */
        private void drawConnection(Building from, Building to, Color color, boolean isOutput) {
            float x1 = from.tile.drawx(), y1 = from.tile.drawy();
            float x2 = to.tile.drawx(), y2 = to.tile.drawy();
            
            Draw.color(Tmp.c1.set(color).a(Mathf.absin(4f, 1f) * 0.4f + 0.6f));
            Lines.stroke(1.5f);
            Lines.line(x1, y1, x2, y2);
            Draw.reset();
        }
        
        /**
         * 绘制方向指示器
         * Draw direction indicator
         */
        private void drawDirectionIndicator(Building from, Building to, Color color, boolean isOutput) {
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
        
        /**
         * 绘制选中的建筑高亮
         * Draw selected building highlight
         */
        private void drawSelectedBuilding(Building building, Color color) {
            Drawf.selected(building, Tmp.c1.set(color).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));
        }
    }
    
    // ===== 传输类型和连接数据结构 Transport Types and Connection Data Structures =====
    
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