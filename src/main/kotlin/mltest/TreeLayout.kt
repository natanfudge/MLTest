package mltest

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.SubcomposeLayout

//@Composable
//fun <T, L> TreeLayout(tree: LabeledEdgeTree<T, L>, node: @Composable (T) -> Unit) {
//    val layers = remember { tree.collectLayers() }.take(3)
//
//
//
//    SubcomposeLayout { constraints ->
//        val nextLayer = layers.last()
//        val nextLayerPlaceables = subcompose(layers.lastIndex) {
//            //TODO: need to separate by parent...
//            Row {
//                for (layerNode in nextLayer) {
//                    node(layerNode)
//                }
//            }
//        }.single().measure(constraints)
//
//        val prevLayer = layers[layers.lastIndex - 1]
//
//
//
//        layout(nextLayerPlaceables.width, nextLayerPlaceables.height) {
//            nextLayerPlaceables.place(0, 0)
//        }
//    }
//}
//
//private fun <T, L> LabeledEdgeTree<T, L>.mapNodesToParents(): Map<T, T> {
//    val map = mutableMapOf<T, T>()
//    visitChildren(depth = Int.MAX_VALUE)
//}