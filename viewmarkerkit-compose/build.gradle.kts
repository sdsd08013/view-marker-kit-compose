plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "io.github.sdsd08013.viewmarkerkit.compose"

    // Compose 1.12 requires API 37
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // api: types exposed in the public API
    api(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.google.maps.services)
    api(libs.maps.compose)

    testImplementation(libs.junit)
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.vintage)
    testImplementation(libs.kotest.runner)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.sdsd08013"
            artifactId = "view-marker-kit-compose"
            version = providers.gradleProperty("version").orElse("0.0.1-SNAPSHOT").get()

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
