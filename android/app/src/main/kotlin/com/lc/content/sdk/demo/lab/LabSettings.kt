package com.lc.content.sdk.demo.lab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lc.content.sdk.demo.DemoEdition

/**
 * The switches of the lab's settings panel that outlive one player page.
 *
 * The playback options are SDK-wide and the SDK offers no getter, so the lab remembers what it set;
 * the first value of each is the SDK default, and setting one makes the SDK call. The player page
 * style and `autoHide` live in `CustomRegions`, because the SDK keeps regions for the whole process;
 * the panel reads them there.
 */
object LabSettings {

    /** Play as soon as an episode is ready. SDK default: [DemoEdition.AUTO_PLAY_DEFAULT]. */
    var autoPlay: Boolean by mutableStateOf(DemoEdition.AUTO_PLAY_DEFAULT)
        private set

    /** Move to the next episode when one ends. SDK default: on. */
    var autoAdvance: Boolean by mutableStateOf(true)
        private set

    /** Open a drama where it was left when no episode is named. SDK default: on. */
    var resumeFromHistory: Boolean by mutableStateOf(true)
        private set

    /** Whether the log overlay is drawn over the video. Demo only. */
    var logOverlay: Boolean by mutableStateOf(true)

    fun changeAutoPlay(enabled: Boolean) {
        autoPlay = enabled
        LabActions.setAutoPlay(enabled)
    }

    fun changeAutoAdvance(enabled: Boolean) {
        autoAdvance = enabled
        LabActions.setAutoAdvance(enabled)
    }

    fun changeResumeFromHistory(enabled: Boolean) {
        resumeFromHistory = enabled
        LabActions.setResumeFromHistory(enabled)
    }
}
