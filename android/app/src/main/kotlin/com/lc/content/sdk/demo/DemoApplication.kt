package com.lc.content.sdk.demo

import android.app.Application
import com.lc.content.sdk.AndroidContentSdk

/**
 * Content SDK - integration Step 3 (initialize)
 *
 * Call `initialize` exactly ONCE, as early as possible, from your `Application.onCreate()`.
 * It only stores your configuration (no network or player start here), so it is cheap.
 * The credentials come from `SdkConfig` in DemoEdition.kt.
 *
 * Register your Application class in AndroidManifest.xml:
 *   <application android:name=".YourApplication" ...>
 */
class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // (Optional) Print SDK status messages to Logcat. Keep it off in your release builds.
        AndroidContentSdk.setLoggingEnabled(BuildConfig.DEBUG)

        AndroidContentSdk.initialize(
            application = this,
            appId = SdkConfig.APP_ID,
            appSecret = SdkConfig.APP_SECRET,
            userId = SdkConfig.USER_ID,
            // (Optional) Content language, e.g. "en-US". Pass "" to follow your app's locale.
            language = "",
        )
    }
}
