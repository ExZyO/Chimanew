// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticEntry
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.common.i18n.stringResource as contextStringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

class ChimanewDiagnosticLogViewerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val allLogs by ChimanewDiagnostics.entriesFlow.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }

        val filteredLogs = remember(allLogs, searchQuery) {
            if (searchQuery.isBlank()) {
                allLogs
            } else {
                val query = searchQuery.trim().lowercase()
                allLogs.filter { entry ->
                    entry.message.lowercase().contains(query) ||
                        entry.tag.lowercase().contains(query) ||
                        entry.category.code.lowercase().contains(query) ||
                        entry.category.label.lowercase().contains(query) ||
                        (entry.error?.lowercase()?.contains(query) == true)
                }
            }
        }

        val listState = rememberLazyListState()

        Scaffold(
            topBar = {
                if (isSearching) {
                    SearchToolbar(
                        searchQuery = searchQuery,
                        onChangeSearchQuery = { searchQuery = it.orEmpty() },
                        placeholderText = "Filter by title, tag, category...",
                        navigateUp = {
                            isSearching = false
                            searchQuery = ""
                        },
                    )
                } else {
                    AppBar(
                        title = "Nuvio Diagnostics",
                        navigateUp = navigator::pop,
                        actions = {
                            AppBarActions(
                                persistentListOf(
                                    AppBar.Action(
                                        title = "Search",
                                        icon = Icons.Default.Search,
                                        onClick = { isSearching = true },
                                    ),
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_copy_to_clipboard),
                                        icon = Icons.Default.ContentCopy,
                                        onClick = {
                                            val text = ChimanewDiagnostics.exportText()
                                            if (text.isBlank()) {
                                                context.toast(MR.strings.pref_chimanew_diagnostics_empty)
                                            } else {
                                                context.copyToClipboard("Nuvio Diagnostics", text)
                                                context.toast(context.contextStringResource(MR.strings.pref_chimanew_diagnostics_copied, allLogs.size))
                                            }
                                        },
                                    ),
                                    AppBar.Action(
                                        title = "Clear",
                                        icon = Icons.Outlined.DeleteSweep,
                                        onClick = {
                                            ChimanewDiagnostics.clear()
                                            context.toast(MR.strings.pref_chimanew_diagnostics_cleared)
                                        },
                                    ),
                                ),
                            )
                        },
                        scrollBehavior = it,
                    )
                }
            },
        ) { contentPadding ->
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(MR.strings.pref_chimanew_diagnostics_empty)
                        } else {
                            "No matching diagnostic events for \"$searchQuery\""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = contentPadding + androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = filteredLogs,
                        key = { it.timestamp.toString() + it.tag + it.message.hashCode() },
                    ) { entry ->
                        DiagnosticLogItem(entry)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun DiagnosticLogItem(entry: DiagnosticEntry) {
        val categoryColor = when (entry.category) {
            DiagnosticCategory.HERO -> Color(0xFF4CAF50) // Green
            DiagnosticCategory.ARTWORK -> Color(0xFF00BCD4) // Cyan
            DiagnosticCategory.NETWORK -> Color(0xFFFFB300) // Amber
            DiagnosticCategory.DETAILS -> Color(0xFFAB47BC) // Purple
            DiagnosticCategory.SHELVES -> Color(0xFF42A5F5) // Blue
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(8.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = entry.category.code.uppercase(),
                            color = categoryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = entry.tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Text(
                        text = entry.message,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                    )
                }

                if (!entry.error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            text = entry.error,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
