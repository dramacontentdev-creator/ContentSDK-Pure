package com.lc.content.sdk.demo.ui

// What the demo screens show. None of this is part of the SDK API.

/** The screens of the demo app. */
enum class DemoScreen { Home, Lab }

/** What the home screen shows about the SDK. */
sealed interface SdkStatus {
    data object NotStarted : SdkStatus
    data object Starting : SdkStatus
    data object Ready : SdkStatus
    data class Failed(val code: String, val message: String) : SdkStatus
}

/** One row of the integration checklist on the home screen. */
data class IntegrationStep(val label: String, val title: String, val file: String)
