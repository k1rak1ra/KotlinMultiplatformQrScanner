import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

group = "net.k1ra.kotlin_qr_scanner"
version = System.getenv("releaseName") ?: "999999.999999.999999"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
        publishLibraryVariants("release", "debug")

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)

            dependencies {
                implementation(libs.androidx.ui.test.junit)
                debugImplementation(libs.androidx.ui.test.manifest)
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "KotlinQrScanner"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
    }

    jvm()

    sourceSets {
        val jvmMain by getting

        androidMain.dependencies {
            implementation(libs.accompanist.permissions)
            implementation(libs.androidx.activity.compose)
            implementation(libs.mlkit)
            implementation(libs.camera)
            implementation(libs.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            implementation(libs.zxing)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.pnc)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        jvmMain.dependencies {
            implementation(libs.desktop.jna)
            implementation(libs.desktop.zxing)
        }
        wasmJsMain.dependencies {
            implementation(npm("html5-qrcode", libs.versions.html5qrcode.get()))
        }
    }
}

android {
    namespace = "net.k1ra.kotlin_qr_scanner"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    repositories {
        maven {
            name = "k1ra-nexus"
            url = uri("https://k1ra.net/nexus/repository/public/")

            credentials(PasswordCredentials::class) {
                username = System.getenv("NEXUS_USERNAME") ?: "anonymous"
                password = System.getenv("NEXUS_PASSWORD") ?: ""
            }
        }
    }
}

tasks{
    register<Jar>("dokkaJar") {
        from(dokkaHtml)
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
    }
}
