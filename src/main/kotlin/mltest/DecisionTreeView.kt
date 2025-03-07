package mltest

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mltest.DecisionTree.Decision


@Composable
fun DecisionTreeViewTest() {
    DecisionTreeView(
        DecisionTreeTTTAI.createDecisionTree(
            TTTDecisionContext(
                iAmX = true,
                board = MutableTTTState()
            )
        )
    )
}

fun Modifier.freeformMovement(transform: TransformationMatrix2D) = this then FreeformMovementElement(transform)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <D, S> DecisionTreeView(tree: DecisionTree<D, S>) {
    val layers = tree.collectLayers()
    val first2 = layers.take(2)

    var transform by remember { mutableStateOf(TransformationMatrix2D()) }

    println("Zom = $transform")

    Box(
        Modifier.fillMaxSize().border(1.dp, Color.Red)
            .onPointerEvent(PointerEventType.Scroll) { pointerEvent ->
                pointerEvent.changes.forEach { change ->
                    transform = transform.scale(1 + change.scrollDelta.y * -0.3f, focalPoint = change.position)
                }
            }) {
        Column(Modifier.freeformMovement(transform), horizontalAlignment = Alignment.CenterHorizontally) {
            for (layer in first2) {
                Row {
                    for (possibility in layer) {
                        Text(possibility.toString())
                    }
                }
            }
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

private data class FreeformMovementElement(val transformation: TransformationMatrix2D) :
    ModifierNodeElement<FreeformMovementNode>() {
    override fun create() = FreeformMovementNode(transformation)

    override fun update(node: FreeformMovementNode) {
        node.transformation = transformation
    }
}


private class FreeformMovementNode(
    var transformation: TransformationMatrix2D,
) : LayoutModifierNode, Modifier.Node() {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(
            Constraints(
                minWidth = 0,
                minHeight = 0,
                maxWidth = Constraints.Infinity,
                maxHeight = Constraints.Infinity
            )
        )
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(x = 0, y = 0) {
                this.transformOrigin = TransformOrigin(0f, 0f)
                scaleX = transformation.scale
                scaleY = transformation.scale
                translationX = transformation.translateX
                translationY = transformation.translateY
            }
        }
    }
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