@file:Suppress("DuplicatedCode")

package mltest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import kotlin.math.max

@Composable
fun Table(
    rows: Int,
    columns: Int,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
    cell: @Composable (row: Int, column: Int) -> Unit,
) {
    val measurePolicy = remember(rows, columns, verticalAlignment, horizontalAlignment) {
        MeasurePolicy { measurables, constraints ->
            val measured = measurables.map { it.measure(constraints) }

            val columnWidths = List(columns) { column ->
                var maxCellWidth = 0
                repeat(rows) { row ->
                    val i = (row * columns) + column
                    val placeable = measured[i]
                    maxCellWidth = max(placeable.width, maxCellWidth)
                }
                maxCellWidth
            }

            val rowHeights = List(rows) { row ->
                var maxCellHeight = 0
                repeat(columns) { column ->
                    val i = (row * columns) + column
                    val placeable = measured[i]
                    maxCellHeight = max(placeable.height, maxCellHeight)
                }
                maxCellHeight
            }

            val tableWidth = columnWidths.sum()
            val tableHeight = rowHeights.sum()

            layout(tableWidth, tableHeight) {
                var y = 0
                repeat(rows) { row ->
                    var x = 0
                    val rowHeight = rowHeights[row]
                    repeat(columns) { column ->
                        val i = row * columns + column
                        val placeable = measured[i]
                        val columnWidth = columnWidths[column]
                        val yOffset = verticalAlignment.align(placeable.height, rowHeight)
                        val xOffset = horizontalAlignment.align(placeable.width, columnWidth, layoutDirection)
                        placeable.placeRelative(x + xOffset, y + yOffset)

                        x += columnWidth
                    }
                    y += rowHeight
                }
            }
        }
    }

    Layout(content = {
        repeat(rows) { row ->
            repeat(columns) { column ->
                cell(row, column)
            }
        }

    }, modifier, measurePolicy = measurePolicy)
}
