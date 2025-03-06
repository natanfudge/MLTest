package mltest


/**
 * 2D Array that uses Compose state lists so updates to this array will update the UI
 */
fun <T> StateArray2D(rows: Int, columns: Int, initialValue: T): MutableArray2D<T> {
    return MutableArray2D(rows, columns, initialValue, MutableStateList(rows * columns) { initialValue })
}

/**
 * Normal read-only array
 */
inline fun <T> Array2D(rows: Int, columns: Int, init: (row: Int, col: Int) -> T): Array2D<T> {
    return ImmutableArray2D(rows, columns, List(rows * columns) { i ->
        val row = i / columns
        val col = i % columns
        init(row, col)
    })
}


interface Array2D<T> {
    operator fun get(row: Int, column: Int): T
    val rows: Int
    val columns: Int
}

/**
 * Returns a new [Array2D] with the value at [row], [column] set to [value]
 */
fun <T> Array2D<T>.withSet(row: Int, column: Int, value: T): Array2D<T> {
    return Array2D(rows, columns) { rowI, colI ->
        if (rowI == row && colI == column) value
        else get(rowI, colI)
    }
}

private fun Array2D<*>.createString(): String = buildString {
    val horizontalBorder = "-".repeat(columns * 4 + 1) + "\n"
    append(horizontalBorder)
    repeat(rows) { row ->
        append("|")
        repeat(columns) { col ->
            append(" ${get(row, col)} |")
        }
        appendLine()
        append(horizontalBorder)
    }
}

class ImmutableArray2D<T>(
    override val rows: Int, override val columns: Int, private val items: List<T>,
) : Array2D<T> {
    override operator fun get(row: Int, column: Int): T {
        return items[row * columns + column]
    }

    override fun toString(): String {
        return createString()
    }
}


class MutableArray2D<T>(
    override val rows: Int, override val columns: Int,
    val initialValue: T,
    private val items: MutableList<T>,
) : Array2D<T> {
    override fun toString(): String {
        return createString()
    }

    fun clear() {
        items.clear()
        items.addAll(List(rows * columns) { initialValue!! })
    }

    override operator fun get(row: Int, column: Int): T {
        return items[row * columns + column]
    }

    operator fun set(row: Int, column: Int, value: T) {
        items[row * columns + column] = value
    }
}
