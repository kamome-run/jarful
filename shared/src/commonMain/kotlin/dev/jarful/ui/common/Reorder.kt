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
 * Vertical drag-and-drop reordering for a LazyColumn (FR-1.6, FR-6.10). Attach [reorderHandle] to a
 * drag handle inside each row and [reorderItem] to the row itself. [keys] returns the current order of
 * the reorderable keys (other items such as headings may sit between them); [onMove] is called with
 * (from, to) indices in that list whenever the dragged row's centre passes a neighbour's centre.
 * Both lambdas are refreshed on every composition so they never see a stale list.
 */
class ReorderState(private val listState: LazyListState) {
    var keys: () -> List<Any> = { emptyList() }
    var onMove: (Int, Int) -> Unit = { _, _ -> }

    var draggingKey by mutableStateOf<Any?>(null); private set
    /** Visual offset of the dragged row relative to its current slot. */
    var offset by mutableFloatStateOf(0f); private set
    /** (lazy index, layout offset) of the dragged row when the last move was requested; cleared once the layout caught up. */
    private var pending: Pair<Int, Int>? = null

    fun start(key: Any) { draggingKey = key; offset = 0f; pending = null }
    fun end() { draggingKey = null; offset = 0f; pending = null }

    fun drag(deltaY: Float) {
        val key = draggingKey ?: return
        offset += deltaY
        val visible = listState.layoutInfo.visibleItemsInfo
        val me = visible.firstOrNull { it.key == key } ?: return
        // After a move, wait until the LazyColumn has re-laid the dragged row before judging the next crossing,
        // otherwise several drag events in one frame would each request a move (the "jumps two rows" bug).
        pending?.let { (i, o) -> if (me.index == i && me.offset == o) return else pending = null }
        val list = keys()
        val index = list.indexOf(key); if (index < 0) return
        val center = me.offset + offset + me.size / 2f
        if (offset > 0 && index < list.lastIndex) {
            val next = visible.firstOrNull { it.key == list[index + 1] } ?: return
            if (center > next.offset + next.size / 2f) {
                pending = me.index to me.offset
                offset -= (next.offset + next.size - me.size) - me.offset // new slot starts where "next" ends
                onMove(index, index + 1)
            }
        } else if (offset < 0 && index > 0) {
            val prev = visible.firstOrNull { it.key == list[index - 1] } ?: return
            if (center < prev.offset + prev.size / 2f) {
                pending = me.index to me.offset
                offset += me.offset - prev.offset // new slot starts where "prev" started
                onMove(index, index - 1)
            }
        }
    }
}

@Composable
fun rememberReorderState(listState: LazyListState, keys: () -> List<Any>, onMove: (Int, Int) -> Unit): ReorderState {
    val state = remember(listState) { ReorderState(listState) }
    state.keys = keys
    state.onMove = onMove
    return state
}

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
