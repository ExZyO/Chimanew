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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
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
        const val CATEGORY_PREFIX = "category_"

        val STANDARD_SHELF_IDS = listOf(
            SHELF_CONTINUE_READING,
            SHELF_READING,
            SHELF_PLAN_TO_READ,
            SHELF_ON_HOLD,
            SHELF_COMPLETED,
            SHELF_DROPPED,
            SHELF_NOT_TRACKED,
            SHELF_RECENTLY_ADDED,
        )

        // Backward compatibility
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

        fun parseOrder(raw: String, categories: List<Category> = emptyList()): List<Pair<String, Boolean>> {
            val categoryIds = categories.map { "$CATEGORY_PREFIX${it.id}" }.toSet()
            val allValidIds = (STANDARD_SHELF_IDS + categoryIds + SHELF_CATEGORIES).toSet()

            if (raw.isBlank()) {
                val defaultList = mutableListOf<Pair<String, Boolean>>()
                defaultList.add(SHELF_CONTINUE_READING to true)
                defaultList.add(SHELF_READING to true)
                defaultList.add(SHELF_PLAN_TO_READ to true)
                defaultList.add(SHELF_ON_HOLD to true)
                defaultList.add(SHELF_COMPLETED to true)
                defaultList.add(SHELF_DROPPED to false)
                defaultList.add(SHELF_NOT_TRACKED to true)
                if (categories.isNotEmpty()) {
                    for (cat in categories) {
                        defaultList.add("$CATEGORY_PREFIX${cat.id}" to true)
                    }
                } else {
                    defaultList.add(SHELF_CATEGORIES to true)
                }
                defaultList.add(SHELF_RECENTLY_ADDED to true)
                return defaultList
            }

            val parsed = mutableListOf<Pair<String, Boolean>>()
            val seen = mutableSetOf<String>()

            for (token in raw.split(",")) {
                val parts = token.trim().split(":", limit = 2)
                if (parts.size != 2) continue
                val id = parts[0]
                val enabled = parts[1].toBooleanStrictOrNull() ?: true

                if (id == SHELF_CATEGORIES) {
                    if (categories.isNotEmpty()) {
                        for (cat in categories) {
                            val catShelfId = "$CATEGORY_PREFIX${cat.id}"
                            if (catShelfId !in seen) {
                                parsed.add(catShelfId to enabled)
                                seen.add(catShelfId)
                            }
                        }
                    } else if (id !in seen) {
                        parsed.add(id to enabled)
                        seen.add(id)
                    }
                } else if (id in allValidIds && id !in seen) {
                    parsed.add(id to enabled)
                    seen.add(id)
                } else if (categories.isEmpty() && id.startsWith(CATEGORY_PREFIX) && id !in seen) {
                    // Guard against dropping custom categories if categories list hasn't finished loading
                    parsed.add(id to enabled)
                    seen.add(id)
                }
            }

            for (id in STANDARD_SHELF_IDS) {
                if (id !in seen) {
                    parsed.add(id to (id != SHELF_DROPPED))
                    seen.add(id)
                }
            }

            for (cat in categories) {
                val catShelfId = "$CATEGORY_PREFIX${cat.id}"
                if (catShelfId !in seen) {
                    parsed.add(catShelfId to true)
                    seen.add(catShelfId)
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
        val getCategories = remember { Injekt.get<GetCategories>() }
        val categoriesNullable by getCategories.subscribe().collectAsState(initial = null)
        val orderPref = chimanewPreferences.homeShelfOrder()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_nuvio_reorder_shelves),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            val categories = categoriesNullable
            if (categories == null) {
                LoadingScreen(modifier = Modifier.padding(contentPadding))
            } else {
                CustomizerListContent(
                    categories = categories,
                    orderPref = orderPref,
                    contentPadding = contentPadding,
                )
            }
        }
    }

    @Composable
    private fun CustomizerListContent(
        categories: List<Category>,
        orderPref: Preference<String>,
        contentPadding: PaddingValues,
    ) {
        val standardTitles = mapOf(
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
        val categoryPrefixStr = stringResource(MR.strings.pref_chimanew_shelf_category_prefix)

        val categoryTitleMap = remember(categories) {
            categories.associate { cat ->
                "$CATEGORY_PREFIX${cat.id}" to categoryPrefixStr.replace("%s", cat.name.ifBlank { "Default" })
            }
        }

        val itemsState = remember {
            val rawOrder = orderPref.get()
            parseOrder(rawOrder, categories).map { (id, enabled) ->
                HomeShelfItem(
                    id = id,
                    title = standardTitles[id] ?: categoryTitleMap[id] ?: id,
                    enabled = enabled,
                )
            }.toMutableStateList()
        }

        fun saveOrder() {
            val serialized = serializeOrder(itemsState.map { it.id to it.enabled })
            orderPref.set(serialized)
        }

        DisposableEffect(Unit) {
            onDispose {
                saveOrder()
            }
        }

        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            if (from.index in itemsState.indices && to.index in itemsState.indices) {
                val item = itemsState.removeAt(from.index)
                itemsState.add(to.index, item)
                saveOrder()
            }
        }

        fun toggleVisibility(index: Int) {
            if (index in itemsState.indices) {
                val current = itemsState[index]
                itemsState[index] = current.copy(enabled = !current.enabled)
                saveOrder()
            }
        }

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
