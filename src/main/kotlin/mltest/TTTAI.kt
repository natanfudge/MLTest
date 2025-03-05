package mltest

interface AI<I, O> {
    fun decide(input: I): O
}

typealias TTTPlacementDecision = TTTCell

interface TTTAI : AI<TTTDecisionContext, TTTPlacementDecision>

object DecisionTreeTTTAI : TTTAI {
    override fun decide(input: TTTDecisionContext): TTTPlacementDecision {
        val tree: TTTDecisionTree = DecisionTree.create(input) {
            possibleDecisions(it)
        }
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
        val newCells = ImmutableArray2D.build(3, 3) {
            repeat(3) { row ->
                repeat(3) { col ->
                    val cell = TTTCell(row, col)
                    if (cell == choice) set(row, col, if (board.xHasTurn) TTTCellValue.X else TTTCellValue.O)
                    else set(row, col, board.get(cell))
                }
            }
        }

        return copy(board = ProjectedTTTState(newCells, xHasTurn = !board.xHasTurn))
    }

    private class ProjectedTTTState(private val board: ImmutableArray2D<TTTCellValue>,
                                    override val xHasTurn: Boolean
    ) : TTTState {
        override fun get(cell: TTTCell): TTTCellValue {
            return board[cell.row, cell.column]
        }

        override fun toString(): String {
            return buildString {
                repeat(3) { row ->
                    repeat(3) { col ->
                        append(get(TTTCell(row, col)))
                        append(" ")
                    }
                    appendLine()
                }
            }
        }
    }


}


class ImmutableArray2D<T>(
    val rows: Int,
    val columns: Int,
    private val items: List<T>,
) {
    companion object {
        fun <T> build(rows: Int, columns: Int, builder: Builder<T>. () -> Unit): ImmutableArray2D<T> =
            Builder<T>(rows, columns).apply(builder).build()
    }

    class Builder<T>(private val rows: Int, private val columns: Int) {
        private val items = MutableList<T?>(rows * columns) { null }
        operator fun set(row: Int, column: Int, value: T) {
            items[row * columns + column] = value
        }

        fun build(): ImmutableArray2D<T> {
            check(items.all { it != null })
            return ImmutableArray2D(rows, columns, items as List<T>)
        }
    }

    operator fun get(row: Int, column: Int): T {
        return items[row * columns + column]
    }
}

typealias TTTDecisionTree = DecisionTree<TTTPlacementDecision, TTTDecisionContext>
typealias TTTDecisionResult = DeterministicDecision<TTTPlacementDecision, TTTDecisionContext>

//class DecisionTreeNode<Decision, Result>(
//    val value: DeterministicDecision<Decision, Result>,
//    val children: List<DecisionTreeNode<Decision, Result>>,
//) {
//
//}

class DecisionTree<Decision, State>(
    val state: State,
    val decision: Decision?,
    val choices: List<DecisionTree<Decision, State>>,
) {


//    init {
//        check(choices.isNotEmpty())
//    }

    fun visit(visitor: (State, Decision?) -> Unit) {
        visitor(state, decision)
        choices.forEach {
            it.visit(visitor)
        }
    }

    companion object {
        fun <Decision, Result> create(
            root: Result,
            options: (Result) -> List<DeterministicDecision<Decision, Result>>,
        ): DecisionTree<Decision, Result> {
            return DecisionTree(
                state = root, decision = null, choices = options(root)
                    .map { (decision, result) -> create(result, decision, options) }
            )
        }

        private fun <Decision, Result> create(
            root: Result,
            decision: Decision,
            options: (Result) -> List<DeterministicDecision<Decision, Result>>,
        ): DecisionTree<Decision, Result> {
            return DecisionTree(
                state = root,
                decision = decision,
                choices = options(root).map { (decision, result) -> create(result, decision, options) }
            )
        }
    }


    fun choose(fitnessFunc: (State) -> Double): Decision {
//        var bestChoice: Decision = choices[0].decision!!
//        var bestScore: Double = Double.MIN_VALUE

        val choice = choices.maxBy {
            val res = it.myChoiceScore(fitnessFunc)
            res
        }

//        for (choice in choices) {
//            val score = choice.myChoiceScore(fitnessFunc)
//
//            if (score > bestScore) {
//                bestChoice = choice.decision!!
//                bestScore = score
//            }
//        }

        return choice.decision!!
    }

    private fun myChoiceScore(fitnessFunc: (State) -> Double): Double {
        if (choices.isEmpty()) return fitnessFunc(state)
        else {
            // We assume the opponent will pick the best choice
            val worstOpponentChoiceForMe = choices.minOf {
                it.opponentChoiceScore(fitnessFunc)
            }
            return worstOpponentChoiceForMe
        }
    }

    //TODO: resolutions:
    // 1. Need strong Tree debugging, debugging certain choices
    // 2. Need to separate between opponent choices and my choices
    // 3. Even a decision tree is not the optimal solution, because this naive implementation only assumes the opponent
    // plays optimally, and won't try to trip up the player and win against bad players.

    private fun opponentChoiceScore(fitnessFunc: (State) -> Double): Double {
        if (choices.isEmpty()) return fitnessFunc(state)
        // We assume the opponent will assume we will pick the best choice
        val bestMyChoiceForMe = choices.maxOf {
            it.myChoiceScore(fitnessFunc)
        }
        return bestMyChoiceForMe
    }

//    private fun choiceScore(fitnessFunc: (State) -> Double): Double {
//        if (choices.isEmpty()) return fitnessFunc(state)
//        else {
//            // My choice is only as good as the result the opponent's best choice brings
//            val worstOpponentChoiceForMe = choices.minOf { it.opponentChoiceScore(fitnessFunc) }
//            return worstOpponentChoiceForMe
//        }
//    }
}

data class DeterministicDecision<Decision, State>(
    val decision: Decision,
    val result: State,
)

data class TTTDecisionContext(
    val iAmX: Boolean,
    val board: TTTState,
)