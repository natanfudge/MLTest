package mltest

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <T> MutableStateList(size: Int, init: (Int) -> T): MutableList<T> {
    val list = mutableStateListOf<T>()
    list.addAll(List(size, init))
    return list
}

inline fun Modifier.addIf(condition: Boolean, addition: Modifier.() -> Modifier): Modifier {
    return if (condition) addition(this) else this
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onPointerChangeEvent(
    type: PointerEventType,
    onEvent: (PointerInputChange) -> Unit,
): Modifier = onPointerEvent(type) {
    it.changes.forEach { change ->
        onEvent(change)
    }
}

data class Holder<T>(var value: T) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}