package com.lc.content.sdk.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.demo.customization.CustomRegions
import com.lc.content.sdk.demo.lab.PlayerLabScreen
import com.lc.content.sdk.demo.ui.DemoHomeScreen
import com.lc.content.sdk.demo.ui.DemoScreen
import com.lc.content.sdk.demo.ui.SdkStatus

/**
 * Demo entry screen.
 *
 * SDK calls made from here:
 *   - [startContentSdk] (DemoEdition.kt) -> Step 4: start the SDK (the commercial edition asks for ad consent first)
 *   - [openDramaHome]                   -> Step 5: show the SDK page
 * The rest of this file is demo UI.
 */
class MainActivity : ComponentActivity() {

    /** Simple UI state so the demo can show what the SDK is doing. */
    private var sdkStatus by mutableStateOf<SdkStatus>(SdkStatus.NotStarted)

    /** Which demo screen is shown. Not part of the SDK API. */
    private var screen by mutableStateOf(DemoScreen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            when (screen) {
                DemoScreen.Home -> DemoHomeScreen(
                    status = sdkStatus,
                    pageStyle = CustomRegions.style,
                    onPageStyleChange = CustomRegions::apply,
                    onOpenDramaHome = ::onOpenDramaHomeClick,
                    onOpenLab = ::onOpenLabClick,
                )

                DemoScreen.Lab -> PlayerLabScreen(onBack = { screen = DemoScreen.Home })
            }
        }
    }

    private fun onOpenDramaHomeClick() = whenSdkReady { openDramaHome() }

    private fun onOpenLabClick() = whenSdkReady { screen = DemoScreen.Lab }

    /** Runs [action] once the SDK is started, starting it first if needed. */
    private fun whenSdkReady(action: () -> Unit) {
        when (sdkStatus) {
            // Already started: run the action directly. `start` only needs to succeed once per process.
            SdkStatus.Ready -> action()
            // A start request is running; ignore repeated taps.
            SdkStatus.Starting -> Unit
            // Not started yet (or the last attempt failed): start first, then run the action.
            else -> {
                sdkStatus = SdkStatus.Starting
                startContentSdk(this) { result ->
                    sdkStatus = result
                    when (result) {
                        SdkStatus.Ready -> action()
                        is SdkStatus.Failed -> Toast.makeText(this, "SDK start failed: ${result.code}", Toast.LENGTH_SHORT).show()
                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * Content SDK - integration Step 5 (show the SDK page)
     *
     * One line opens the SDK's own full-screen drama home. The SDK handles browsing, the player,
     * and episode switching from there.
     *
     * Want the page inside your own screen (a Fragment tab, a Composable), the player opened
     * directly, or your own player? See SdkApiCatalog.kt.
     */
    private fun openDramaHome() {
        AndroidContentSdk.openDramaHomePage(this)
    }
}
