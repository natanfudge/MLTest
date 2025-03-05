package mltest

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier

inline fun <T> MutableStateList(size: Int, init: (Int) -> T): MutableList<T> {
    val list = mutableStateListOf<T>()
    list.addAll(List(size, init))
    return list
}

fun Modifier.addIf(condition: Boolean, addition: Modifier.() -> Modifier) = if(condition) addition(this) else this