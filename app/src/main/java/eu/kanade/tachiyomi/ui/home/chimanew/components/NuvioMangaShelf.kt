// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.home.chimanew.HomeMangaItem

@Composable
fun NuvioMangaShelf(
    title: String,
    items: List<HomeMangaItem>,
    modifier: Modifier = Modifier,
    showProgressBar: Boolean = true,
    showUnreadBadge: Boolean = true,
    onItemClick: (HomeMangaItem) -> Unit,
    onLongClick: ((HomeMangaItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = items,
                key = { it.mangaId },
            ) { item ->
                NuvioMangaCard(
                    item = item,
                    showProgressBar = showProgressBar,
                    showUnreadBadge = showUnreadBadge,
                    onClick = { onItemClick(item) },
                    onLongClick = onLongClick?.let { { it(item) } },
                )
            }
        }
    }
}
