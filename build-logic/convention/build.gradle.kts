import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "cn.floriax.amber.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    // Compile-time only: at runtime these Gradle plugins are provided by the root project's
    // `plugins { ... apply false }` block.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "amber.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "amber.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "amber.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidDomain") {
            id = "amber.android.domain"
            implementationClass = "AndroidDomainConventionPlugin"
        }
        register("compose") {
            id = "amber.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("hilt") {
            id = "amber.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("room") {
            id = "amber.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("serialization") {
            id = "amber.serialization"
            implementationClass = "SerializationConventionPlugin"
        }
    }
}
