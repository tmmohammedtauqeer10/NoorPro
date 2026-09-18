// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:9.1.1")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
    classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.2.10")
    classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.5")
    classpath("io.github.takahirom.roborazzi:roborazzi-gradle-plugin:1.59.0")
    classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    classpath("com.google.gms:google-services:4.4.4")
    classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.3")
  }
}
