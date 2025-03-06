package mltest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.reload.DevelopmentEntryPoint


@Composable
fun App() {
    val state = remember {
        val s = MutableTTTState()
        val decision = DecisionTreeTTTAI.decide(TTTDecisionContext(iAmX = true, s))
        s.makePlacement(decision)
        s
    }
    TTTView(state)
}

fun main(): Unit = application {
    Window(onCloseRequest = ::exitApplication) {
        DevelopmentEntryPoint {
            DecisionTreeViewTest()
        }
    }
}
