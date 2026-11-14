package com.amrreda.downloader.ui.screens

import android.content.ClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amrreda.downloader.R
import com.amrreda.downloader.data.DownloadStatus
import com.amrreda.downloader.extract.MediaItem
import com.amrreda.downloader.ui.MainViewModel
import com.amrreda.downloader.ui.components.ActiveDownloadRow
import com.amrreda.downloader.ui.components.Thumb
import com.amrreda.downloader.ui.components.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val all by vm.downloads.collectAsStateWithLifecycle()
    val active = all.filter { it.statusEnum != DownloadStatus.COMPLETED }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = {
                if (active.any { it.statusEnum == DownloadStatus.RUNNING || it.statusEnum == DownloadStatus.QUEUED })
                    TextButton(onClick = { vm.engine.pauseAll() }) { Text(stringResource(R.string.pause_all)) }
                else if (active.any { it.statusEnum == DownloadStatus.PAUSED || it.statusEnum == DownloadStatus.FAILED })
                    TextButton(onClick = { vm.engine.resumeAll() }) { Text(stringResource(R.string.resume_all)) }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                OutlinedTextField(
                    value = vm.urlInput,
                    onValueChange = { vm.urlInput = it },
                    label = { Text(stringResource(R.string.url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.analyze() }),
                    trailingIcon = {
                        IconButton(onClick = {
                            val cm = context.getSystemService(ClipboardManager::class.java)
                            cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { vm.urlInput = it.trim() }
                        }) { Icon(Icons.Filled.ContentPaste, stringResource(R.string.paste)) }
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = vm.fileNameInput,
                    onValueChange = { vm.fileNameInput = it },
                    label = { Text(stringResource(R.string.filename_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { vm.downloadDirect() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.download_now))
                    }
                    FilledTonalButton(onClick = { vm.analyze() }, enabled = !vm.analyzing, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Search, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.find_media))
                    }
                }
            }
            item {
                Text(stringResource(R.string.home_tip), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (vm.analyzing) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.analyzing))
                }
            }
            vm.analyzeError?.let { err ->
                item {
                    Column {
                        Text(err, color = MaterialTheme.colorScheme.error)
                        vm.lastAnalyzedUrl?.let { url ->
                            TextButton(onClick = { vm.openInBrowser(url) }) { Text(stringResource(R.string.open_in_browser)) }
                        }
                    }
                }
            }

            if (vm.foundItems.isNotEmpty()) {
                item {
                    Column {
                        vm.pageTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.found_n, vm.foundItems.size), modifier = Modifier.weight(1f))
                            val allSelected = vm.selectedUrls.size == vm.foundItems.size
                            TextButton(onClick = { vm.selectAll(!allSelected) }) {
                                Text(stringResource(if (allSelected) R.string.select_none else R.string.select_all))
                            }
                            Button(onClick = { vm.downloadSelected() }, enabled = vm.selectedUrls.isNotEmpty()) {
                                Text(stringResource(R.string.download_n, vm.selectedUrls.size))
                            }
                        }
                    }
                }
                items(vm.foundItems, key = { "f_" + it.url }) { item ->
                    FoundItemRow(item, item.url in vm.selectedUrls) { vm.toggleSelected(item.url) }
                }
            }

            if (active.isNotEmpty()) {
                item { Text(stringResource(R.string.active_downloads), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                items(active, key = { "d_" + it.id }) { ActiveDownloadRow(it, vm.engine) }
            } else if (vm.foundItems.isEmpty() && !vm.analyzing) {
                item {
                    Text(stringResource(R.string.no_active), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp))
                }
            }
        }
    }
}

@Composable
fun FoundItemRow(item: MediaItem, selected: Boolean, downloadMode: Boolean = false, onToggle: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(item.url, item.category, 56.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(item.category.label()) + " • " + item.url, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (downloadMode) IconButton(onClick = onToggle) { Icon(Icons.Filled.Download, stringResource(R.string.download_now)) }
            else Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}
