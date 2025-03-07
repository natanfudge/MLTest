package mltest

import androidx.compose.ui.geometry.Offset

data class TransformationMatrix2D(
    val scale: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
) {
    fun scale(scale: Float): TransformationMatrix2D {
        return TransformationMatrix2D(
            scale = this.scale * scale,
            translateX = this.translateX * scale,
            translateY = this.translateY * scale
        )
    }

    /**
     * Scales the matrix around a focal point.
     */
    fun scale(scale: Float, focalPoint: Offset): TransformationMatrix2D {
        return translate(-focalPoint.x, -focalPoint.y)
            .scale(scale)
            .translate(focalPoint.x, focalPoint.y)
    }

    fun translate(offset: Offset): TransformationMatrix2D {
        return translate(offset.x, offset.y)
    }

    fun translate(x: Float, y: Float): TransformationMatrix2D {
        return TransformationMatrix2D(
            scale = this.scale,
            translateX = this.translateX + x,
            translateY = this.translateY + y
        )
    }
}