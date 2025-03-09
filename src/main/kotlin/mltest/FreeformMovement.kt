package mltest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints

class FreeformMovementState(transform: TransformationMatrix2D) {
    var transform by mutableStateOf(transform)
    fun scale(factor: Float, focalPoint: Offset) {
        transform = transform.scale(factor, focalPoint)
    }

    fun translate(offset: Offset) {
        transform = transform.translate(offset)
    }
}

@Composable
fun rememberFreeformMovementState() = remember { FreeformMovementState(TransformationMatrix2D()) }

val LocalFreeformMovementState = staticCompositionLocalOf<FreeformMovementState> { error("") }

/**
 * Allows panning and zooming in this component.
 */
@Composable
fun FreeformMovement(content: @Composable () -> Unit) {
    val transform = rememberFreeformMovementState()
    // We don't need UI to recompose on prevMousePos change to we use a Holder instead of a State
    var prevMousePos by remember<Holder<Offset?>> { Holder(null) }

    CompositionLocalProvider(LocalFreeformMovementState provides transform) {
        //TODO: replace Box with Layout
        Box(
            Modifier.fillMaxSize()
                .onPointerChangeEvent(PointerEventType.Press) {
                    // Start panning
                    prevMousePos = it.position
                }
                .onPointerChangeEvent(PointerEventType.Release) {
                    // Stop panning
                    prevMousePos = null
                }
                .onPointerChangeEvent(PointerEventType.Move) { change ->
                    val prevMousePosValue = prevMousePos
                    if (prevMousePosValue != null) {
                        val delta = change.position - prevMousePosValue
                        transform.translate(delta)
                        prevMousePos = change.position
                    }
                }
                .onPointerChangeEvent(PointerEventType.Scroll) { change ->
                    // Scale relative to scroll delta y
                    transform.scale(1 + change.scrollDelta.y * -ZoomSpeed, focalPoint = change.position)
                }.movementTransform { transform.transform }
        ) {
            content()
        }
    }

}

private const val ZoomSpeed = 0.3f

private fun Modifier.movementTransform(transform: () -> TransformationMatrix2D): Modifier {
    return (this then InfiniteConstraintsElement).graphicsLayer {
        val transformation = transform()

        // This is required for basic transformation to work properly
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = transformation.scale
        scaleY = transformation.scale
        translationX = transformation.translateX
        translationY = transformation.translateY
    }
}

/**
 * Makes the content render as if it has infinite space, useful for freeform movement.
 */
private data object InfiniteConstraintsElement : ModifierNodeElement<InfiniteConstraintsNode>() {
    override fun create() = InfiniteConstraintsNode()

    override fun update(node: InfiniteConstraintsNode) {
    }
}


private class InfiniteConstraintsNode : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Make it so the content will draw as if it has infinite space
        val placeable = measurable.measure(
            Constraints(
                minWidth = 0,
                minHeight = 0,
                maxWidth = Constraints.Infinity,
                maxHeight = Constraints.Infinity
            )
        )
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, 0)
        }
    }
}