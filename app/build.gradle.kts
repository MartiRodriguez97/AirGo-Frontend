import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("jacoco")
}

val localProps =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

fun localProperty(
    name: String,
    default: String = "",
): String {
    // local.properties en local; env vars en CI (GitHub Actions, etc.)
    return (localProps.getProperty(name) ?: System.getenv(name) ?: default).trim()
}

android {
    namespace = "com.airgo.airgoApp"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        val backendUrl = localProperty("BACKEND_URL", "http://10.0.2.2:8000")
        val googleWebClientId = localProperty("GOOGLE_WEB_CLIENT_ID", "")

        applicationId = "com.airgo.airgoApp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            // Esto es lo que activa la recolección de datos en el dispositivo
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes +=
                setOf(
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                )
        }
    }
}

// Configuració per generar un informe combinat de cobertura de tests unitaris i android test
tasks.register<JacocoReport>("jacocoCombinedReport") {
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
            exclude(fileFilter)
        },
    )

    sourceDirectories.setFrom(files("src/main/java"))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
            )
        },
    )
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.play.services.auth)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.googleid)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.foundation.layout)

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("io.mockk:mockk-android:1.14.9")
    androidTestImplementation("io.mockk:mockk-agent:1.14.9")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation(libs.androidx.material.icons.extended)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}
