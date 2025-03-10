package mltest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize


@Composable
fun DecisionTreeViewTest() {
    val tree = remember {
        DecisionTreeTTTAI.createDecisionTree(
            TTTDecisionContext(
                iAmX = true,
                board = ProjectedTTTState()
            )
        )
    }
    DecisionTreeView(tree) {
        TTTView(it.board)
    }

}

class PositionHolder<T> {
    private val positionMap = mutableStateMapOf<T, Rect>()

    fun updatePosition(key: T, pos: Rect) {
        positionMap[key] = pos
    }

    fun positionOf(key: T): Rect? {
        return positionMap[key]
    }
}

@Composable
fun <T> Modifier.trackPosition(positionHolder: PositionHolder<T>, byKey: T): Modifier {
    val movementState = LocalFreeformMovementState.current
    return onGloballyPositioned {
        println("Update to ${it.positionInRoot()}")
        positionHolder.updatePosition(
            byKey, Rect(
                it.positionInRoot(), it.size.toSize() * movementState.transform.scale
            )
        )
    }
}

//class PositionMap


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T, L> DecisionTreeView(tree: LabeledEdgeTree<T, L>, displayNode: @Composable (T) -> Unit) {
    val layers = remember { tree.collectLayers() }
    val first2 = remember { layers.take(3) }

    val nodePositions = remember { PositionHolder<T>() }
    val freeform = rememberFreeformMovementState(TransformationMatrix2D().scale(0.06f).translate(0f, 100f))
    Box {
        FreeformMovement(freeform) {
            ResizeWidthColumn(
                verticalArrangement = Arrangement.spacedBy(500.dp)
            ) {
                for (layer in first2) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (possibility in layer) {
                            Box(Modifier.trackPosition(nodePositions, possibility).padding(horizontal = 10.dp)) {
                                displayNode(possibility)
                            }
                        }
                    }
                }

            }
        }

        Canvas(Modifier.fillMaxSize()) {
            tree.visitChildren(depth = 2) { parent, causingChoice, state ->
                val parentPos = nodePositions.positionOf(parent)
                val childPos = nodePositions.positionOf(state)


                if (parentPos != null && childPos != null) {
                    drawLine(Color.Red, parentPos.bottomCenter, childPos.topCenter)
                }
            }
        }

    }

}


fun <S> LabeledEdgeTree<S, *>.collectLayers(): List<List<S>> {
    var row = listOf(this)
    val result = mutableListOf<List<S>>()
    while (row.isNotEmpty()) {
        result.add(row.map { it.value })
        row = row.flatMap { layerNode -> layerNode.children.map { it.node } }
    }

    return result
}


data class LabeledEdge<T, L>(
    val label: L,
    val value: T,
)

class LabeledEdgeTree<T, L>(
    val value: T,
    val children: List<LabeledChild<T, L>>,
) {

    data class LabeledChild<T, L>(val label: L, val node: LabeledEdgeTree<T, L>)

    companion object {
        fun <T, L> create(
            root: T,
            options: (T) -> List<LabeledEdge<T, L>>,
        ): LabeledEdgeTree<T, L> {
            return LabeledEdgeTree<T, L>(
                value = root, children = options(root)
                    .map { (decision, result) -> LabeledChild(decision, create(result, options)) }
            )
        }
    }

    fun visitChildren(depth: Int, visitor: (parent: T, causingChoice: L, child: T) -> Unit) {
        if (depth == 0) return
        for (choice in children) {
            visitor(this.value, choice.label, choice.node.value)
            choice.node.visitChildren(depth - 1, visitor)
        }
    }


    fun choose(fitnessFunc: (T) -> Double): L {
        val choice = children.maxBy {
            it.node.myChoiceScore(fitnessFunc)
        }

        return choice.label
    }

    private fun myChoiceScore(fitnessFunc: (T) -> Double): Double {
        if (children.isEmpty()) return fitnessFunc(value)
        else {
            // We assume the opponent will pick the best choice
            val worstOpponentChoiceForMe = children.minOf {
                it.node.opponentChoiceScore(fitnessFunc)
            }
            return worstOpponentChoiceForMe
        }
    }

    //TODO: resolutions:
    // 1. Need strong Tree debugging, debugging certain choices
    // 2. Need to separate between opponent choices and my choices
    // 3. Even a decision tree is not the optimal solution, because this naive implementation only assumes the opponent
    // plays optimally, and won't try to trip up the player and win against bad players.

    private fun opponentChoiceScore(fitnessFunc: (T) -> Double): Double {
        if (children.isEmpty()) return fitnessFunc(value)
        // We assume the opponent will assume we will pick the best choice
        val bestMyChoiceForMe = children.maxOf {
            it.node.myChoiceScore(fitnessFunc)
        }
        return bestMyChoiceForMe
    }
}