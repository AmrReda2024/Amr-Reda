package com.amrreda.downloader.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.amrreda.downloader.R
import com.amrreda.downloader.ui.screens.BrowserScreen
import com.amrreda.downloader.ui.screens.FilesScreen
import com.amrreda.downloader.ui.screens.HomeScreen
import com.amrreda.downloader.ui.screens.SettingsScreen

private data class Tab(val label: Int, val icon: ImageVector)

@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(vm.requestedTab) {
        vm.requestedTab?.let { tab = it; vm.requestedTab = null }
    }
    val tabs = listOf(
        Tab(R.string.tab_home, Icons.Filled.Download),
        Tab(R.string.tab_browser, Icons.Filled.Language),
        Tab(R.string.tab_files, Icons.Filled.Folder),
        Tab(R.string.tab_settings, Icons.Filled.Settings),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            }
        }
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            0 -> HomeScreen(vm, m)
            1 -> BrowserScreen(vm, m)
            2 -> FilesScreen(vm, m)
            else -> SettingsScreen(vm, m)
        }
    }
}
