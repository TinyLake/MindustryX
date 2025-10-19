package mindustryX.features.ui

import arc.Core
import arc.math.geom.Rect
import kotlin.math.abs

/**
 * Central coordinator for managing rectangular element adsorption logic.
 *
 * The AdsorptionSystem handles constraint-based positioning of rectangular frames,
 * allowing each frame to snap, align, or attach to others based on defined anchor relationships.
 * It supports axis-specific constraints (X and Y), enforces single constraint per axis,
 * and resolves positions dynamically to maintain layout consistency.
 *
 * Key responsibilities include:
 * - Managing frame registration and constraint resolution
 * - Preventing circular or conflicting adsorption chains
 * - Providing anchor-based coordinate computation
 * - Supporting serialization and layout persistence
 *
 * This system is designed to be extensible for UI layout engines, graphical editors,
 * or any environment requiring dynamic spatial alignment between rectangular entities.
 *
 * @author WayZer
 */
object AdsorptionSystem {
    const val ADSORPTION_DISTANCE = 16f

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

    data class Constraint(val axis: Axis, val target: String, val type: ConstraintType) {
        @Suppress("unused")//For Json
        private constructor() : this(Axis.X, "", ConstraintType.AlignLeading)

        val targetPoint get() = all[target]
    }

    class Element(val name: String) {
        val rect: Rect = Rect()
        val dependencies = mutableSetOf<Element>()
        var lastUpdate = 0L

        fun reset(x: Float, y: Float, width: Float, height: Float) {
            lastUpdate = Core.graphics.frameId
            rect.set(x, y, width, height)
            all[name] = this
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

        fun applyConstraint(constraint: Constraint) {
            val target = constraint.targetPoint ?: return
            dependencies.add(target)
            val cur = computeAnchor(constraint.axis, constraint.type.sourceAnchor)
            val tar = target.computeAnchor(constraint.axis, constraint.type.targetAnchor)
            val delta = tar - cur
            if (constraint.axis == Axis.X) {
                rect.x += delta
            } else {
                rect.y += delta
            }
        }

        fun findBestConstraint(target: Element, axis: Axis): Pair<Constraint, Float>? {
            return ConstraintType.entries.map { it to abs(computeAnchor(axis, it.sourceAnchor) - target.computeAnchor(axis, it.targetAnchor)) }
                .filter { it.second < ADSORPTION_DISTANCE }
                .minByOrNull { it.second }
                ?.let { Constraint(axis, target.name, it.first) to it.second }
        }

        fun findBestConstraints(): Pair<Constraint?, Constraint?> {
            val available = filterCandidates(this)
            return (available.mapNotNull { target -> findBestConstraint(target, Axis.X) }.minByOrNull { it.second }?.first) to
                    (available.mapNotNull { target -> findBestConstraint(target, Axis.Y) }.minByOrNull { it.second }?.first)
        }


        fun remove() {
            if (all[name] == this)
                all.remove(name)
        }
    }

    val all = mutableMapOf<String, Element>()
    val updaters = mutableListOf<Runnable>()

    val scene = Element("scene").apply {
        updaters.add {
            reset(0f, 0f, Core.scene.width, Core.scene.height)
        }
    }
    val placementRect = Element("placementRect")

    fun addDynamic(name: String, updater: Element.() -> Unit) {
        val elem = Element(name)
        updaters += Runnable {
            elem.updater()
        }
    }

    fun update() {
        updaters.forEach { it.run() }
    }

    private fun filterCandidates(forPoint: Element): List<Element> {
        // 构造反向依赖图：谁依赖了谁
        val reverseDeps = mutableMapOf<Element, MutableList<Element>>()
        for (point in all.values) {
            for (dep in point.dependencies) {
                reverseDeps.getOrPut(dep) { mutableListOf() }.add(point)
            }
        }

        // 从 forPoint 出发，找出所有依赖它的节点
        val excluded = mutableSetOf<Element>()
        fun dfs(current: Element) {
            if (!excluded.add(current)) return
            reverseDeps[current]?.forEach { dfs(it) }
        }

        dfs(forPoint)

        // 只考虑相邻吸附
        val around = Rect.tmp.set(forPoint.rect).grow(2 * ADSORPTION_DISTANCE)
        return all.values.filter {
            it.lastUpdate == Core.graphics.frameId
                    && around.overlaps(it.rect)
                    && it !in excluded
        }
    }
}