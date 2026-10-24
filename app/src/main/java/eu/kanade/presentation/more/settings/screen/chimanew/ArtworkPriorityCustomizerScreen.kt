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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

data class ArtworkPriorityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean,
)

class ArtworkPriorityCustomizerScreen : Screen() {

    companion object {
        const val PROVIDER_ANILIST = "anilist"
        const val PROVIDER_KITSU = "kitsu"
        const val PROVIDER_MANGADEX = "mangadex"
        const val PROVIDER_NAVER = "naver"
        const val PROVIDER_KUAIKAN = "kuaikan"

        val ALL_PROVIDERS = listOf(
            PROVIDER_NAVER,
            PROVIDER_KUAIKAN,
            PROVIDER_MANGADEX,
            PROVIDER_ANILIST,
            PROVIDER_KITSU,
        )

        fun parseOrder(raw: String, allowed: List<String> = ALL_PROVIDERS): List<Pair<String, Boolean>> {
            if (raw.isBlank()) {
                return allowed.map { it to true }
            }
            val parsed = mutableListOf<Pair<String, Boolean>>()
            val seen = mutableSetOf<String>()

            for (token in raw.split(",")) {
                val trimmed = token.trim()
                if (trimmed.isBlank()) continue
                if (trimmed.contains(":")) {
                    val parts = trimmed.split(":", limit = 2)
                    val id = parts[0].lowercase()
                    val enabled = parts[1].toBooleanStrictOrNull() ?: true
                    if (id in allowed && id !in seen) {
                        parsed.add(id to enabled)
                        seen.add(id)
                    }
                } else {
                    val id = trimmed.lowercase()
                    if (id in allowed && id !in seen) {
                        parsed.add(id to true)
                        seen.add(id)
                    }
                }
            }

            for (provider in allowed) {
                if (provider !in seen) {
                    parsed.add(provider to true)
                    seen.add(provider)
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
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

        val providerTitles = mapOf(
            PROVIDER_NAVER to stringResource(MR.strings.pref_chimanew_artwork_provider_naver),
            PROVIDER_KUAIKAN to stringResource(MR.strings.pref_chimanew_artwork_provider_kuaikan),
            PROVIDER_ANILIST to stringResource(MR.strings.pref_chimanew_artwork_provider_anilist),
            PROVIDER_KITSU to stringResource(MR.strings.pref_chimanew_artwork_provider_kitsu),
            PROVIDER_MANGADEX to stringResource(MR.strings.pref_chimanew_artwork_provider_mangadex),
        )

        val providerSubtitles = mapOf(
            PROVIDER_NAVER to stringResource(MR.strings.pref_chimanew_artwork_provider_naver_desc),
            PROVIDER_KUAIKAN to stringResource(MR.strings.pref_chimanew_artwork_provider_kuaikan_desc),
            PROVIDER_ANILIST to stringResource(MR.strings.pref_chimanew_artwork_provider_anilist_desc),
            PROVIDER_KITSU to stringResource(MR.strings.pref_chimanew_artwork_provider_kitsu_desc),
            PROVIDER_MANGADEX to stringResource(MR.strings.pref_chimanew_artwork_provider_mangadex_desc),
        )

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_chimanew_artwork_priority_screen_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(MR.strings.pref_chimanew_artwork_tab_covers)) },
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(MR.strings.pref_chimanew_artwork_tab_banners)) },
                    )
                }

                key(selectedTabIndex) {
                    val orderPref = if (selectedTabIndex == 0) {
                        chimanewPreferences.artworkCoverOrder()
                    } else {
                        chimanewPreferences.artworkBannerOrder()
                    }
                    val allowed = if (selectedTabIndex == 0) {
                        listOf(PROVIDER_MANGADEX, PROVIDER_ANILIST, PROVIDER_NAVER, PROVIDER_KUAIKAN, PROVIDER_KITSU)
                    } else {
                        listOf(PROVIDER_NAVER, PROVIDER_KUAIKAN, PROVIDER_ANILIST, PROVIDER_KITSU)
                    }

                    val rawOrder = remember { orderPref.get() }
                    val itemsState = remember {
                        parseOrder(rawOrder, allowed).map { (id, enabled) ->
                            ArtworkPriorityItem(
                                id = id,
                                title = providerTitles[id] ?: id,
                                subtitle = providerSubtitles[id] ?: "",
                                enabled = enabled,
                            )
                        }.toMutableStateList()
                    }

                    val lazyListState = rememberLazyListState()
                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        if (from.index in itemsState.indices && to.index in itemsState.indices) {
                            itemsState.apply {
                                add(to.index, removeAt(from.index))
                            }
                        }
                    }

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

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(
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
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = LocalContentColor.current.let {
                                                    if (item.enabled) it else it.copy(alpha = 0.5f)
                                                },
                                                textDecoration = TextDecoration.LineThrough.takeIf { !item.enabled },
                                            )
                                            if (item.subtitle.isNotBlank()) {
                                                Text(
                                                    text = item.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.let {
                                                        if (item.enabled) it else it.copy(alpha = 0.5f)
                                                    },
                                                )
                                            }
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
    }
}
