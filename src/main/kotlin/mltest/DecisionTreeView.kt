package mltest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import mltest.DecisionTree.Decision


@Composable
fun DecisionTreeViewTest() {
    DecisionTreeView(
        DecisionTreeTTTAI.createDecisionTree(
            TTTDecisionContext(
                iAmX = true,
                board = ProjectedTTTState()
            )
        )
    ) {
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
fun <D, S> DecisionTreeView(tree: DecisionTree<D, S>, displayNode: @Composable (S) -> Unit) {
    val layers = remember { tree.collectLayers() }
    val first2 = remember { layers.take(3) }

    val nodePositions = remember { PositionHolder<S>() }
    Box {
        FreeformMovement {
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

//            val p1 = nodePositions.positionOf(first2[0][0])
//            val p2 = nodePositions.positionOf(first2[1][0])
//
//            println("p1 = $p1, p2 = $p2")
//
//            if (p1 != null && p2 != null) {
//                drawLine(Color.Red, p1.bottomCenter, p2.topCenter)
//            }

        }

    }

}


private fun <S> DecisionTree<*, S>.collectLayers(): List<List<S>> {
    var row = listOf(this)
    val result = mutableListOf<List<S>>()
    while (row.isNotEmpty()) {
        result.add(row.map { it.state })
        row = row.flatMap { layerNode -> layerNode.choices.map { it.node } }
    }

    return result
}


class DecisionTree<D, S>(
    val state: S,
    val choices: List<Decision<D, S>>,
) {

    data class Decision<D, S>(val decision: D, val node: DecisionTree<D, S>)

    companion object {
        fun <Decision, Result> create(
            root: Result,
            options: (Result) -> List<DeterministicDecision<Decision, Result>>,
        ): DecisionTree<Decision, Result> {
            return DecisionTree(
                state = root, choices = options(root)
                    .map { (decision, result) -> Decision(decision, create(result, options)) }
            )
        }
    }

    fun visitChildren(depth: Int, visitor: (parent: S, causingChoice: D, child: S) -> Unit) {
        if (depth == 0) return
        for (choice in choices) {
            visitor(this.state, choice.decision, choice.node.state)
            choice.node.visitChildren(depth - 1, visitor)
        }
    }


    fun choose(fitnessFunc: (S) -> Double): D {
        val choice = choices.maxBy {
            it.node.myChoiceScore(fitnessFunc)
        }

        return choice.decision
    }

    private fun myChoiceScore(fitnessFunc: (S) -> Double): Double {
        if (choices.isEmpty()) return fitnessFunc(state)
        else {
            // We assume the opponent will pick the best choice
            val worstOpponentChoiceForMe = choices.minOf {
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

    private fun opponentChoiceScore(fitnessFunc: (S) -> Double): Double {
        if (choices.isEmpty()) return fitnessFunc(state)
        // We assume the opponent will assume we will pick the best choice
        val bestMyChoiceForMe = choices.maxOf {
            it.node.myChoiceScore(fitnessFunc)
        }
        return bestMyChoiceForMe
    }
}