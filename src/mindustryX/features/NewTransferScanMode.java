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
     * 收集结果数据类
     * Collection result data class
     */
    public static class CollectResult {
        public final Building build;
        public final boolean isOutput; // true = output, false = input
        public final TransportType type;
        
        public CollectResult(Building build, boolean isOutput, TransportType type) {
            this.build = build;
            this.isOutput = isOutput;
            this.type = type;
        }
    }
    
    /**
     * 收集器 - 负责收集建筑的连接信息，处理绝大部分建筑类型相关的逻辑
     * Collector - responsible for collecting building connection information and handling most building type logic
     */
    private static class TransportCollector {
        
        /**
         * 收集指定建筑的所有连接信息
         * Collect all connections for the specified building
         */
        public Seq<CollectResult> collect(Building self) {
            Seq<CollectResult> results = new Seq<>();
            if(self == null) return results;
            
            // 收集物品传输连接
            if(self.block.hasItems) {
                collectItemConnections(self, results);
            }
            
            // 收集液体传输连接
            if(self.block.hasLiquids) {
                collectLiquidConnections(self, results);
            }
            
            return results;
        }
        
        /**
         * 收集物品传输连接
         * Collect item transport connections
         */
        private void collectItemConnections(Building self, Seq<CollectResult> results) {
            // 传送带类
            if(self instanceof Conveyor.ConveyorBuild || self instanceof Duct.DuctBuild) {
                handleConveyorConnections(self, results);
            }
            // 塑钢传送带
            else if(self instanceof StackConveyor.StackConveyorBuild stackConveyor) {
                handleStackConveyorConnections(stackConveyor, results);
            }
            // 路由器和交叉器
            else if(self instanceof Router.RouterBuild || self instanceof Junction.JunctionBuild || 
                    self instanceof DuctRouter.DuctRouterBuild || self instanceof DuctJunction.DuctJunctionBuild) {
                handleRouterConnections(self, results);
            }
            // 分拣器
            else if(self instanceof Sorter.SorterBuild sorter) {
                handleSorterConnections(sorter, results);
            }
            // 溢流门
            else if(self instanceof OverflowGate.OverflowGateBuild || self instanceof OverflowDuct.OverflowDuctBuild) {
                handleOverflowConnections(self, results);
            }
            // 定向卸载器
            else if(self instanceof DirectionalUnloader.DirectionalUnloaderBuild) {
                handleDirectionalUnloaderConnections(self, results);
            }
            // 物品桥
            else if(self instanceof ItemBridge.ItemBridgeBuild bridge) {
                handleItemBridgeConnections(bridge, results);
            }
            // 质量驱动器
            else if(self instanceof MassDriver.MassDriverBuild massDriver) {
                handleMassDriverConnections(massDriver, results);
            }
            // 导管桥
            else if(self instanceof DirectionBridge.DirectionBridgeBuild dirBridge) {
                handleDirectionBridgeConnections(dirBridge, results);
            }
            // 生产建筑和存储建筑
            else if(hasProduction(self.block) || hasStorage(self.block)) {
                handleGenericItemConnections(self, results);
            }
        }
        
        /**
         * 收集液体传输连接
         * Collect liquid transport connections
         */
        private void collectLiquidConnections(Building self, Seq<CollectResult> results) {
            // 导管
            if(self instanceof Conduit.ConduitBuild) {
                handleConduitConnections(self, results);
            }
            // 液体路由器
            else if(self instanceof LiquidRouter.LiquidRouterBuild) {
                handleLiquidRouterConnections(self, results);
            }
            // 液体桥
            else if(self instanceof LiquidBridge.LiquidBridgeBuild bridge) {
                handleLiquidBridgeConnections(bridge, results);
            }
            // 生产建筑和存储建筑
            else if(hasProduction(self.block) || hasStorage(self.block)) {
                handleGenericLiquidConnections(self, results);
            }
        }
        
        // 传送带类连接处理
        private void handleConveyorConnections(Building self, Seq<CollectResult> results) {
            // 输出：向前方
            Building front = self.front();
            if(front != null && canAcceptItems(front, self)) {
                results.add(new CollectResult(front, true, TransportType.ITEM));
            }
            
            // 输入：从除前方外的相邻建筑
            for(Building nearby : self.proximity) {
                if(nearby != front && canOutputItems(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.ITEM));
                }
            }
        }
        
        // 塑钢传送带连接处理
        private void handleStackConveyorConnections(StackConveyor.StackConveyorBuild stackConveyor, Seq<CollectResult> results) {
            Building front = stackConveyor.front();
            Building back = stackConveyor.back();
            
            switch(stackConveyor.state) {
                case 2: // 输出模式
                    if(((StackConveyor)stackConveyor.block).outputRouter) {
                        // 向除后方外的所有方向输出
                        for(Building nearby : stackConveyor.proximity) {
                            if(nearby != back && canAcceptItems(nearby, stackConveyor)) {
                                results.add(new CollectResult(nearby, true, TransportType.ITEM));
                            }
                        }
                    } else {
                        // 只向前方输出
                        if(front != null && canAcceptItems(front, stackConveyor)) {
                            results.add(new CollectResult(front, true, TransportType.ITEM));
                        }
                    }
                    // 只从后方的塑钢传送带接收
                    if(back instanceof StackConveyor.StackConveyorBuild) {
                        results.add(new CollectResult(back, false, TransportType.ITEM));
                    }
                    break;
                case 1: // 输入模式
                    // 向前方输出
                    if(front != null && canAcceptItems(front, stackConveyor)) {
                        results.add(new CollectResult(front, true, TransportType.ITEM));
                    }
                    // 从除前方外的相邻建筑接收
                    for(Building nearby : stackConveyor.proximity) {
                        if(nearby != front && canOutputItems(nearby, stackConveyor)) {
                            results.add(new CollectResult(nearby, false, TransportType.ITEM));
                        }
                    }
                    break;
                default: // 待机模式
                    // 只与塑钢传送带连接
                    for(Building nearby : stackConveyor.proximity) {
                        if(nearby instanceof StackConveyor.StackConveyorBuild) {
                            results.add(new CollectResult(nearby, true, TransportType.ITEM));
                            results.add(new CollectResult(nearby, false, TransportType.ITEM));
                        }
                    }
                    break;
            }
        }
        
        // 路由器和交叉器连接处理
        private void handleRouterConnections(Building self, Seq<CollectResult> results) {
            // 与所有相邻建筑双向连接
            for(Building nearby : self.proximity) {
                if(canAcceptItems(nearby, self)) {
                    results.add(new CollectResult(nearby, true, TransportType.ITEM));
                }
                if(canOutputItems(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.ITEM));
                }
            }
        }
        
        // 分拣器连接处理
        private void handleSorterConnections(Sorter.SorterBuild sorter, Seq<CollectResult> results) {
            // 输出：向前方和侧面（不向后方）
            for(Building nearby : sorter.proximity) {
                int relativeDir = sorter.relativeTo(nearby);
                if(relativeDir == sorter.rotation || relativeDir != (sorter.rotation + 2) % 4) {
                    if(canAcceptItems(nearby, sorter)) {
                        results.add(new CollectResult(nearby, true, TransportType.ITEM));
                    }
                }
            }
            
            // 输入：从除前方外的相邻建筑
            for(Building nearby : sorter.proximity) {
                if(sorter.relativeTo(nearby) != sorter.rotation && canOutputItems(nearby, sorter)) {
                    results.add(new CollectResult(nearby, false, TransportType.ITEM));
                }
            }
        }
        
        // 溢流门连接处理
        private void handleOverflowConnections(Building self, Seq<CollectResult> results) {
            // 输出：向除后方外的所有方向
            for(Building nearby : self.proximity) {
                if(self.relativeTo(nearby) != (self.rotation + 2) % 4 && canAcceptItems(nearby, self)) {
                    results.add(new CollectResult(nearby, true, TransportType.ITEM));
                }
            }
            
            // 输入：从除前方外的相邻建筑
            for(Building nearby : self.proximity) {
                if(self.relativeTo(nearby) != self.rotation && canOutputItems(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.ITEM));
                }
            }
        }
        
        // 定向卸载器连接处理
        private void handleDirectionalUnloaderConnections(Building self, Seq<CollectResult> results) {
            // 只向前方输出，不接受输入
            Building front = self.front();
            if(front != null && canAcceptItems(front, self)) {
                results.add(new CollectResult(front, true, TransportType.ITEM));
            }
        }
        
        // 物品桥连接处理
        private void handleItemBridgeConnections(ItemBridge.ItemBridgeBuild bridge, Seq<CollectResult> results) {
            // 输出：优先通过链接，否则向相邻建筑
            if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                Building linkTarget = world.build(bridge.link);
                if(linkTarget != null) {
                    results.add(new CollectResult(linkTarget, true, TransportType.ITEM));
                }
            } else {
                // 无链接时向相邻建筑输出
                for(Building nearby : bridge.proximity) {
                    if(canAcceptItems(nearby, bridge)) {
                        results.add(new CollectResult(nearby, true, TransportType.ITEM));
                    }
                }
            }
            
            // 输入：从后方和链接源接收
            Building back = bridge.back();
            if(back != null && canOutputItems(back, bridge)) {
                results.add(new CollectResult(back, false, TransportType.ITEM));
            }
            
            // 从链接源接收
            bridge.incoming.each(pos -> {
                Building source = world.tile(pos).build;
                if(source != null) {
                    results.add(new CollectResult(source, false, TransportType.ITEM));
                }
            });
        }
        
        // 质量驱动器连接处理
        private void handleMassDriverConnections(MassDriver.MassDriverBuild massDriver, Seq<CollectResult> results) {
            // 输出：通过链接发射
            if(massDriver.linkValid()) {
                Building target = world.build(massDriver.link);
                if(target != null) {
                    results.add(new CollectResult(target, true, TransportType.ITEM));
                }
            }
            
            // 输入：从其他质量驱动器接收
            for(Building other : massDriver.proximity) {
                if(other instanceof MassDriver.MassDriverBuild otherMd && 
                   otherMd.linkValid() && world.build(otherMd.link) == massDriver) {
                    results.add(new CollectResult(other, false, TransportType.ITEM));
                }
            }
            
            // 从相邻建筑接收物品
            for(Building nearby : massDriver.proximity) {
                if(canOutputItems(nearby, massDriver)) {
                    results.add(new CollectResult(nearby, false, TransportType.ITEM));
                }
            }
        }
        
        // 导管桥连接处理
        private void handleDirectionBridgeConnections(DirectionBridge.DirectionBridgeBuild dirBridge, Seq<CollectResult> results) {
            // 输出：通过链接
            DirectionBridge.DirectionBridgeBuild link = dirBridge.findLink();
            if(link != null) {
                results.add(new CollectResult(link, true, TransportType.ITEM));
            }
            
            // 输入：从占用的建筑
            for(Building occupied : dirBridge.occupied) {
                if(occupied != null) {
                    results.add(new CollectResult(occupied, false, TransportType.ITEM));
                }
            }
        }
        
        // 通用物品连接处理
        private void handleGenericItemConnections(Building self, Seq<CollectResult> results) {
            for(Building nearby : self.proximity) {
                // 可以向相邻建筑输出
                if(self.canDump(nearby, null)) {
                    results.add(new CollectResult(nearby, true, TransportType.ITEM));
                }
                // 可以从相邻建筑接收
                if(canOutputItems(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.ITEM));
                }
            }
        }
        
        // 导管连接处理
        private void handleConduitConnections(Building self, Seq<CollectResult> results) {
            // 输出：向前方
            Building front = self.front();
            if(front != null && canAcceptLiquids(front, self)) {
                results.add(new CollectResult(front, true, TransportType.LIQUID));
            }
            
            // 输入：从除前方外的相邻建筑
            for(Building nearby : self.proximity) {
                if(nearby != front && canOutputLiquids(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.LIQUID));
                }
            }
        }
        
        // 液体路由器连接处理
        private void handleLiquidRouterConnections(Building self, Seq<CollectResult> results) {
            // 与所有相邻建筑双向连接
            for(Building nearby : self.proximity) {
                if(canAcceptLiquids(nearby, self)) {
                    results.add(new CollectResult(nearby, true, TransportType.LIQUID));
                }
                if(canOutputLiquids(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.LIQUID));
                }
            }
        }
        
        // 液体桥连接处理
        private void handleLiquidBridgeConnections(LiquidBridge.LiquidBridgeBuild bridge, Seq<CollectResult> results) {
            // 输出：优先通过链接
            if(bridge.block.linkValid(bridge.tile, world.tile(bridge.link))) {
                Building linkTarget = world.build(bridge.link);
                if(linkTarget != null) {
                    results.add(new CollectResult(linkTarget, true, TransportType.LIQUID));
                }
            } else {
                // 无链接时向相邻建筑输出
                for(Building nearby : bridge.proximity) {
                    if(bridge.canDump(nearby, null)) {
                        results.add(new CollectResult(nearby, true, TransportType.LIQUID));
                    }
                }
            }
            
            // 输入：从后方和链接源接收
            Building back = bridge.back();
            if(back != null && canOutputLiquids(back, bridge)) {
                results.add(new CollectResult(back, false, TransportType.LIQUID));
            }
            
            // 从链接源接收
            bridge.incoming.each(pos -> {
                Building source = world.tile(pos).build;
                if(source != null) {
                    results.add(new CollectResult(source, false, TransportType.LIQUID));
                }
            });
        }
        
        // 通用液体连接处理
        private void handleGenericLiquidConnections(Building self, Seq<CollectResult> results) {
            if(!self.block.hasLiquids) return;
            
            for(Building nearby : self.proximity) {
                // 可以向相邻建筑输出
                if(self.canDump(nearby, null)) {
                    results.add(new CollectResult(nearby, true, TransportType.LIQUID));
                }
                // 可以从相邻建筑接收
                if(canOutputLiquids(nearby, self)) {
                    results.add(new CollectResult(nearby, false, TransportType.LIQUID));
                }
            }
        }
        
        // 辅助方法
        private boolean hasStorage(Block block) {
            return block instanceof StorageBlock || block.hasItems || block.hasLiquids;
        }
        
        private boolean hasProduction(Block block) {
            return block instanceof GenericCrafter || block instanceof Drill ||
                   block instanceof Pump || block instanceof SolidPump ||
                   block instanceof Fracker || block instanceof UnitFactory;
        }
        
        private boolean canAcceptItems(Building to, Building from) {
            return logic.canInput(to, from, (Item)null);
        }
        
        private boolean canOutputItems(Building from, Building to) {
            return logic.canOutput(from, to, (Item)null);
        }
        
        private boolean canAcceptLiquids(Building to, Building from) {
            return logic.canInput(to, from, (Liquid)null);
        }
        
        private boolean canOutputLiquids(Building from, Building to) {
            return logic.canOutput(from, to, (Liquid)null);
        }
    }
    
    // ===== 逻辑判断接口 Logic Interface =====
    
    /**
     * 逻辑判断器 - 负责传输逻辑的判断，专注于canInput/canOutput验证
     * Logic Judge - responsible for transport logic decisions, focused on canInput/canOutput validation
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
         * 判断建筑是否为端点（存储或生产建筑）
         * Check if building is an endpoint (storage or production building)
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
        
        // 简化的输入输出验证方法
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
            
            // 消费建筑和存储建筑
            if(hasConsumption(to.block) || hasStorage(to.block)) {
                return true;
            }
            
            return false;
        }
        
        private boolean canOutputItems(Building from, Building to) {
            // 传送带类
            if(from instanceof Conveyor.ConveyorBuild || from instanceof Duct.DuctBuild) {
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
        
        private boolean canOutputLiquids(Building from, Building to) {
            // 导管
            if(from instanceof Conduit.ConduitBuild) {
                return to == from.front();
            }
            
            // 液体路由器
            if(from instanceof LiquidRouter.LiquidRouterBuild) {
                return true;
            }
            
            // 生产建筑
            if(hasProduction(from.block) || hasStorage(from.block)) {
                return from.canDump(to, null);
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
            
            // 使用新的收集接口
            Seq<CollectResult> connections = collector.collect(target);
            
            // 分离输入和输出连接
            Seq<CollectResult> itemInputs = new Seq<>();
            Seq<CollectResult> itemOutputs = new Seq<>();
            Seq<CollectResult> liquidInputs = new Seq<>();
            Seq<CollectResult> liquidOutputs = new Seq<>();
            
            for(CollectResult result : connections) {
                if(result.type == TransportType.ITEM) {
                    if(result.isOutput) {
                        itemOutputs.add(result);
                    } else {
                        itemInputs.add(result);
                    }
                } else if(result.type == TransportType.LIQUID) {
                    if(result.isOutput) {
                        liquidOutputs.add(result);
                    } else {
                        liquidInputs.add(result);
                    }
                }
            }
            
            // 绘制连接
            drawCollectResults(itemInputs, itemInputColor, false);
            drawCollectResults(itemOutputs, itemOutputColor, true);
            drawCollectResults(liquidInputs, liquidInputColor, false);
            drawCollectResults(liquidOutputs, liquidOutputColor, true);
        }
        
        /**
         * 绘制收集结果连接
         * Draw collect result connections
         */
        private void drawCollectResults(Seq<CollectResult> results, Color color, boolean isOutput) {
            // 分离传输建筑和端点建筑
            Seq<CollectResult> transportConnections = new Seq<>();
            Seq<CollectResult> endpointConnections = new Seq<>();
            
            for(CollectResult result : results) {
                if(logic.isEndpoint(result.build, result.type)) {
                    endpointConnections.add(result);
                } else {
                    transportConnections.add(result);
                }
            }
            
            // 绘制传输连接
            for(CollectResult result : transportConnections) {
                Building target = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y).build;
                if(isOutput) {
                    drawConnection(target, result.build, color, true);
                    drawDirectionIndicator(target, result.build, color, true);
                } else {
                    drawConnection(result.build, target, color, false);
                    drawDirectionIndicator(result.build, target, color, false);
                }
            }
            
            // 绘制端点高亮
            for(CollectResult result : endpointConnections) {
                Color endpointColor = color.cpy().mul(1.2f);
                drawSelectedBuilding(result.build, endpointColor);
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
    
    // ===== 传输类型枚举 Transport Type Enum =====
    
    public enum TransportType {
        ITEM, LIQUID
    }
}