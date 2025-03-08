package mltest

interface AI<I, O> {
    fun decide(input: I): O
}

typealias TTTPlacementDecision = TTTCell

interface TTTAI : AI<TTTDecisionContext, TTTPlacementDecision>

object DecisionTreeTTTAI : TTTAI {

    fun createDecisionTree(input: TTTDecisionContext): TTTDecisionTree {
        return DecisionTree.create(input) {
            possibleDecisions(it)
        }
    }

    override fun decide(input: TTTDecisionContext): TTTPlacementDecision {
        val tree: TTTDecisionTree = createDecisionTree(input)
        val choice = tree.choose {
            val winner = it.board.getWinningLine()
            if (winner == null) 0.0 // No one won / tie
            else {
                if (winner.winnerIsX == input.iAmX) 1.0
                else -1.0
            }
        }
        return choice
    }

    private fun possibleDecisions(state: TTTDecisionContext): List<TTTDecisionResult> {
        if (state.board.getWinningLine() != null) return listOf()
        return buildList {
            repeat(3) { row ->
                repeat(3) { col ->
                    val cell = TTTCell(row, col)
                    if (state.board.canPlaceAt(cell)) {
                        val result = state.withChoice(cell)
                        add(TTTDecisionResult(cell, result))
                    }
                }
            }
        }
    }

    private fun TTTDecisionContext.withChoice(choice: TTTPlacementDecision): TTTDecisionContext {
        val newCells = board.withSet(
            choice.row, choice.column, if (board.xHasTurn) TTTCellValue.X else TTTCellValue.O
        )

        return copy(board = ProjectedTTTState(newCells, xHasTurn = !board.xHasTurn))
    }


}

class ProjectedTTTState(
    private val board: Array2D<TTTCellValue> = Array2D(3, 3) { _, _ -> TTTCellValue.EMPTY },
    override val xHasTurn: Boolean = true,
) : TTTState, Array2D<TTTCellValue> by board {
    override fun toString(): String {
        return "Turn: ${if (xHasTurn) "X" else "O"}: \n" + board
    }
}


typealias TTTDecisionTree = DecisionTree<TTTPlacementDecision, TTTDecisionContext>
typealias TTTDecisionResult = DeterministicDecision<TTTPlacementDecision, TTTDecisionContext>

// TODO: First do some visualizations for easy debugging, and only then we'll do this stuff:
//TODO: next thing to clean up: decision tree. I'm thinking of adding distinction between my player and
// opponent player actions, and then having a "Prediction Policy" that can assume the opponent might
// do bad plays.


data class DeterministicDecision<Decision, State>(
    val decision: Decision,
    val result: State,
)

data class TTTDecisionContext(
    val iAmX: Boolean,
    val board: TTTState,
) {
    override fun toString(): String {
        return "Me=${if (iAmX) "X" else "O"}, $board"
    }
}