package eu.kanade.presentation.updates.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.updates.CollapseButton
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.updates.novel.groupByDateAndNovel
import tachiyomi.domain.updates.novel.model.NovelUpdatesWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun NovelUpdatesScreen(
    isLoading: Boolean,
    items: List<NovelUpdatesWithRelations>,
    expandedState: Set<String> = emptySet(),
    consolidateUpdates: Boolean = true,
    collapseToggle: (key: String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    onClickItem: (NovelUpdatesWithRelations) -> Unit,
) {
    if (isLoading) {
        LoadingScreen(Modifier.padding(contentPadding))
        return
    }
    if (items.isEmpty()) {
        EmptyScreen(
            stringRes = MR.strings.information_no_recent_manga,
            modifier = Modifier.padding(contentPadding),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items.groupBy { it.dateFetch / 86_400_000L }.forEach { (day, dayUpdates) ->
            item(key = "novelUpdatesHeader-$day") {
                ListGroupHeader(text = relativeDateText(dayUpdates.first().dateFetch))
            }
            if (consolidateUpdates) {
                val novelGroups = dayUpdates.groupBy { it.novelId }
                    .values
                    .sortedByDescending { it.maxOf { ch -> ch.dateFetch } }

                novelGroups.forEach { chapters ->
                    val isExpandable = chapters.size > 1
                    val key = chapters.first().groupByDateAndNovel()
                    val isExpanded = expandedState.contains(key)
                    val unreadChapters = chapters.filter { !it.read }
                    val earliestUnread = unreadChapters.minByOrNull { it.dateFetch } ?: chapters.first()
                    val latestChapter = chapters.maxByOrNull { it.dateFetch } ?: chapters.first()
                    val hasUnread = unreadChapters.isNotEmpty()
                    val hasBookmark = chapters.any { it.bookmark }
                    val extraCount = (chapters.size - 1).coerceAtLeast(0)

                    item(key = "novelUpdatesLeader-$key") {
                        NovelUpdatesUiItem(
                            update = latestChapter,
                            onClick = { onClickItem(earliestUnread) },
                            isLeader = true,
                            isExpandable = isExpandable,
                            expanded = isExpanded,
                            collapseToggle = { collapseToggle(key) },
                            extraCount = extraCount,
                            latestDateFetch = latestChapter.dateFetch,
                            hasUnread = hasUnread,
                            hasBookmark = hasBookmark,
                        )
                    }

                    if (isExpandable && isExpanded) {
                        items(
                            items = chapters.drop(1),
                            key = { "novelUpdatesChild-${it.novelId}-${it.chapterId}" },
                        ) { update ->
                            NovelUpdatesUiItem(
                                update = update,
                                onClick = { onClickItem(update) },
                                isLeader = false,
                                isExpandable = true,
                                expanded = false,
                                collapseToggle = {},
                                extraCount = 0,
                                latestDateFetch = null,
                                hasUnread = !update.read,
                                hasBookmark = update.bookmark,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = dayUpdates,
                    key = { "novelUpdates-${it.novelId}-${it.chapterId}" },
                ) { update ->
                    NovelUpdatesUiItem(
                        update = update,
                        onClick = { onClickItem(update) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelUpdatesUiItem(
    update: NovelUpdatesWithRelations,
    onClick: () -> Unit,
    isLeader: Boolean = true,
    isExpandable: Boolean = false,
    expanded: Boolean = false,
    collapseToggle: () -> Unit = {},
    extraCount: Int = 0,
    latestDateFetch: Long? = null,
    hasUnread: Boolean = !update.read,
    hasBookmark: Boolean = update.bookmark,
) {
    val textAlpha = if (!hasUnread) DISABLED_ALPHA else 1f

    Row(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = null)
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLeader) {
            ItemCover.Book(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .width(48.dp)
                    .fillMaxHeight(),
                data = update.coverData,
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .width(48.dp)
                    .fillMaxHeight(),
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
        ) {
            if (isLeader) {
                Text(
                    text = update.novelTitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = textAlpha),
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasUnread) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = stringResource(MR.strings.unread),
                        modifier = Modifier
                            .height(8.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (hasBookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier
                            .height(14.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = update.chapterName,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = textAlpha),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                )
                if (extraCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "+$extraCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                if (latestDateFetch != null) {
                    DotSeparatorText()
                    Text(
                        text = relativeTimeSpanString(latestDateFetch),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (isLeader && isExpandable) {
            CollapseButton(
                expanded = expanded,
                collapseToggle = collapseToggle,
            )
        }
    }
}
