pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // Content SDK - integration Step 1 (repositories)
        // The pure edition has no ad SDKs, so google() and mavenCentral() are enough.
        google()
        mavenCentral()

        // Folder that holds the downloaded ContentSdk-pure-YOUR_SDK_VERSION.aar.
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "ContentSdkPureDemo"
include(":app")
