package eu.kanade.tachiyomi.ui.updates

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastFilter
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.lang.toLocalDate
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration.Companion.milliseconds
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.service.UpdatesPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

class UpdatesScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val setReadStatus: SetReadStatus = Injekt.get(),
    private val getUpdates: GetUpdates = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val getChapter: GetChapter = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val updatesPreferences: UpdatesPreferences = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    // SY -->
    readerPreferences: ReaderPreferences = Injekt.get(),
    // SY <--
) : StateScreenModel<UpdatesScreenModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val lastUpdated by libraryPreferences.lastUpdatedTimestamp().asState(screenModelScope)

    // SY -->
    val preserveReadingPosition by readerPreferences.preserveReadingPosition().asState(screenModelScope)
    // SY <--

    // First and last selected index in list
    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedChapterIds: HashSet<Long> = HashSet()

    init {
        screenModelScope.launchIO {
            // Set date limit for recent chapters
            val limit = ZonedDateTime.now().minusMonths(3).toInstant()

            combine(
                // needed for SQL filters (unread, started, bookmarked, etc)
                getUpdatesItemPreferenceFlow()
                    .distinctUntilChanged()
                    .flatMapLatest {
                        getUpdates.subscribe(
                            limit,
                            unread = it.filterUnread.toBooleanOrNull(),
                            started = it.filterStarted.toBooleanOrNull(),
                            bookmarked = it.filterBookmarked.toBooleanOrNull(),
                            hideExcludedScanlators = it.filterExcludedScanlators,
                        ).distinctUntilChanged()
                    },
                downloadCache.changes,
                downloadManager.queueState,
                // needed for Kotlin filters (downloaded)
                getUpdatesItemPreferenceFlow().distinctUntilChanged { old, new ->
                    old.filterDownloaded == new.filterDownloaded
                },
            ) { updates, _, _, itemPreferences ->
                updates
                    .toUpdateItems()
                    .applyFilters(itemPreferences)
                    .toPersistentList()
            }
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(Event.InternalError)
                }
                .collectLatest { updateItems ->
                    val seriesCount = updateItems.groupBy { it.update.mangaId }.size
                    ChimanewDiagnostics.log(DiagnosticCategory.UPDATES, "MangaUpdates") {
                        "Loaded ${updateItems.size} raw chapter update rows from DB (uncapped, limit=250,000) across $seriesCount unique manga series."
                    }
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = updateItems
                                // KMK -->
                                .groupBy { it.update.dateFetch.toLocalDate() }
                                .flatMap { (_, mangas) ->
                                    mangas.groupBy { it.update.mangaId }
                                        .values
                                        .flatMap { mangaChapters ->
                                            val (unread, read) = mangaChapters.partition { !it.update.read }
                                            unread.sortedBy { it.update.dateFetch } +
                                                read.sortedByDescending { it.update.dateFetch }
                                        }
                                }
                                .toPersistentList(),
                            // KMK <--
                        )
                    }
                }
        }

        screenModelScope.launchIO {
            merge(
                downloadManager.statusFlow(),
                // Each progress tick rebuilds every row of the list, so cap how often that happens
                downloadManager.progressFlow().sample(200.milliseconds),
            )
                .catch { logcat(LogPriority.ERROR, it) }
                .collect(this@UpdatesScreenModel::updateDownloadState)
        }

        getUpdatesItemPreferenceFlow()
            .map { prefs ->
                listOf(
                    prefs.filterUnread,
                    prefs.filterDownloaded,
                    prefs.filterStarted,
                    prefs.filterBookmarked,
                )
                    .any { it != TriState.DISABLED }
            }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(screenModelScope)

        updatesPreferences.consolidateUpdates().changes()
            .distinctUntilChanged()
            .onEach { consolidate ->
                mutableState.update { it.copy(consolidateUpdates = consolidate) }
            }
            .launchIn(screenModelScope)
    }

    private fun List<UpdatesItem>.applyFilters(
        preferences: ItemPreferences,
    ): List<UpdatesItem> {
        val filterDownloaded = preferences.filterDownloaded

        val filterFnDownloaded: (UpdatesItem) -> Boolean = {
            applyFilter(filterDownloaded) {
                it.downloadStateProvider() == Download.State.DOWNLOADED
            }
        }

        return fastFilter {
            filterFnDownloaded(it)
        }
    }

    private fun List<UpdatesWithRelations>.toUpdateItems(): List<UpdatesItem> {
        return this
            .map { update ->
                val activeDownload = downloadManager.getQueuedDownloadOrNull(update.chapterId)
                val downloaded = downloadManager.isChapterDownloaded(
                    update.chapterName,
                    update.scanlator,
                    update.chapterUrl,
                    // SY -->
                    update.ogMangaTitle,
                    // SY <--
                    update.sourceId,
                )
                val downloadState = when {
                    activeDownload != null -> activeDownload.status
                    downloaded -> Download.State.DOWNLOADED
                    else -> Download.State.NOT_DOWNLOADED
                }
                UpdatesItem(
                    update = update,
                    downloadStateProvider = { downloadState },
                    downloadProgressProvider = { activeDownload?.progress ?: 0 },
                    selected = update.chapterId in selectedChapterIds,
                )
            }
    }

    fun updateLibrary(): Boolean {
        val started = LibraryUpdateJob.startNow(Injekt.get<Application>())
        screenModelScope.launch {
            _events.send(Event.LibraryUpdateTriggered(started))
        }
        return started
    }

    /**
     * Update status of chapters.
     *
     * @param download download object containing progress.
     */
    private fun updateDownloadState(download: Download) {
        mutableState.update { state ->
            val newItems = state.items.mutate { list ->
                val modifiedIndex = list.indexOfFirst { it.update.chapterId == download.chapter.id }
                if (modifiedIndex < 0) return@mutate

                val item = list[modifiedIndex]
                list[modifiedIndex] = item.copy(
                    downloadStateProvider = { download.status },
                    downloadProgressProvider = { download.progress },
                )
            }
            state.copy(items = newItems)
        }
    }

    fun downloadChapters(items: List<UpdatesItem>, action: ChapterDownloadAction) {
        if (items.isEmpty()) return
        ChimanewDiagnostics.log(DiagnosticCategory.UPDATES, "MangaUpdates") {
            "Download triggered (action=$action) for ${items.size} chapters of '${items.firstOrNull()?.update?.mangaTitle}'."
        }
        screenModelScope.launch {
            when (action) {
                ChapterDownloadAction.START -> {
                    downloadChapters(items)
                    if (items.any { it.downloadStateProvider() == Download.State.ERROR }) {
                        downloadManager.startDownloads()
                    }
                }
                ChapterDownloadAction.START_NOW -> {
                    val chapterId = items.singleOrNull()?.update?.chapterId ?: return@launch
                    startDownloadingNow(chapterId)
                }
                ChapterDownloadAction.CANCEL -> {
                    val chapterId = items.singleOrNull()?.update?.chapterId ?: return@launch
                    cancelDownload(chapterId)
                }
                ChapterDownloadAction.DELETE -> {
                    deleteChapters(items)
                }
                ChapterDownloadAction.OCR,
                ChapterDownloadAction.DELETE_OCR,
                -> {
                    // OCR actions are not applicable in updates screen yet
                }
            }
            toggleAllSelection(false)
        }
    }

    private fun startDownloadingNow(chapterId: Long) {
        downloadManager.startDownloadNow(chapterId)
    }

    private fun cancelDownload(chapterId: Long) {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return
        downloadManager.cancelQueuedDownloads(listOf(activeDownload))
        updateDownloadState(activeDownload.apply { status = Download.State.NOT_DOWNLOADED })
    }

    /**
     * Mark the selected updates list as read/unread.
     * @param updates the list of selected updates.
     * @param read whether to mark chapters as read or unread.
     */
    fun markUpdatesRead(updates: List<UpdatesItem>, read: Boolean) {
        screenModelScope.launchIO {
            setReadStatus.await(
                read = read,
                chapters = updates
                    .mapNotNull { getChapter.await(it.update.chapterId) }
                    .toTypedArray(),
            )
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param updates the list of chapters to bookmark.
     */
    fun bookmarkUpdates(updates: List<UpdatesItem>, bookmark: Boolean) {
        screenModelScope.launchIO {
            updates
                .filterNot { it.update.bookmark == bookmark }
                .map { ChapterUpdate(id = it.update.chapterId, bookmark = bookmark) }
                .let { updateChapter.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param updatesItem the list of chapters to download.
     */
    private fun downloadChapters(updatesItem: List<UpdatesItem>) {
        screenModelScope.launchNonCancellable {
            val batch = updatesItem.groupBy { it.update.mangaId }.values.mapNotNull { updates ->
                val mangaId = updates.first().update.mangaId
                val manga = getManga.await(mangaId) ?: return@mapNotNull null
                // Don't download if source isn't available
                sourceManager.get(manga.source) ?: return@mapNotNull null
                val chapters = updates.mapNotNull { getChapter.await(it.update.chapterId) }
                manga to chapters
            }.toMap()
            if (batch.isNotEmpty()) {
                downloadManager.downloadChapters(batch)
            }
        }
    }

    /**
     * Delete selected chapters
     *
     * @param updatesItem list of chapters
     */
    fun deleteChapters(updatesItem: List<UpdatesItem>) {
        screenModelScope.launchNonCancellable {
            updatesItem
                .groupBy { it.update.mangaId }
                .entries
                .forEach { (mangaId, updates) ->
                    val manga = getManga.await(mangaId) ?: return@forEach
                    val source = sourceManager.get(manga.source) ?: return@forEach
                    val chapters = updates.mapNotNull { getChapter.await(it.update.chapterId) }
                    downloadManager.deleteChapters(
                        chapters,
                        manga,
                        source,
                        // KMK -->
                        ignoreCategoryExclusion = true,
                        // KMK <--
                    )
                }
        }
        toggleAllSelection(false)
    }

    fun showConfirmDeleteChapters(updatesItem: List<UpdatesItem>) {
        setDialog(Dialog.DeleteConfirmation(updatesItem))
    }

    // KMK -->
    /** Bundles all of the boolean flags for update‐selection into one type */
    data class UpdateSelectionOptions(
        val selected: Boolean,
        val fromLongPress: Boolean = false,
        val isGroup: Boolean = false,
        val isExpanded: Boolean = false,
    )
    // KMK <--

    fun toggleSelection(
        item: UpdatesItem,
        // KMK -->
        selectionOptions: UpdateSelectionOptions,
        // KMK <--
    ) {
        // KMK -->
        val (selected, fromLongPress, isGroup, isExpanded) = selectionOptions
        // KMK <--
        mutableState.update { state ->
            // KMK -->
            val selectedIndex = state.items.indexOfFirst { it.update.chapterId == item.update.chapterId }
            if (selectedIndex < 0) return@update state
            val selectedItem = state.items[selectedIndex]
            if (selectedItem.selected == selected) return@update state
            // KMK <--

            val newItems = state.items.toMutableList().apply {
                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedChapterIds.addOrRemove(item.update.chapterId, selected)

                // KMK -->
                if (isGroup) {
                    val selectedItemDate = selectedItem.update.dateFetch.toLocalDate()
                    val zone = java.time.ZoneId.systemDefault()
                    val dayStartMillis = selectedItemDate.atStartOfDay(zone).toInstant().toEpochMilli()
                    val dayEndMillis = selectedItemDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

                    state.items.mapIndexed { index, item -> index to item }
                        .filter {
                            it.second.update.mangaId == selectedItem.update.mangaId &&
                                it.second.update.dateFetch in dayStartMillis..<dayEndMillis
                        }
                        .forEach { (index, item) ->
                            set(index, item.copy(selected = selected))
                            selectedChapterIds.addOrRemove(item.update.chapterId, selected)
                        }
                }
                // KMK <--

                if (selected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Try to select the items in-between when possible
                        val range: IntRange
                        if (selectedIndex < selectedPositions[0]) {
                            range = selectedIndex + 1..<selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            range = (selectedPositions[1] + 1)..<selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Just select itself
                            range = IntRange.EMPTY
                        }

                        range.forEach {
                            val inBetweenItem = get(it)
                            if (!inBetweenItem.selected) {
                                selectedChapterIds.add(inBetweenItem.update.chapterId)
                                set(it, inBetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (!fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) {
                            selectedPositions[0] = indexOfFirst { it.selected }
                        } else if (selectedIndex == selectedPositions[1]) {
                            selectedPositions[1] = indexOfLast { it.selected }
                        }
                    } else {
                        if (selectedIndex < selectedPositions[0]) {
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            selectedPositions[1] = selectedIndex
                        }
                    }
                }
            }
            state.copy(items = newItems.toPersistentList())
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedChapterIds.addOrRemove(it.update.chapterId, selected)
                it.copy(selected = selected)
            }
            // KMK -->
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            // KMK <--
            state.copy(items = newItems.toPersistentList())
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedChapterIds.addOrRemove(it.update.chapterId, !it.selected)
                it.copy(selected = !it.selected)
            }
            // KMK -->
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            // KMK <--
            state.copy(items = newItems.toPersistentList())
        }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun resetNewUpdatesCount() {
        libraryPreferences.newUpdatesCount().set(0)
    }

    // KMK -->
    fun toggleExpandedState(key: String) {
        val willExpand = !mutableState.value.expandedState.contains(key)
        ChimanewDiagnostics.log(DiagnosticCategory.UPDATES, "MangaUpdates") {
            "Series card $key -> ${if (willExpand) "EXPANDED" else "COLLAPSED"}"
        }
        mutableState.update {
            it.copy(
                expandedState = it.expandedState.toMutableSet().apply {
                    if (it.expandedState.contains(key)) remove(key) else add(key)
                },
            )
        }
    }

    val chapterSwipeStartAction by libraryPreferences.swipeToEndAction().asState(screenModelScope)
    val chapterSwipeEndAction by libraryPreferences.swipeToStartAction().asState(screenModelScope)

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.ChapterSwipeAction.Disabled]
     */
    fun updateSwipe(updateItem: UpdatesItem, swipeAction: LibraryPreferences.ChapterSwipeAction) {
        updateSwipe(listOf(updateItem), swipeAction)
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.ChapterSwipeAction.Disabled]
     */
    fun updateSwipe(items: List<UpdatesItem>, swipeAction: LibraryPreferences.ChapterSwipeAction) {
        screenModelScope.launch {
            executeUpdateSwipeAction(items, swipeAction)
        }
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.ChapterSwipeAction.Disabled]
     */
    private fun executeUpdateSwipeAction(
        items: List<UpdatesItem>,
        swipeAction: LibraryPreferences.ChapterSwipeAction,
    ) {
        if (items.isEmpty()) return
        ChimanewDiagnostics.log(DiagnosticCategory.UPDATES, "MangaUpdates") {
            "Swipe action ($swipeAction) executed on ${items.size} chapters of '${items.firstOrNull()?.update?.mangaTitle}'."
        }
        when (swipeAction) {
            LibraryPreferences.ChapterSwipeAction.ToggleRead -> {
                val anyUnread = items.any { !it.update.read }
                markUpdatesRead(items, anyUnread)
            }
            LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> {
                val anyUnbookmarked = items.any { !it.update.bookmark }
                bookmarkUpdates(items, anyUnbookmarked)
            }
            LibraryPreferences.ChapterSwipeAction.Download -> {
                val allDownloaded = items.all { it.downloadStateProvider() == Download.State.DOWNLOADED }
                val anyDownloading = items.any {
                    val s = it.downloadStateProvider()
                    s == Download.State.QUEUE || s == Download.State.DOWNLOADING
                }
                val downloadAction = when {
                    allDownloaded -> ChapterDownloadAction.DELETE
                    anyDownloading -> ChapterDownloadAction.CANCEL
                    else -> ChapterDownloadAction.START_NOW
                }
                downloadChapters(
                    items = items,
                    action = downloadAction,
                )
            }
            LibraryPreferences.ChapterSwipeAction.Disabled -> throw IllegalStateException()
        }
    }
    // KMK <--

    private fun getUpdatesItemPreferenceFlow(): Flow<ItemPreferences> {
        return combine(
            updatesPreferences.filterDownloaded().changes(),
            updatesPreferences.filterUnread().changes(),
            updatesPreferences.filterStarted().changes(),
            updatesPreferences.filterBookmarked().changes(),
            updatesPreferences.filterExcludedScanlators().changes(),
        ) { downloaded, unread, started, bookmarked, excludedScanlators ->
            ItemPreferences(
                filterDownloaded = downloaded,
                filterUnread = unread,
                filterStarted = started,
                filterBookmarked = bookmarked,
                filterExcludedScanlators = excludedScanlators,
            )
        }
    }

    fun showFilterDialog() {
        mutableState.update { it.copy(dialog = Dialog.FilterSheet) }
    }

    @Immutable
    private data class ItemPreferences(
        val filterDownloaded: TriState,
        val filterUnread: TriState,
        val filterStarted: TriState,
        val filterBookmarked: TriState,
        val filterExcludedScanlators: Boolean,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val hasActiveFilters: Boolean = false,
        val items: PersistentList<UpdatesItem> = persistentListOf(),
        // KMK -->
        val expandedState: Set<String> = persistentSetOf(),
        val consolidateUpdates: Boolean = true,
        // KMK <--
        val dialog: Dialog? = null,
    ) {
        val selected = items.filter { it.selected }
        val selectionMode = selected.isNotEmpty()

        fun getUiModel(): List<UpdatesUiModel> {
            // KMK -->
            return items
                .groupBy { it.update.dateFetch.toLocalDate() }
                .flatMap { (date, mangas) ->
                    val mangaGroups = mangas.groupBy { it.update.mangaId }
                    val header = UpdatesUiModel.Header(date, mangaGroups.size)
                    val mangaItems = if (consolidateUpdates) {
                        mangaGroups.values
                            .sortedByDescending { chapters -> chapters.maxOf { it.update.dateFetch } }
                            .flatMap { mangaChapters ->
                                val isExpandable = mangaChapters.size > 1
                                val isExpanded = expandedState.contains(mangaChapters.first().update.groupByDateAndManga())
                                val (unread, read) = mangaChapters.partition { !it.update.read }
                                val sortedChapters = unread.sortedBy { it.update.dateFetch } +
                                    read.sortedByDescending { it.update.dateFetch }
                                val leader = UpdatesUiModel.Leader(
                                    item = sortedChapters.first(),
                                    isExpandable = isExpandable,
                                    allItems = sortedChapters,
                                )
                                if (!isExpandable || !isExpanded) {
                                    listOf(leader)
                                } else {
                                    listOf(leader) + sortedChapters.drop(1).map {
                                        UpdatesUiModel.Item(it, isExpandable = true)
                                    }
                                }
                            }
                    } else {
                        mangaGroups.values.flatMap { mangaChapters ->
                            mangaChapters.map { UpdatesUiModel.Item(it, isExpandable = false) }
                        }
                    }
                    listOf(header) + mangaItems
                }
                .distinctBy {
                    when (it) {
                        is UpdatesUiModel.Header -> "header-${it.date}"
                        is UpdatesUiModel.Leader -> "leader-${it.item.update.groupByDateAndManga()}"
                        is UpdatesUiModel.Item -> "item-${it.item.update.mangaId}-${it.item.update.chapterId}"
                    }
                }
            // KMK <--
        }
    }

    sealed interface Dialog {
        data class DeleteConfirmation(val toDelete: List<UpdatesItem>) : Dialog
        data object FilterSheet : Dialog
    }

    sealed interface Event {
        data object InternalError : Event
        data class LibraryUpdateTriggered(val started: Boolean) : Event
    }
}

private fun TriState.toBooleanOrNull(): Boolean? {
    return when (this) {
        TriState.DISABLED -> null
        TriState.ENABLED_IS -> true
        TriState.ENABLED_NOT -> false
    }
}

@Immutable
data class UpdatesItem(
    val update: UpdatesWithRelations,
    val downloadStateProvider: () -> Download.State,
    val downloadProgressProvider: () -> Int,
    val selected: Boolean = false,
) {
    // SY -->
    fun isEhBasedUpdate(): Boolean {
        return update.sourceId == EH_SOURCE_ID || update.sourceId == EXH_SOURCE_ID
    }
    // SY <--
}

// KMK -->
/** String to identify which manga's update on which day it is collapsing */
fun UpdatesWithRelations.groupByDateAndManga() = "${dateFetch.toLocalDate().toEpochDay()}-$mangaId"
// KMK <--
