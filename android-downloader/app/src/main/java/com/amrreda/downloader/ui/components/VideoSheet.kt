package com.amrreda.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.amrreda.downloader.R
import com.amrreda.downloader.engine.QualityOption
import com.amrreda.downloader.engine.VideoFormats
import com.amrreda.downloader.extract.Platform
import com.amrreda.downloader.ui.Format
import com.amrreda.downloader.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoSheet(vm: MainViewModel) {
    val url = vm.videoSheetUrl ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { vm.closeVideoSheet() }, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()
        ) {
            val details = vm.videoDetails
            val error = vm.videoError
            when {
                error != null -> ErrorContent(vm, url, error.first, error.second)
                details == null -> Row(Modifier.padding(vertical = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.video_fetching), style = MaterialTheme.typography.titleMedium)
                        Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Icon(Icons.Filled.Videocam, null, modifier = Modifier.align(Alignment.Center).size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        if (details.thumbnail != null) AsyncImage(
                            model = details.thumbnail, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        )
                        if (details.durationSec > 0) Text(
                            duration(details.durationSec),
                            color = Color.White, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                .background(Color(0xAA000000), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(details.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    val sub = listOfNotNull(details.platform?.displayName ?: details.extractor, details.uploader).joinToString(" • ")
                    if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val videoOptions = details.options.filter { !it.isAudio }
                    val audioOptions = details.options.filter { it.isAudio }
                    var selected by remember(details) {
                        mutableStateOf(videoOptions.firstOrNull { it.height in 1..1080 } ?: details.options.firstOrNull())
                    }

                    if (videoOptions.isNotEmpty()) {
                        SectionTitle(Icons.Filled.Videocam, stringResource(R.string.video_quality))
                        OptionChips(videoOptions, selected) { selected = it }
                    }
                    if (audioOptions.isNotEmpty()) {
                        SectionTitle(Icons.Filled.MusicNote, stringResource(R.string.audio_only))
                        OptionChips(audioOptions, selected) { selected = it }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { selected?.let(vm::downloadVideo) },
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Filled.Download, null)
                        Spacer(Modifier.width(8.dp))
                        val s = selected
                        Text(
                            if (s == null) stringResource(R.string.download_now)
                            else stringResource(R.string.download_quality, optionLabel(s)) +
                                (if (s.estimatedBytes > 0) " • ~" + Format.bytes(s.estimatedBytes) else "")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun optionLabel(o: QualityOption): String = when (o.label) {
    "BEST" -> stringResource(R.string.quality_best)
    else -> o.label
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(options: List<QualityOption>, selected: QualityOption?, onSelect: (QualityOption) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(optionLabel(o), fontWeight = FontWeight.SemiBold)
                        if (o.estimatedBytes > 0) Text(Format.bytes(o.estimatedBytes), style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ErrorContent(vm: MainViewModel, url: String, message: String, hint: VideoFormats.ErrorHint) {
    val platform = Platform.detect(url)
    LaunchedEffect(Unit) { vm.refreshAccounts() }
    Column(Modifier.padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.video_failed), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        val explanation = when (hint) {
            VideoFormats.ErrorHint.SIGN_IN -> if (platform != null) stringResource(R.string.hint_sign_in, platform.displayName)
            else stringResource(R.string.hint_sign_in_generic)
            VideoFormats.ErrorHint.UNSUPPORTED -> stringResource(R.string.hint_unsupported)
            VideoFormats.ErrorHint.RATE_LIMIT -> stringResource(R.string.hint_rate_limit)
            VideoFormats.ErrorHint.NETWORK -> stringResource(R.string.err_no_internet)
            VideoFormats.ErrorHint.UPDATE -> stringResource(R.string.hint_update)
        }
        Text(explanation, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (platform != null && platform !in vm.signedIn) {
                Button(onClick = { vm.closeVideoSheet(); vm.signIn(platform) }) {
                    Text(stringResource(R.string.sign_in_to, platform.displayName))
                }
            }
            if (hint == VideoFormats.ErrorHint.UPDATE) {
                OutlinedButton(onClick = { vm.updateAndRetryVideo() }) { Text(stringResource(R.string.update_engine)) }
            }
            OutlinedButton(onClick = { vm.retryVideo() }) { Text(stringResource(R.string.retry)) }
            OutlinedButton(onClick = { vm.closeVideoSheet(); vm.openInBrowser(url) }) { Text(stringResource(R.string.open_in_browser)) }
        }
    }
}

private fun duration(sec: Int): String {
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
