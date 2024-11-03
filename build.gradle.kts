buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        content {
            includeGroup("com.jillesvangurp")
            includeGroup("com.github.jillesvangurp")
            includeGroup("com.tryformation")
            includeGroup("com.tryformation.fritz2")
        }
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    js(IR) {
        browser()
    }.binaries.executable()

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalStdlibApi")
            }
        }
        commonMain {
            dependencies {
                implementation("dev.fritz2:core:_")
                implementation("dev.fritz2:headless:_")
                implementation("com.jillesvangurp:rankquest-core:_")
                implementation("com.jillesvangurp:kotlinx-serialization-extensions:_")
                implementation("com.github.jillesvangurp:querylight:_")
                implementation("com.soywiz.korlibs.krypto:krypto:_")
                implementation("org.jetbrains:markdown:_")
                implementation(KotlinX.serialization.json)
                implementation(Koin.core)
            }
        }
        jsMain {
            dependencies {
                // tailwind
                implementation(npm("tailwindcss", "_"))
                implementation(npm("@tailwindcss/forms", "_"))

                // fluent-js
                implementation("com.tryformation:fluent-kotlin:_")

                // webpack
                implementation(devNpm("postcss", "_"))
                implementation(devNpm("postcss-loader", "_"))
                implementation(devNpm("autoprefixer", "_"))
                implementation(devNpm("css-loader", "_"))
                implementation(devNpm("style-loader", "_"))
                implementation(devNpm("cssnano", "_"))
            }
        }
    }
}