package com.lc.content.sdk.demo.lab

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.api.Drama
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.ui.DemoTheme
import kotlinx.coroutines.launch

/**
 * Content SDK - player lab (demo only)
 *
 * The SDK player page fills the screen, as it does in an app, and the demo draws its own controls
 * over it, the way a host app with its own player UI would:
 *
 * - the button at the top right opens the **control panel**: playback controls (play / pause, seek,
 *   speed, quality, episodes) and settings (playback options, page UI style, region visibility),
 *   every one a `ContentPlayerController` or `AndroidContentSdk` call;
 * - the strip at the top left is the **log**: every SDK call made here and every playback event,
 *   from `setPlayerEventListener`.
 *
 * The controller comes from `setPlayerControllerListener`; the SDK calls are in [LabActions] and the
 * switches' state in [LabSettings]. The files of this folder are only the screen.
 */
@Composable
fun PlayerLabScreen(onBack: () -> Unit) {
    // State input: the page's controller and the drama list from the SDK; the picked drama.
    var controller by remember { mutableStateOf<ContentPlayerController?>(null) }
    var dramas by remember { mutableStateOf<List<Drama>>(emptyList()) }
    var selected by remember { mutableStateOf<Drama?>(null) }
    // Chrome state: panel open and its section survive a rotation.
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableStateOf(PanelSection.Controls) }
    val scope = rememberCoroutineScope()

    // Listeners are process-wide and keep one each: start them here, stop them when the screen goes.
    DisposableEffect(Unit) {
        LabActions.startListening { controller = it }
        onDispose { LabActions.stopListening() }
    }
    LaunchedEffect(Unit) { dramas = LabActions.loadDramas() }
    // While the panel covers the page, keep the page controls shown; let them hide again when it closes.
    DisposableEffect(controller, panelOpen) {
        val held = controller.takeIf { panelOpen }
        held?.let { LabActions.holdControls(it, true) }
        onDispose { held?.let { LabActions.holdControls(it, false) } }
    }
    val closePlayer = {
        selected = null
        panelOpen = false
    }
    // Back closes the panel, then the player page, then leaves the screen. The custom info region's
    // back button sends a system back too, so it lands here.
    BackHandler {
        when {
            panelOpen -> panelOpen = false
            selected != null -> closePlayer()
            else -> onBack()
        }
    }

    val pick: (Drama) -> Unit = {
        selected = it
        LabLog.add(LogKind.Api, "open ${it.name}")
    }
    val reload: () -> Unit = { scope.launch { dramas = LabActions.loadDramas() } }
    val history: () -> Unit = { scope.launch { LabActions.loadHistory() } }

    DemoTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val drama = selected
            if (drama == null) {
                DramaPicker(dramas = dramas, onPick = pick, onReload = reload, onHistory = history, onBack = onBack)
            } else {
                PlayerWithChrome(
                    drama = drama,
                    controller = controller,
                    panelOpen = panelOpen,
                    section = section,
                    onSection = { section = it },
                    onTogglePanel = { panelOpen = !panelOpen },
                    onClose = closePlayer,
                )
            }
        }
    }
}

/** The SDK player page with the lab's chrome over it: log overlay, panel button, control panel. */
@Composable
private fun PlayerWithChrome(
    drama: Drama,
    controller: ContentPlayerController?,
    panelOpen: Boolean,
    section: PanelSection,
    onSection: (PanelSection) -> Unit,
    onTogglePanel: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // The SDK player page, full screen. Its own back button returns to the picker.
        key(drama.id) {
            AndroidContentSdk.DramaPlayerScreen(
                params = Drama.PlayerParams(id = drama.id),
                onCloseRequest = onClose,
            )
        }
        if (LabSettings.logOverlay) LogOverlay(Modifier.align(Alignment.TopStart))
        // Top right, where the SDK page draws nothing.
        RoundIconButton(
            icon = if (panelOpen) R.drawable.ic_close else R.drawable.ic_tune,
            description = if (panelOpen) "Close panel" else "Open panel",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = LabLayout.EDGE_MARGIN.dp),
            filled = panelOpen,
            onClick = onTogglePanel,
        )
        AnimatedVisibility(
            visible = panelOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            ControlPanel(controller = controller, section = section, onSection = onSection)
        }
    }
}
