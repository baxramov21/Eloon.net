buildscript {
    dependencies {
        classpath ("gradle.plugin.com.onesignal:onesignal-gradle-plugin:[0.12.10, 0.99.99]")
    }
}

plugins {
    // Apply other plugins if needed
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id ("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
    id ("com.google.gms.google-services") version "4.3.15" apply false
}
