package com.amrreda.downloader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amrreda.downloader.R
import com.amrreda.downloader.data.DownloadEntity
import com.amrreda.downloader.data.DownloadStatus
import com.amrreda.downloader.data.FileCategory
import com.amrreda.downloader.ui.Format
import com.amrreda.downloader.ui.MainViewModel
import com.amrreda.downloader.ui.components.FileActions
import com.amrreda.downloader.ui.components.Thumb
import com.amrreda.downloader.ui.components.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val all by vm.downloads.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<DownloadEntity?>(null) }

    val completed = all.filter { it.statusEnum == DownloadStatus.COMPLETED }
    val shown = completed
        .filter { filter == null || it.category == filter }
        .filter { query.isBlank() || it.fileName.contains(query, ignoreCase = true) }
        .sortedByDescending { it.completedAt ?: it.createdAt }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.tab_files)) })
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text(stringResource(R.string.search_files)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null },
                label = { Text(stringResource(R.string.cat_all) + " (${completed.size})") })
            FileCategory.entries.forEach { cat ->
                val count = completed.count { it.category == cat.name }
                if (count > 0) FilterChip(
                    selected = filter == cat.name, onClick = { filter = cat.name },
                    label = { Text(stringResource(cat.label()) + " ($count)") },
                )
            }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_files), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { item -> FileRow(item, onDelete = { toDelete = item }) }
            }
        }
    }

    toDelete?.let { item ->
        var alsoFile by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = {
                Column {
                    Text(item.fileName)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { alsoFile = !alsoFile }) {
                        Checkbox(checked = alsoFile, onCheckedChange = { alsoFile = it })
                        Text(stringResource(R.string.delete_file_too))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.engine.delete(item.id, alsoFile); toDelete = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun FileRow(item: DownloadEntity, onDelete: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { FileActions.open(context, item.contentUri, item.mimeType) }) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(item.contentUri, item.categoryEnum, 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text(
                    Format.bytes(item.totalBytes) + " • " + Format.date(item.completedAt ?: item.createdAt),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.open)) },
                        onClick = { menu = false; FileActions.open(context, item.contentUri, item.mimeType) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.share)) },
                        onClick = { menu = false; FileActions.share(context, item.contentUri, item.mimeType) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.copy_link)) },
                        onClick = { menu = false; FileActions.copyLink(context, item.url) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) },
                        onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
