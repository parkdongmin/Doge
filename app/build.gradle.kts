import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProps = Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { props.load(it) }
}

android {
    namespace = "com.doge.simulator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.doge.simulator"
        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val googleWebClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        val storageBaseUrl = localProps.getProperty("FIREBASE_STORAGE_BASE_URL")
            ?: project.findProperty("FIREBASE_STORAGE_BASE_URL") as String?
            ?: "https://firebasestorage.googleapis.com/v0/b/placeholder.appspot.com/o"
        buildConfigField("String", "FIREBASE_STORAGE_BASE_URL", "\"$storageBaseUrl\"")

        // AdMob — debug 빌드는 항상 Google 공식 테스트 ID를 쓰도록 고정한다 (release에서만 실제 ID로 교체).
        // local.properties에 실제 ID를 넣어두더라도 debug 빌드에서 그걸 쓰면 신규 광고단위 워밍업 기간 동안
        // NO_FILL(3)이 뜨는 걸 실제 SDK 연동 문제와 구분할 수 없게 되므로, 두 빌드타입을 명확히 분리한다
        buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
        buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "AD_UNIT_REWARD_REFRESH", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "AD_UNIT_REWARD_OFFLINE_X2", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "AD_UNIT_REWARD_SKIP_WAIT", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "AD_UNIT_REWARD_UPGRADE_REVERT", "\"ca-app-pub-3940256099942544/5224354917\"")
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val admobAppId = localProps.getProperty("ADMOB_APP_ID")
                ?: "ca-app-pub-3940256099942544~3347511713"
            val admobInterstitial = localProps.getProperty("ADMOB_UNIT_INTERSTITIAL")
                ?: "ca-app-pub-3940256099942544/1033173712"
            val admobRewardRefresh = localProps.getProperty("ADMOB_UNIT_REWARD_REFRESH")
                ?: "ca-app-pub-3940256099942544/5224354917"
            val admobRewardOfflineX2 = localProps.getProperty("ADMOB_UNIT_REWARD_OFFLINE_X2")
                ?: "ca-app-pub-3940256099942544/5224354917"
            val admobRewardSkipWait = localProps.getProperty("ADMOB_UNIT_REWARD_SKIP_WAIT")
                ?: "ca-app-pub-3940256099942544/5224354917"
            val admobRewardUpgradeRevert = localProps.getProperty("ADMOB_UNIT_REWARD_UPGRADE_REVERT")
                ?: "ca-app-pub-3940256099942544/5224354917"

            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$admobInterstitial\"")
            buildConfigField("String", "AD_UNIT_REWARD_REFRESH", "\"$admobRewardRefresh\"")
            buildConfigField("String", "AD_UNIT_REWARD_OFFLINE_X2", "\"$admobRewardOfflineX2\"")
            buildConfigField("String", "AD_UNIT_REWARD_SKIP_WAIT", "\"$admobRewardSkipWait\"")
            buildConfigField("String", "AD_UNIT_REWARD_UPGRADE_REVERT", "\"$admobRewardUpgradeRevert\"")
            manifestPlaceholders["admobAppId"] = admobAppId
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // ViewModel Compose
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // WorkManager + Hilt integration
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // AdMob (UMP 동의 SDK 포함)
    implementation(libs.play.services.ads)
}