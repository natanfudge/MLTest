package mltest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import java.util.LinkedList
import java.util.Queue
import kotlin.math.roundToInt


@Composable fun DecisionTreeViewTest() {
    DecisionTreeView(DecisionTreeTTTAI.createDecisionTree(
        TTTDecisionContext(
            iAmX = true,
            board = MutableTTTState()
        )
    ))
}

fun Modifier.zoom(amount: Float) = this then ZoomElement(amount)

@OptIn(ExperimentalComposeUiApi::class)
@Composable fun <D,S> DecisionTreeView(tree: DecisionTree<D,S>) {
    val layers = tree.collectLayers()
    val first2 = layers.take(2)

    var zom by remember { mutableStateOf(0.4f) }

    println("Zom = $zom")

    Column(Modifier.zoom(zom).onPointerEvent(PointerEventType.Scroll) {
        println("Event 2")
//        it.changes.forEach { change ->
//            zom *= (1 + change.scrollDelta.y * -0.3f)
//        }
    }, horizontalAlignment = Alignment.CenterHorizontally) {
        for(layer in first2) {
            Row {
                for(possibility in layer) {
                    Text(possibility.toString())
                }
            }
        }
    }
}


private fun <S>DecisionTree<*,S>.collectLayers(): List<List<S>> {
    var row = listOf(this)
    val result = mutableListOf<List<S>>()
    while (row.isNotEmpty()) {
        result.add(row.map { it.state })
        row = row.flatMap { layerNode -> layerNode.choices.map { it.node } }
    }

    return result
}

private data class ZoomElement(val zoom: Float) : ModifierNodeElement<ZoomModifierNode>() {
    override fun create() = ZoomModifierNode(zoom)

    override fun update(node: ZoomModifierNode) {
        node.zoom = zoom
    }
}


private class ZoomModifierNode(
    var zoom: Float,
    //TODO: var transformation: TransformationMatrix and rename to FreeformMovementNode

    //TODO: it's more complicated than a set focal point because we need a series of transformations i.e. a multiplication of matrices.

    // We can then decompose the result matrix into seperate scale/transform transformations, see
    // https://chatgpt.com/c/67ca2481-e180-8013-b4bf-c88248596ae2

//    val focalPoint: TransformOrigin
//    var zoomState: ZoomState,
//    var zoomEnabled: Boolean,
) :  LayoutModifierNode, Modifier.Node(), PointerInputModifierNode {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(Constraints(
            minWidth = 0,
            minHeight = 0,
            maxWidth = Constraints.Infinity,
            maxHeight = Constraints.Infinity
        ))
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(x = 0, y = 0) {
                this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                scaleX = zoom
                scaleY = zoom
            }
        }
    }

    override fun onCancelPointerInput() {
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        pointerEvent.changes.forEach { change ->
            if(change.scrollDelta != Offset.Zero) {
                zoom *= (1 + change.scrollDelta.y * -0.3f)
            }
        }
//        pointerEvent.changes.forEach { change ->
//            val scrollDelta = change.scrollDelta
//            if (scrollDelta != Offset.Zero) {
//                zoom += scrollDelta.y
//            }
//        }
    }
}




class DecisionTree<D, S>(
    val state: S,
    val choices: List<Decision<D, S>>,
) {

    data class Decision<D, S>(val decision: D, val node: DecisionTree<D,S>)

    companion object {
        fun <Decision, Result> create(
            root: Result,
            options: (Result) -> List<DeterministicDecision<Decision, Result>>,
        ): DecisionTree<Decision, Result> {
            return DecisionTree(
                state = root,  choices = options(root)
                    .map { (decision, result) -> Decision(decision, create(result, options))  }
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