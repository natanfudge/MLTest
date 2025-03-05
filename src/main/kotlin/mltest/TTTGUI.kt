package mltest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TTTView(state: MutableTTTState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Table(rows = 3, columns = 3) { row, column ->
            val cell = TTTCell(row, column)
            val win = state.getWinningLine()
            Box(
                Modifier.border(1.dp, Color.Black).size(50.dp)
                    .addIf(win == null && state.canPlaceAt(cell)) {
                        clickable {
                            state.makePlacement(cell)
                            if (!state.isOver()) {
                                val aiPlacement = DecisionTreeTTTAI.decide(TTTDecisionContext(iAmX = true, state))
                                state.makePlacement(aiPlacement)
                            }

                        }
                    }.addIf(win != null && cell in win.winningLine) {
                        background(Color.Red.copy(alpha = 0.3f))
                    }
            ) {
                when (state.board[row, column]) {
                    TTTCellValue.EMPTY -> {

                    }

                    TTTCellValue.X -> Text("X", Modifier.align(Alignment.Center), fontSize = 30.sp)
                    TTTCellValue.O -> Text("O", Modifier.align(Alignment.Center), fontSize = 30.sp)
                }
            }
        }

        Button(onClick = { state.reset() }) {
            Text("Reset")
        }
    }

}