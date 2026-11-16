plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// Content SDK - integration Step 2 (dependencies)
//
// From the pure Android SDK package (ContentSdk-pure-Android-YOUR_SDK_VERSION-release):
// 1. Copy `ContentSdk-pure-YOUR_SDK_VERSION.aar` into `app/libs/`.
// 2. Copy `content-sdk-dependencies.gradle` into `app/` (next to this file).
// Both files MUST come from the same package. Nothing else needs editing:
// the AAR file name is detected below.
// ---------------------------------------------------------------------------
val contentSdkAarName: String = file("libs")
    .listFiles { file -> file.extension == "aar" && file.name.startsWith("ContentSdk") }
    .orEmpty()
    .let { aars ->
        check(aars.size == 1) {
            if (aars.isEmpty()) {
                "Content SDK AAR not found. Copy ContentSdk-pure-YOUR_SDK_VERSION.aar from the pure SDK package into app/libs/ (see README)."
            } else {
                "Found ${aars.size} Content SDK AARs in app/libs/ (${aars.joinToString { it.name }}). Keep only one."
            }
        }
        aars.single().nameWithoutExtension
    }

check(file("content-sdk-dependencies.gradle").isFile) {
    "content-sdk-dependencies.gradle not found. Copy it from the same SDK package into app/ (see README)."
}

// This is the PURE demo: refuse the commercial AAR or dependency script, so the editions never mix.
check(contentSdkAarName.startsWith("ContentSdk-pure-")) {
    "app/libs/$contentSdkAarName.aar is not a pure-edition AAR. Use ContentSdk-pure-YOUR_SDK_VERSION.aar here; the commercial AAR belongs in the ContentSDK-Commercial demo."
}
check("play-services-ads" !in file("content-sdk-dependencies.gradle").readText()) {
    "content-sdk-dependencies.gradle comes from the commercial package (it lists ad SDKs). Copy the one from the pure package."
}

// Adds every library the SDK needs at runtime (AndroidX, media3, ...). The pure edition has no ad SDKs.
apply(from = "content-sdk-dependencies.gradle")

android {
    namespace = "com.lc.content.sdk.demo"
    // Content SDK requires compileSdk >= 37 and minSdk >= 24.
    compileSdk = 37

    defaultConfig {
        // One application id per edition, so both demos can be installed side by side.
        // Your Content App ID / Secret are bound to your own application id.
        applicationId = "com.lc.content.sdk.demo.pure"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.2.0"
        resValue("string", "app_name", "CSDK Pure")
    }

    buildTypes {
        release {
            // The SDK ships its own consumer ProGuard rules, so shrinking works out of the box.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // Needed for the per-edition app_name in defaultConfig.
        resValues = true
    }
}

dependencies {
    // Content SDK (local AAR resolved through the `flatDir` repository in settings.gradle.kts).
    // Do NOT use `files("libs/xxx.aar")` - some AGP versions fail IDE sync with it.
    add("implementation", mapOf("name" to contentSdkAarName, "ext" to "aar"))

    // ---- Everything below is only for this demo's own UI, not required by the SDK ----
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.11.0")
    // Needed only for the Fragment example in SdkApiCatalog.kt.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}
