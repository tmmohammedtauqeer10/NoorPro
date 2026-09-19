import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
  id("io.github.takahirom.roborazzi")
  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

if (file("google-services.json").exists()) {
  apply(plugin = "com.google.gms.google-services")
  apply(plugin = "com.google.firebase.crashlytics")
}

val noorLocalProperties = Properties().apply {
  val localFile = rootProject.file("local.properties")
  if (localFile.exists()) localFile.inputStream().use { stream -> load(stream) }
}

tasks.matching { it.name == "uploadCrashlyticsMappingFileRelease" }.configureEach {
  enabled = false
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.noorpro.app"
    minSdk = 24
    targetSdk = 36
    // Unit-test manifest merging does not receive placeholders from the Secrets plugin.
    manifestPlaceholders["MAPS_API_KEY"] = noorLocalProperties.getProperty("MAPS_API_KEY", "NO_KEY_CONFIGURED")
        versionCode = 29
        versionName = "1.0.28"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8 verified via a temporary shrunk debug build (home/Room, Explore, Library/Moshi-JSON
      // all worked). proguard-rules.pro keeps Moshi/Retrofit/Firestore reflection paths.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = false
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation("com.batoulapps.adhan:adhan:1.2.1")
  implementation(platform("androidx.compose:compose-bom:2026.01.00"))
  implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-storage")
  implementation("com.google.firebase:firebase-appcheck-playintegrity")
  debugImplementation("com.google.firebase:firebase-appcheck-debug")
  implementation("com.google.firebase:firebase-crashlytics")
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  implementation("com.google.accompanist:accompanist-permissions:0.37.3")
  implementation("androidx.activity:activity-compose:1.10.1")
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material:1.6.5")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.core:core-ktx:1.18.0")
  implementation("androidx.datastore:datastore-preferences:1.1.7")
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.media3:media3-exoplayer:1.2.0")
  implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
  implementation("androidx.media3:media3-ui:1.2.0")
  // QR generation for WhatsApp-style profile/group sharing (pure-Java, small).
  implementation("com.google.zxing:core:3.5.3")
  // implementation(libs.androidx.navigation.compose)
  implementation("androidx.room:room-ktx:2.7.0")
  implementation("androidx.room:room-runtime:2.7.0")
  implementation("io.coil-kt:coil-compose:2.7.0")
  implementation("io.coil-kt:coil-video:2.7.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.12.0")
  // implementation(libs.firebase.ai)
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  implementation("com.google.android.gms:play-services-location:21.3.0")
  // 8.2.0 is compatible with NoorPro's compileSdk 36.
  implementation("com.google.maps.android:maps-compose:8.2.0")
  implementation("com.google.android.gms:play-services-ads:23.6.0")
  implementation("com.squareup.retrofit2:retrofit:2.12.0")
  testImplementation("androidx.compose.ui:ui-test-junit4")
  testImplementation("androidx.test:core:1.6.1")
  testImplementation("androidx.test.ext:junit:1.3.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
  testImplementation("org.robolectric:robolectric:4.16.1")
  testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
  testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
  testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.59.0")
  androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test:runner:1.6.2")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("com.airbnb.android:lottie-compose:6.4.0")
  add("ksp", "androidx.room:room-compiler:2.7.0")
  add("ksp", "com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
}

secrets {
  propertiesFileName = "local.properties"
  defaultPropertiesFileName = "local.defaults.properties"
}
