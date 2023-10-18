package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.activity.main.router.navigateAndPopAllStack
import com.xayah.databackup.ui.activity.operation.LocalCloudMode
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.GridItemPackage
import com.xayah.databackup.ui.component.ListItemManifestHorizontal
import com.xayah.databackup.ui.component.ListItemManifestVertical
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.ManifestTopBar
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.GridItemTokens
import com.xayah.databackup.util.IntentUtil
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.librootservice.util.withIOContext
import com.xayah.librootservice.util.withMainContext
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageBackupManifest() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<ManifestViewModel>()
    val scope = rememberCoroutineScope()
    val navController = LocalSlotScope.current!!.navController
    val cloudMode = LocalCloudMode.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val titles = remember {
        listOf(context.getString(R.string.overlook), context.getString(R.string.both), context.getString(R.string.apk), context.getString(R.string.data))
    }
    val uiState by viewModel.uiState
    val selectedBoth by uiState.selectedBoth.collectAsState(initial = 0)
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val bothPackages by uiState.bothPackages.collectAsState(initial = listOf())
    val apkOnlyPackages by uiState.apkOnlyPackages.collectAsState(initial = listOf())
    val dataOnlyPackages by uiState.dataOnlyPackages.collectAsState(initial = listOf())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ManifestTopBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.manifest),
                selectedTabIndex = selectedTabIndex,
                onTabClick = { index -> selectedTabIndex = index },
                titles = titles
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = true, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    modifier = Modifier.padding(CommonTokens.PaddingMedium),
                    onClick = {
                        scope.launch {
                            if (cloudMode) {
                                withIOContext {
                                    manifestOnFabClickExtension(context, uiState) {
                                        withMainContext {
                                            navController.navigateAndPopAllStack(OperationRoutes.PackageBackupProcessing.route)
                                        }
                                    }
                                }
                            } else {
                                withMainContext {
                                    navController.navigateAndPopAllStack(OperationRoutes.PackageBackupProcessing.route)
                                }
                            }
                        }
                    },
                    content = {
                        Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null)
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium)) {
                    Crossfade(targetState = selectedTabIndex, label = AnimationTokens.CrossFadeLabel) { index ->
                        when (index) {
                            // Overlook
                            0 -> {
                                Column(
                                    modifier = Modifier.paddingVertical(CommonTokens.PaddingLarge),
                                    verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
                                ) {
                                    ListItemManifestHorizontal(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_checklist),
                                        title = stringResource(R.string.selected_both),
                                        content = selectedBoth.toString()
                                    ) {
                                        selectedTabIndex = 1
                                    }
                                    ListItemManifestHorizontal(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_apps),
                                        title = stringResource(R.string.selected_apks),
                                        content = (selectedAPKs - selectedBoth).toString()
                                    ) {
                                        selectedTabIndex = 2
                                    }
                                    ListItemManifestHorizontal(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_database),
                                        title = stringResource(R.string.selected_data),
                                        content = (selectedData - selectedBoth).toString()
                                    ) {
                                        selectedTabIndex = 3
                                    }

                                    if (cloudMode) {
                                        OverlookExtensionItems()
                                    } else {
                                        val backupSavePath by context.readBackupSavePath().collectAsState(initial = "")
                                        ListItemManifestVertical(
                                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                                            title = stringResource(R.string.backup_dir),
                                            content = backupSavePath
                                        ) {
                                            IntentUtil.toDirectoryActivity(context = context, route = DirectoryRoutes.DirectoryBackup)
                                        }
                                    }
                                }
                            }

                            // APK + Data
                            1 -> {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                                    items(items = bothPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }
                            }

                            // APK only
                            2 -> {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                                    items(items = apkOnlyPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }
                            }

                            // Data only
                            3 -> {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                                    items(items = dataOnlyPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
