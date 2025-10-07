package mindustryX.features.ui

import arc.Core
import arc.Events
import arc.math.geom.Rect
import mindustry.game.EventType
import kotlin.math.abs

object AdsorptionSystem {
    enum class Axis { X, Y }
    enum class Anchor {
        Leading, Center, Trailing

    }

    enum class ConstraintType(val sourceAnchor: Anchor, val targetAnchor: Anchor) {
        AlignLeading(Anchor.Leading, Anchor.Leading),
        AlignTrailing(Anchor.Trailing, Anchor.Trailing),
        AlignCenter(Anchor.Center, Anchor.Center),
        AttachTrailing(Anchor.Leading, Anchor.Trailing),
        AttachLeading(Anchor.Trailing, Anchor.Leading),
    }

    data class Constraint(val target: String, val type: ConstraintType) {
        @Suppress("unused")//For Json
        private constructor() : this("", ConstraintType.AlignLeading)
    }

    class Point(val name: String, val rect: Rect = Rect()) {
        val dependencies = mutableSetOf<Point>()

        fun reset(x: Float, y: Float, width: Float, height: Float) {
            rect.set(x, y, width, height)
            dependencies.clear()
        }

        fun computeAnchor(axis: Axis, anchor: Anchor): Float {
            return when (axis) {
                Axis.X -> when (anchor) {
                    Anchor.Leading -> rect.x
                    Anchor.Center -> rect.x + rect.width / 2
                    Anchor.Trailing -> rect.x + rect.width
                }

                Axis.Y -> when (anchor) {
                    Anchor.Leading -> rect.y
                    Anchor.Center -> rect.y + rect.height / 2
                    Anchor.Trailing -> rect.y + rect.height
                }
            }
        }

        fun apply(axis: Axis, constraint: Constraint) {
            val target = all[constraint.target] ?: return
            dependencies.add(target)
            val cur = computeAnchor(axis, constraint.type.sourceAnchor)
            val tar = target.computeAnchor(axis, constraint.type.targetAnchor)
            val delta = tar - cur
            if (axis == Axis.X) {
                rect.x += delta
            } else {
                rect.y += delta
            }
        }

        fun findBestConstraint(target: Point, axis: Axis): Pair<Constraint, Float>? {
            return ConstraintType.entries.map { it to abs(computeAnchor(axis, it.sourceAnchor) - target.computeAnchor(axis, it.targetAnchor)) }
                .filter { it.second < 16f }
                .minByOrNull { it.second }
                ?.let { Constraint(target.name, it.first) to it.second }
        }

        fun findBestConstraints(): Pair<Constraint?, Constraint?> {
            val available = filterCandidates(this)
            return (available.mapNotNull { target -> findBestConstraint(target, Axis.X) }.minByOrNull { it.second }?.first) to
                    (available.mapNotNull { target -> findBestConstraint(target, Axis.Y) }.minByOrNull { it.second }?.first)
        }


        init {
            all[name] = this
        }

        fun remove() {
            if (all[name] == this)
                all.remove(name)
        }
    }

    val all = mutableMapOf<String, Point>()
    val scene = Point("scene", Rect(0f, 0f, Core.scene.width, Core.scene.height)).apply {
        Events.on(EventType.ResizeEvent::class.java) { _ ->
            rect.setSize(Core.scene.width, Core.scene.height)
        }
    }

    private fun filterCandidates(forPoint: Point): List<Point> {
        // 构造反向依赖图：谁依赖了谁
        val reverseDeps = mutableMapOf<Point, MutableList<Point>>()
        for (point in all.values) {
            for (dep in point.dependencies) {
                reverseDeps.getOrPut(dep) { mutableListOf() }.add(point)
            }
        }

        // 从 forPoint 出发，找出所有依赖它的节点
        val excluded = mutableSetOf<Point>()
        fun dfs(current: Point) {
            if (!excluded.add(current)) return
            reverseDeps[current]?.forEach { dfs(it) }
        }

        dfs(forPoint)
        return all.values.filter { it !in excluded }
    }
}