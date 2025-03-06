package mltest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface TTTState: Array2D<TTTCellValue> {
    val xHasTurn: Boolean
}

fun TTTState.get(cell: TTTCell): TTTCellValue = get(cell.row, cell.column)

fun TTTState.canPlaceAt(cell: TTTCell): Boolean {
    return get(cell) == TTTCellValue.EMPTY
}

fun TTTState.isOver() = getWinningLine() != null || boardIsFull()

fun TTTState.boardIsFull() = (0..2).all { row -> (0..2).all { col -> get(TTTCell(row, col)) != TTTCellValue.EMPTY } }

/**
 * If a player has won - return how, otherwise  - return null
 */
fun TTTState.getWinningLine(): GameResult? {
    for (line in possibleWinningLines) {
        if (line.all { get(it) == TTTCellValue.X }) return GameResult(line, winnerIsX = true)
        if (line.all { get(it) == TTTCellValue.O }) return GameResult(line, winnerIsX = false)
    }
    return null
}


class MutableTTTState: TTTState {
    val board = StateArray2D(3, 3, TTTCellValue.EMPTY)
    override var xHasTurn by mutableStateOf(true)

    override val columns: Int = 3
    override val rows: Int = 3
    override fun get(row: Int, column: Int): TTTCellValue  = board[row, column]

    private fun set(cell: TTTCell, value: TTTCellValue) {
        board[cell.row, cell.column] = value
    }

    fun makePlacement(cell: TTTCell) {
        set(cell, if (xHasTurn) TTTCellValue.X else TTTCellValue.O)
        xHasTurn = !xHasTurn
    }

    override fun toString(): String {
        return "Turn: ${if (xHasTurn) "X" else "O"}: \n" + board
    }

    fun reset() {
        board.clear()
        xHasTurn = false
    }
}

private val possibleWinningLines = listOf(
    rowWin(0),
    rowWin(1),
    rowWin(2),
    colWin(0),
    colWin(1),
    colWin(2),
    WinningLine(
        TTTCell(0, 0), TTTCell(1, 1), TTTCell(2, 2),
    ), // Main diagonal
    WinningLine(
        TTTCell(2, 0), TTTCell(1, 1), TTTCell(0, 2),
    ) // Secondary diagonal
)

private fun rowWin(row: Int): WinningLine = WinningLine(
    TTTCell(row, 0), TTTCell(row, 1), TTTCell(row, 2),
)

private fun colWin(col: Int): WinningLine = WinningLine(
    TTTCell(0, col), TTTCell(1, col), TTTCell(2, col),
)

data class TTTCell(val row: Int, val column: Int) {
    init {
        require(row in 0..2 && column in 0..2)
    }

    override fun toString(): String {
        return "($row,$column)"
    }
}

data class GameResult(
    val winningLine: WinningLine,
    val winnerIsX: Boolean,
)

data class WinningLine(val a: TTTCell, val b: TTTCell, val c: TTTCell) : List<TTTCell> by listOf(a, b, c)

enum class TTTCellValue {
    EMPTY, X, O;

    override fun toString(): String  = when(this) {
        TTTCellValue.EMPTY ->  " "
        TTTCellValue.X -> "X"
        TTTCellValue.O -> "O"
    }
}

