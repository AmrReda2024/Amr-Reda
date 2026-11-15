package com.amrreda.downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amrreda.downloader.R
import com.amrreda.downloader.engine.StorageSaver
import com.amrreda.downloader.engine.YtDlpEngine
import com.amrreda.downloader.extract.Platform
import com.amrreda.downloader.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val max by vm.settings.maxConcurrent.collectAsStateWithLifecycle()
    val wifi by vm.settings.wifiOnly.collectAsStateWithLifecycle()
    val engineState by vm.video.state.collectAsStateWithLifecycle()
    val updating by vm.video.updating.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshAccounts() }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.tab_settings)) })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {

            // ---- Accounts ----
            Text(stringResource(R.string.accounts_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.accounts_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Platform.entries.forEach { p ->
                val signed = p in vm.signedIn
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(platformColor(p)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(if (signed) R.string.signed_in else R.string.not_signed_in),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (signed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (signed) TextButton(onClick = { vm.signOut(p) }) { Text(stringResource(R.string.sign_out)) }
                    else OutlinedButton(onClick = { vm.signIn(p) }) { Text(stringResource(R.string.sign_in)) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ---- Video engine ----
            Text(stringResource(R.string.engine_title), style = MaterialTheme.typography.titleMedium)
            val status = when (val s = engineState) {
                is YtDlpEngine.State.Initializing -> stringResource(R.string.engine_starting)
                is YtDlpEngine.State.Ready -> stringResource(R.string.engine_ready, s.version ?: "?")
                is YtDlpEngine.State.Failed -> stringResource(R.string.engine_failed, s.message)
            }
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.engine_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = { vm.updateVideoEngine() }, enabled = !updating && engineState is YtDlpEngine.State.Ready) {
                    Text(stringResource(R.string.update_engine))
                }
                if (updating) {
                    Spacer(Modifier.width(12.dp)); CircularProgressIndicator(Modifier.size(20.dp))
                }
            }
            vm.engineUpdateMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ---- Downloads ----
            Text(stringResource(R.string.setting_parallel, max), style = MaterialTheme.typography.titleMedium)
            Slider(value = max.toFloat(), onValueChange = { vm.settings.setMaxConcurrent(it.toInt()) },
                valueRange = 1f..6f, steps = 4)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.setting_wifi), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.setting_wifi_desc), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = wifi, onCheckedChange = { vm.settings.setWifiOnly(it) })
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text(stringResource(R.string.setting_location), style = MaterialTheme.typography.titleMedium)
            Text("Download/${StorageSaver.ROOT_FOLDER}/", style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.setting_location_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            OutlinedButton(onClick = { vm.engine.clearFinished() }) { Text(stringResource(R.string.clear_history)) }
            Text(stringResource(R.string.clear_history_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodySmall)
        }
    }
}
