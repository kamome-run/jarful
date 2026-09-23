package dev.jarful.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * Vertical drag-and-drop reordering for a LazyColumn (FR-1.6, FR-6.10). Attach [handle] to a drag
 * handle inside each row and [item] to the row itself; [onMove] is called with (from, to) indices in
 * [keys] every time the dragged row passes a neighbour, so it works with mouse and touch alike.
 */
class ReorderState(private val listState: LazyListState, private val keys: () -> List<Any>, private val onMove: (Int, Int) -> Unit) {
    var draggingKey by mutableStateOf<Any?>(null); private set
    var offset by mutableFloatStateOf(0f); private set

    fun start(key: Any) { draggingKey = key; offset = 0f }
    fun end() { draggingKey = null; offset = 0f }

    fun drag(deltaY: Float) {
        val key = draggingKey ?: return
        offset += deltaY
        val list = keys()
        val index = list.indexOf(key); if (index < 0) return
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.key == key }) return
        if (offset > 0 && index < list.lastIndex) {
            val next = visible.firstOrNull { it.key == list[index + 1] } ?: return
            if (offset > next.size / 2f) { onMove(index, index + 1); offset -= next.size }
        } else if (offset < 0 && index > 0) {
            val prev = visible.firstOrNull { it.key == list[index - 1] } ?: return
            if (-offset > prev.size / 2f) { onMove(index, index - 1); offset += prev.size }
        }
    }
}

@Composable
fun rememberReorderState(listState: LazyListState, keys: () -> List<Any>, onMove: (Int, Int) -> Unit): ReorderState =
    remember(listState) { ReorderState(listState, keys, onMove) }

/** Drag handle modifier: press and move to reorder. */
fun Modifier.reorderHandle(state: ReorderState, key: Any): Modifier = pointerInput(key) {
    detectDragGestures(
        onDragStart = { state.start(key) },
        onDragEnd = { state.end() },
        onDragCancel = { state.end() },
        onDrag = { change, drag -> change.consume(); state.drag(drag.y) },
    )
}

/** Row modifier: follows the finger while its row is being dragged and floats above the others. */
fun Modifier.reorderItem(state: ReorderState, key: Any): Modifier =
    if (state.draggingKey == key) this.zIndex(1f).graphicsLayer { translationY = state.offset; shadowElevation = 8f } else this
