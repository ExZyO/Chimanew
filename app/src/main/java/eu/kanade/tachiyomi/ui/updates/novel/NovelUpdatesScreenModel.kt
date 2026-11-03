package eu.kanade.tachiyomi.ui.updates.novel

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.updates.novel.model.NovelUpdatesWithRelations
import tachiyomi.domain.updates.novel.repository.NovelUpdatesRepository
import tachiyomi.domain.updates.service.UpdatesPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

class NovelUpdatesScreenModel(
    private val novelUpdatesRepository: NovelUpdatesRepository = Injekt.get(),
    private val updatesPreferences: UpdatesPreferences = Injekt.get(),
) : StateScreenModel<NovelUpdatesScreenModel.State>(State()) {

    init {
        load()
        updatesPreferences.consolidateUpdates().changes()
            .distinctUntilChanged()
            .onEach { consolidate ->
                mutableState.update { it.copy(consolidateUpdates = consolidate) }
            }
            .launchIn(screenModelScope)
    }

    fun load() {
        screenModelScope.launch {
            val after = ZonedDateTime.now().minusMonths(3).toInstant().toEpochMilli()
            runCatching {
                novelUpdatesRepository.awaitUpdates(after = after, limit = 250000L)
            }.onSuccess { updates ->
                val novelCount = updates.groupBy { it.novelId }.size
                ChimanewDiagnostics.log(DiagnosticCategory.UPDATES, "NovelUpdates") {
                    "Loaded ${updates.size} raw novel update rows from DB (uncapped, limit=250,000) across $novelCount unique novels."
                }
                mutableState.update {
                    it.copy(isLoading = false, items = updates)
                }
            }
        }
    }

    fun toggleExpandedState(key: String) {
        mutableState.update {
            it.copy(
                expandedState = it.expandedState.toMutableSet().apply {
                    if (it.expandedState.contains(key)) remove(key) else add(key)
                },
            )
        }
    }

    fun setSearchQuery(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<NovelUpdatesWithRelations> = emptyList(),
        val searchQuery: String? = null,
        val expandedState: Set<String> = emptySet(),
        val consolidateUpdates: Boolean = true,
    ) {
        val filteredItems: List<NovelUpdatesWithRelations>
            get() {
                val q = searchQuery?.trim()?.lowercase()
                return if (q.isNullOrEmpty()) {
                    items
                } else {
                    items.filter {
                        it.novelTitle.lowercase().contains(q) ||
                            it.chapterName.lowercase().contains(q)
                    }
                }
            }
    }
}

fun NovelUpdatesWithRelations.groupByDateAndNovel() = "${dateFetch / 86_400_000L}-$novelId"
