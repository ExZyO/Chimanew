// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
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
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private val categoryArtworkRulesJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

class CategoryArtworkRulesScreen : Screen() {

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }
        val getCategories = remember { Injekt.get<GetCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val allCoverProviders = listOf(
            "MANGADEX" to stringResource(MR.strings.pref_chimanew_artwork_provider_mangadex),
            "ANILIST" to stringResource(MR.strings.pref_chimanew_artwork_provider_anilist),
            "BANGUMI" to stringResource(MR.strings.pref_chimanew_artwork_provider_bangumi),
            "BOOKWALKER" to stringResource(MR.strings.pref_chimanew_artwork_provider_bookwalker),
            "NAVER" to stringResource(MR.strings.pref_chimanew_artwork_provider_naver),
            "KUAIKAN" to stringResource(MR.strings.pref_chimanew_artwork_provider_kuaikan),
            "KITSU" to stringResource(MR.strings.pref_chimanew_artwork_provider_kitsu),
            "DEFAULT" to stringResource(MR.strings.pref_chimanew_category_artwork_source_default),
        )

        val allBannerProviders = listOf(
            "ANILIST" to stringResource(MR.strings.pref_chimanew_artwork_provider_anilist),
            "BANGUMI" to stringResource(MR.strings.pref_chimanew_artwork_provider_bangumi),
            "NAVER" to stringResource(MR.strings.pref_chimanew_artwork_provider_naver),
            "KUAIKAN" to stringResource(MR.strings.pref_chimanew_artwork_provider_kuaikan),
            "KITSU" to stringResource(MR.strings.pref_chimanew_artwork_provider_kitsu),
        )

        val coverProviderLabels = remember(allCoverProviders) { allCoverProviders.toMap() }
        val bannerProviderLabels = remember(allBannerProviders) { allBannerProviders.toMap() }

        // Initial config parse
        val initialConfig = remember {
            val raw = chimanewPreferences.categoryArtworkRulesConfig().get()
            if (raw.isNotBlank()) {
                try {
                    categoryArtworkRulesJson.decodeFromString<CategoryArtworkRulesConfig>(raw)
                } catch (_: Exception) {
                    CategoryArtworkRulesConfig()
                }
            } else {
                CategoryArtworkRulesConfig()
            }
        }

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
            )
            val json = categoryArtworkRulesJson.encodeToString(config)
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
            floatingActionButton = {
                val unconfiguredCategories = allCategories.filter { it.id !in priorityList }
                if (unconfiguredCategories.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddCategoryDialog = true },
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text(stringResource(MR.strings.pref_chimanew_category_artwork_add_rule)) },
                    )
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                // Header Explanations (outside LazyColumn so reordering indices are 100% 1:1)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(MR.strings.pref_chimanew_category_artwork_rules_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(MR.strings.pref_chimanew_category_artwork_priority_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(MR.strings.pref_chimanew_category_artwork_global_inheritance_hint),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                if (priorityList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(MR.strings.pref_chimanew_category_artwork_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(MR.strings.pref_chimanew_category_artwork_empty_description),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            start = MaterialTheme.padding.medium,
                            end = MaterialTheme.padding.medium,
                            top = MaterialTheme.padding.extraSmall,
                            bottom = 80.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    ) {
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

                                        // Cover Sources Row
                                        RuleSummaryButton(
                                            title = stringResource(MR.strings.pref_chimanew_category_artwork_covers_label),
                                            sources = rule.effectiveCoverSources(),
                                            providerLabels = coverProviderLabels,
                                            fallback = rule.effectiveCoverFallback(),
                                            isCover = true,
                                            onClick = {
                                                activeDialog = RuleDialogState.Covers(categoryId, categoryName, rule)
                                            },
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Banner Sources Row
                                        RuleSummaryButton(
                                            title = stringResource(MR.strings.pref_chimanew_category_artwork_banners_label),
                                            sources = rule.effectiveBannerSources(),
                                            providerLabels = bannerProviderLabels,
                                            fallback = rule.effectiveBannerFallback(),
                                            isCover = false,
                                            onClick = {
                                                activeDialog = RuleDialogState.Banners(categoryId, categoryName, rule)
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

        // Multi-source Priority & Fallback Dialogs
        when (val dialog = activeDialog) {
            is RuleDialogState.Covers -> {
                SourceOrderAndFallbackDialog(
                    title = "${dialog.categoryName} Covers",
                    allProviders = allCoverProviders,
                    initialSources = dialog.rule.effectiveCoverSources(),
                    initialFallback = dialog.rule.effectiveCoverFallback(),
                    isCover = true,
                    onSave = { updatedSources, updatedFallback ->
                        val currentRule = rulesMap.value[dialog.categoryId.toString()] ?: CategoryArtworkRule(dialog.categoryId)
                        rulesMap.value = rulesMap.value.toMutableMap().apply {
                            put(
                                dialog.categoryId.toString(),
                                currentRule.copy(
                                    coverSources = updatedSources,
                                    coverFallback = updatedFallback,
                                ),
                            )
                        }
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            is RuleDialogState.Banners -> {
                SourceOrderAndFallbackDialog(
                    title = "${dialog.categoryName} Banners",
                    allProviders = allBannerProviders,
                    initialSources = dialog.rule.effectiveBannerSources(),
                    initialFallback = dialog.rule.effectiveBannerFallback(),
                    isCover = false,
                    onSave = { updatedSources, updatedFallback ->
                        val currentRule = rulesMap.value[dialog.categoryId.toString()] ?: CategoryArtworkRule(dialog.categoryId)
                        rulesMap.value = rulesMap.value.toMutableMap().apply {
                            put(
                                dialog.categoryId.toString(),
                                currentRule.copy(
                                    bannerSources = updatedSources,
                                    bannerFallback = updatedFallback,
                                ),
                            )
                        }
                        saveConfig()
                    },
                    onDismissRequest = { activeDialog = null },
                )
            }
            null -> {}
        }
    }

    private sealed interface RuleDialogState {
        data class Covers(val categoryId: Long, val categoryName: String, val rule: CategoryArtworkRule) : RuleDialogState
        data class Banners(val categoryId: Long, val categoryName: String, val rule: CategoryArtworkRule) : RuleDialogState
    }

    @Composable
    private fun RuleSummaryButton(
        title: String,
        sources: List<String>,
        providerLabels: Map<String, String>,
        fallback: String,
        isCover: Boolean,
        onClick: () -> Unit,
    ) {
        val fallbackText = when {
            fallback.equals(CategoryArtworkRule.FALLBACK_WATERFALL, ignoreCase = true) ->
                stringResource(MR.strings.pref_chimanew_category_artwork_source_waterfall)
            isCover ->
                stringResource(MR.strings.pref_chimanew_category_artwork_source_default)
            else ->
                stringResource(MR.strings.pref_chimanew_category_artwork_source_none)
        }

        val sourcesText = if (sources.isEmpty()) {
            stringResource(MR.strings.pref_chimanew_category_artwork_no_active_sources)
        } else {
            sources.joinToString(" → ") { providerLabels[it.uppercase()] ?: it }
        }

        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "Fallback: $fallbackText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourcesText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (sources.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun SourceOrderAndFallbackDialog(
        title: String,
        allProviders: List<Pair<String, String>>,
        initialSources: List<String>,
        initialFallback: String,
        isCover: Boolean,
        onSave: (List<String>, String) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        val currentSources = remember { initialSources.toMutableStateList() }
        var currentFallback by remember { mutableStateOf(initialFallback) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(MR.strings.pref_chimanew_category_artwork_active_sources),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentSources.isEmpty()) {
                        Text(
                            text = stringResource(MR.strings.pref_chimanew_category_artwork_no_active_sources),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    } else {
                        currentSources.forEachIndexed { index, sourceKey ->
                            val label = allProviders.firstOrNull { it.first.equals(sourceKey, ignoreCase = true) }?.second ?: sourceKey
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(28.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val item = currentSources.removeAt(index)
                                                currentSources.add(index - 1, item)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < currentSources.lastIndex) {
                                                val item = currentSources.removeAt(index)
                                                currentSources.add(index + 1, item)
                                            }
                                        },
                                        enabled = index < currentSources.lastIndex,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            currentSources.removeAt(index)
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val unaddedProviders = allProviders.filter { prov ->
                        !currentSources.any { it.equals(prov.first, ignoreCase = true) }
                    }
                    if (unaddedProviders.isNotEmpty()) {
                        Text(
                            text = stringResource(MR.strings.pref_chimanew_category_artwork_add_source),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            unaddedProviders.forEach { (code, name) ->
                                SuggestionChip(
                                    onClick = { currentSources.add(code) },
                                    label = { Text("+ $name") },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = stringResource(MR.strings.pref_chimanew_category_artwork_fallback_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val strictValue = if (isCover) CategoryArtworkRule.FALLBACK_DEFAULT else CategoryArtworkRule.FALLBACK_NONE
                    val strictLabel = if (isCover) {
                        stringResource(MR.strings.pref_chimanew_category_artwork_fallback_cover_default)
                    } else {
                        stringResource(MR.strings.pref_chimanew_category_artwork_fallback_banner_none)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { currentFallback = strictValue }
                            .padding(vertical = 6.dp),
                    ) {
                        RadioButton(
                            selected = currentFallback.equals(strictValue, ignoreCase = true),
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strictLabel,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    val waterfallLabel = stringResource(MR.strings.pref_chimanew_category_artwork_fallback_cascade)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { currentFallback = CategoryArtworkRule.FALLBACK_WATERFALL }
                            .padding(vertical = 6.dp),
                    ) {
                        RadioButton(
                            selected = currentFallback.equals(CategoryArtworkRule.FALLBACK_WATERFALL, ignoreCase = true),
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = waterfallLabel,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(currentSources.toList(), currentFallback)
                        onDismissRequest()
                    },
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}
