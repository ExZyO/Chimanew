// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class HomeShelfItem(
    val id: String,
    val title: String,
    val enabled: Boolean,
)

class HomeRowsCustomizerScreen : Screen() {

    companion object {
        const val SHELF_CONTINUE_READING = "continue_reading"
        const val SHELF_READING = "reading"
        const val SHELF_PLAN_TO_READ = "plan_to_read"
        const val SHELF_ON_HOLD = "on_hold"
        const val SHELF_COMPLETED = "completed"
        const val SHELF_DROPPED = "dropped"
        const val SHELF_NOT_TRACKED = "not_tracked"
        const val SHELF_CATEGORIES = "categories"
        const val SHELF_RECENTLY_ADDED = "recently_added"

        val ALL_SHELF_IDS = listOf(
            SHELF_CONTINUE_READING,
            SHELF_READING,
            SHELF_PLAN_TO_READ,
            SHELF_ON_HOLD,
            SHELF_COMPLETED,
            SHELF_DROPPED,
            SHELF_NOT_TRACKED,
            SHELF_CATEGORIES,
            SHELF_RECENTLY_ADDED,
        )

        fun parseOrder(raw: String): List<Pair<String, Boolean>> {
            if (raw.isBlank()) {
                return ALL_SHELF_IDS.map { it to (it != SHELF_DROPPED) }
            }
            val parsed = mutableListOf<Pair<String, Boolean>>()
            val seen = mutableSetOf<String>()
            for (token in raw.split(",")) {
                val parts = token.trim().split(":", limit = 2)
                if (parts.size != 2) continue
                val id = parts[0]
                val enabled = parts[1].toBooleanStrictOrNull() ?: true
                if (id in ALL_SHELF_IDS && id !in seen) {
                    parsed.add(id to enabled)
                    seen.add(id)
                }
            }
            for (id in ALL_SHELF_IDS) {
                if (id !in seen) {
                    parsed.add(id to (id != SHELF_DROPPED))
                }
            }
            return parsed
        }

        fun serializeOrder(items: List<Pair<String, Boolean>>): String {
            return items.joinToString(",") { "${it.first}:${it.second}" }
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }
        val orderPref = chimanewPreferences.homeShelfOrder()

        val titleMap = mapOf(
            SHELF_CONTINUE_READING to stringResource(MR.strings.pref_nuvio_shelf_continue_reading),
            SHELF_READING to stringResource(MR.strings.pref_nuvio_shelf_reading),
            SHELF_PLAN_TO_READ to stringResource(MR.strings.pref_nuvio_shelf_plan_to_read),
            SHELF_ON_HOLD to stringResource(MR.strings.pref_nuvio_shelf_on_hold),
            SHELF_COMPLETED to stringResource(MR.strings.pref_nuvio_shelf_completed),
            SHELF_DROPPED to stringResource(MR.strings.pref_nuvio_shelf_dropped),
            SHELF_NOT_TRACKED to stringResource(MR.strings.pref_nuvio_shelf_not_tracked),
            SHELF_CATEGORIES to stringResource(MR.strings.pref_nuvio_shelf_categories),
            SHELF_RECENTLY_ADDED to stringResource(MR.strings.pref_nuvio_shelf_recently_added),
        )

        val rawOrder = remember { orderPref.get() }
        val parsedList = remember(rawOrder) {
            parseOrder(rawOrder).map { (id, enabled) ->
                HomeShelfItem(
                    id = id,
                    title = titleMap[id] ?: id,
                    enabled = enabled,
                )
            }
        }

        val itemsState = remember { parsedList.toMutableStateList() }
        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            if (from.index in itemsState.indices && to.index in itemsState.indices) {
                itemsState.apply {
                    add(to.index, removeAt(from.index))
                }
            }
        }

        // Persist when dragging completes
        LaunchedEffect(reorderableState) {
            snapshotFlow { reorderableState.isAnyItemDragging }
                .drop(1)
                .collectLatest { isDragging ->
                    if (!isDragging) {
                        val serialized = serializeOrder(itemsState.map { it.id to it.enabled })
                        orderPref.set(serialized)
                    }
                }
        }

        fun toggleVisibility(index: Int) {
            if (index in itemsState.indices) {
                val current = itemsState[index]
                itemsState[index] = current.copy(enabled = !current.enabled)
                val serialized = serializeOrder(itemsState.map { it.id to it.enabled })
                orderPref.set(serialized)
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_nuvio_reorder_shelves),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding + PaddingValues(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                items(
                    items = itemsState,
                    key = { it.id },
                ) { item ->
                    ReorderableItem(
                        state = reorderableState,
                        key = item.id,
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = MaterialTheme.padding.small,
                                        end = MaterialTheme.padding.medium,
                                        top = MaterialTheme.padding.small,
                                        bottom = MaterialTheme.padding.small,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DragHandle,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(MaterialTheme.padding.medium)
                                        .draggableHandle(),
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = LocalContentColor.current.let {
                                            if (item.enabled) it else it.copy(alpha = 0.5f)
                                        },
                                        textDecoration = TextDecoration.LineThrough.takeIf { !item.enabled },
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val idx = itemsState.indexOfFirst { it.id == item.id }
                                        if (idx != -1) toggleVisibility(idx)
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (item.enabled) {
                                            Icons.Outlined.Visibility
                                        } else {
                                            Icons.Outlined.VisibilityOff
                                        },
                                        contentDescription = null,
                                        tint = if (item.enabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
