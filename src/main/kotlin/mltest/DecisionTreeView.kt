package mltest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

class PositionMap

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <D, S> DecisionTreeView(tree: DecisionTree<D, S>, displayNode: @Composable (S) -> Unit) {
    val layers = remember { tree.collectLayers() }
    val first2 = remember { layers.take(2) }
    Box(Modifier.freeformMovement()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for (layer in first2) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (possibility in layer) {
                        displayNode(possibility)
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