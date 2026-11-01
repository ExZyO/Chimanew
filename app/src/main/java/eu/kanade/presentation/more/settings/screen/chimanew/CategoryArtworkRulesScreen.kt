// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.CategoryArtworkRule
import eu.kanade.tachiyomi.ui.home.chimanew.data.CategoryArtworkRulesConfig
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoryArtworkRulesScreen : Screen() {

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }
        val getCategories = remember { Injekt.get<GetCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val coverSources = listOf(
            "waterfall" to stringResource(MR.strings.pref_chimanew_category_artwork_source_waterfall),
            "mangadex" to stringResource(MR.strings.pref_chimanew_artwork_provider_mangadex),
            "anilist" to stringResource(MR.strings.pref_chimanew_artwork_provider_anilist),
            "bangumi" to stringResource(MR.strings.pref_chimanew_artwork_provider_bangumi),
            "bookwalker" to stringResource(MR.strings.pref_chimanew_artwork_provider_bookwalker),
            "naver" to stringResource(MR.strings.pref_chimanew_artwork_provider_naver),
            "kuaikan" to stringResource(MR.strings.pref_chimanew_artwork_provider_kuaikan),
            "kitsu" to stringResource(MR.strings.pref_chimanew_artwork_provider_kitsu),
            "default" to stringResource(MR.strings.pref_chimanew_category_artwork_source_default),
        )

        val bannerSources = listOf(
            "waterfall" to stringResource(MR.strings.pref_chimanew_category_artwork_source_waterfall),
            "anilist" to stringResource(MR.strings.pref_chimanew_artwork_provider_anilist),
            "bangumi" to stringResource(MR.strings.pref_chimanew_artwork_provider_bangumi),
            "naver" to stringResource(MR.strings.pref_chimanew_artwork_provider_naver),
            "kuaikan" to stringResource(MR.strings.pref_chimanew_artwork_provider_kuaikan),
            "kitsu" to stringResource(MR.strings.pref_chimanew_artwork_provider_kitsu),
            "none" to stringResource(MR.strings.pref_chimanew_category_artwork_source_none),
            "default" to stringResource(MR.strings.pref_chimanew_category_artwork_source_default),
        )

        val fallbackModes = listOf(
            "strict" to stringResource(MR.strings.pref_chimanew_category_artwork_fallback_strict),
            "waterfall" to stringResource(MR.strings.pref_chimanew_category_artwork_fallback_waterfall),
        )

        // Initial config parse
        val initialConfig = remember {
            val raw = chimanewPreferences.categoryArtworkRulesConfig().get()
            if (raw.isNotBlank()) {
                try {
                    jsonHelper.decodeFromString<CategoryArtworkRulesConfig>(raw)
                } catch (_: Exception) {
                    CategoryArtworkRulesConfig()
                }
            } else {
                CategoryArtworkRulesConfig()
            }
        }

        // State holders
        var defaultCoverSource by remember { mutableStateOf(initialConfig.defaultCoverSource) }
        var defaultBannerSource by remember { mutableStateOf(initialConfig.defaultBannerSource) }
        var defaultFallbackMode by remember { mutableStateOf(initialConfig.defaultFallbackMode) }

        val rulesMap = remember {
            mutableStateOf(initialConfig.rules.toMutableMap())
        }

        val priorityList = remember {
            val existing = initialConfig.priorityCategoryIds.filter { initialConfig.rules.containsKey(it.toString()) }
            val missing = initialConfig.rules.keys.mapNotNull { it.toLongOrNull() }.filter { it !in existing }
            (existing + missing).toMutableStateList()
        }

        fun saveConfig() {
            val config = CategoryArtworkRulesConfig(
                priorityCategoryIds = priorityList.toList(),
                rules = rulesMap.value.toMap(),
                defaultCoverSource = defaultCoverSource,
                defaultBannerSource = defaultBannerSource,
                defaultFallbackMode = defaultFallbackMode,
            )
            val json = jsonHelper.encodeToString(config)
            chimanewPreferences.categoryArtworkRulesConfig().set(json)
            chimanewPreferences.categoryArtworkRulesUpdated().set(System.currentTimeMillis())
        }

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            if (from.index in priorityList.indices && to.index in priorityList.indices) {
                priorityList.apply {
                    add(to.index, removeAt(from.index))
                }
            }
        }

        LaunchedEffect(reorderableState) {
            snapshotFlow { reorderableState.isAnyItemDragging }
                .drop(1)
                .collectLatest { isDragging ->
                    if (!isDragging) {
                        saveConfig()
                    }
                }
        }

        // Active selection dialog states
        var activeDialog by remember { mutableStateOf<RuleDialogState?>(null) }
        var showAddCategoryDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_rules_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                state = lazyListState,
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                // Header Hint
                item(key = "header_hint") {
                    Text(
                        text = stringResource(MR.strings.pref_chimanew_category_artwork_priority_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }

                // Reorderable Priority Categories
                items(
                    items = priorityList,
                    key = { it },
                ) { categoryId ->
                    val category = allCategories.firstOrNull { it.id == categoryId }
                    val categoryName = category?.name?.takeIf { it.isNotBlank() } ?: "Category #$categoryId"
                    val rule = rulesMap.value[categoryId.toString()] ?: CategoryArtworkRule(categoryId)

                    ReorderableItem(
                        state = reorderableState,
                        key = categoryId,
                    ) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DragHandle,
                                            contentDescription = "Drag to reorder priority",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .draggableHandle(),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            rulesMap.value = rulesMap.value.toMutableMap().apply {
                                                remove(categoryId.toString())
                                            }
                                            priorityList.remove(categoryId)
                                            saveConfig()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(MR.strings.pref_chimanew_category_artwork_remove_rule),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                // Rule Selectors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    RuleChipButton(
                                        label = stringResource(MR.strings.pref_chimanew_category_artwork_cover_source),
                                        value = coverSources.firstOrNull { it.first == rule.coverSource }?.second ?: rule.coverSource,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            activeDialog = RuleDialogState.Cover(categoryId, rule.coverSource)
                                        },
                                    )
                                    RuleChipButton(
                                        label = stringResource(MR.strings.pref_chimanew_category_artwork_banner_source),
                                        value = bannerSources.firstOrNull { it.first == rule.bannerSource }?.second ?: rule.bannerSource,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            activeDialog = RuleDialogState.Banner(categoryId, rule.bannerSource)
                                        },
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                RuleChipButton(
                                    label = stringResource(MR.strings.pref_chimanew_category_artwork_fallback_mode),
                                    value = fallbackModes.firstOrNull { it.first == rule.fallbackMode }?.second ?: rule.fallbackMode,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        activeDialog = RuleDialogState.Fallback(categoryId, rule.fallbackMode)
                                    },
                                )
                            }
                        }
                    }
                }

                // Add Category Button
                val unconfiguredCategories = allCategories.filter { it.id !in priorityList }
                if (unconfiguredCategories.isNotEmpty()) {
                    item(key = "add_category_button") {
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(MR.strings.pref_chimanew_category_artwork_add_rule))
                        }
                    }
                }

                // Default / All Other Categories Card
                item(key = "default_other_card") {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 24.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.pref_chimanew_category_artwork_all_other),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(MR.strings.pref_chimanew_category_artwork_all_other_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RuleChipButton(
                                    label = stringResource(MR.strings.pref_chimanew_category_artwork_cover_source),
                                    value = coverSources.firstOrNull { it.first == defaultCoverSource }?.second ?: defaultCoverSource,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        activeDialog = RuleDialogState.DefaultCover(defaultCoverSource)
                                    },
                                )
                                RuleChipButton(
                                    label = stringResource(MR.strings.pref_chimanew_category_artwork_banner_source),
                                    value = bannerSources.firstOrNull { it.first == defaultBannerSource }?.second ?: defaultBannerSource,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        activeDialog = RuleDialogState.DefaultBanner(defaultBannerSource)
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            RuleChipButton(
                                label = stringResource(MR.strings.pref_chimanew_category_artwork_fallback_mode),
                                value = fallbackModes.firstOrNull { it.first == defaultFallbackMode }?.second ?: defaultFallbackMode,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    activeDialog = RuleDialogState.DefaultFallback(defaultFallbackMode)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Dialog for adding unconfigured category
        if (showAddCategoryDialog) {
            val unconfigured = allCategories.filter { it.id !in priorityList }
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text(stringResource(MR.strings.pref_chimanew_category_artwork_add_rule)) },
                text = {
                    LazyColumn {
                        items(unconfigured) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        rulesMap.value = rulesMap.value.toMutableMap().apply {
                                            put(
                                                category.id.toString(),
                                                CategoryArtworkRule(categoryId = category.id),
                                            )
                                        }
                                        priorityList.add(category.id)
                                        saveConfig()
                                        showAddCategoryDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = category.name.ifBlank { "Category #${category.id}" },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        // Option Pickers
        when (val dialog = activeDialog) {
            is RuleDialogState.Cover -> {
                SelectionDialog(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_cover_source),
                    items = coverSources,
                    selected = dialog.current,
                    onSelect = { selected ->
                        val currentRule = rulesMap.value[dialog.categoryId.toString()] ?: CategoryArtworkRule(dialog.categoryId)
                        rulesMap.value = rulesMap.value.toMutableMap().apply {
                            put(dialog.categoryId.toString(), currentRule.copy(coverSource = selected))
                        }
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            is RuleDialogState.Banner -> {
                SelectionDialog(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_banner_source),
                    items = bannerSources,
                    selected = dialog.current,
                    onSelect = { selected ->
                        val currentRule = rulesMap.value[dialog.categoryId.toString()] ?: CategoryArtworkRule(dialog.categoryId)
                        rulesMap.value = rulesMap.value.toMutableMap().apply {
                            put(dialog.categoryId.toString(), currentRule.copy(bannerSource = selected))
                        }
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            is RuleDialogState.Fallback -> {
                SelectionDialog(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_fallback_mode),
                    items = fallbackModes,
                    selected = dialog.current,
                    onSelect = { selected ->
                        val currentRule = rulesMap.value[dialog.categoryId.toString()] ?: CategoryArtworkRule(dialog.categoryId)
                        rulesMap.value = rulesMap.value.toMutableMap().apply {
                            put(dialog.categoryId.toString(), currentRule.copy(fallbackMode = selected))
                        }
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            is RuleDialogState.DefaultCover -> {
                SelectionDialog(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_cover_source),
                    items = coverSources,
                    selected = dialog.current,
                    onSelect = { selected ->
                        defaultCoverSource = selected
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            is RuleDialogState.DefaultBanner -> {
                SelectionDialog(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_banner_source),
                    items = bannerSources,
                    selected = dialog.current,
                    onSelect = { selected ->
                        defaultBannerSource = selected
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            is RuleDialogState.DefaultFallback -> {
                SelectionDialog(
                    title = stringResource(MR.strings.pref_chimanew_category_artwork_fallback_mode),
                    items = fallbackModes,
                    selected = dialog.current,
                    onSelect = { selected ->
                        defaultFallbackMode = selected
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            null -> {}
        }
    }

    private sealed interface RuleDialogState {
        data class Cover(val categoryId: Long, val current: String) : RuleDialogState
        data class Banner(val categoryId: Long, val current: String) : RuleDialogState
        data class Fallback(val categoryId: Long, val current: String) : RuleDialogState
        data class DefaultCover(val current: String) : RuleDialogState
        data class DefaultBanner(val current: String) : RuleDialogState
        data class DefaultFallback(val current: String) : RuleDialogState
    }

    @Composable
    private fun RuleChipButton(
        label: String,
        value: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    @Composable
    private fun <T> SelectionDialog(
        title: String,
        items: List<Pair<T, String>>,
        selected: T,
        onSelect: (T) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                LazyColumn {
                    items(items) { (value, label) ->
                        val isSelected = value == selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        onSelect(value)
                                        onDismissRequest()
                                    },
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}
