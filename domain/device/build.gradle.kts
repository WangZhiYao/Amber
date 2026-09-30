plugins {
    id("amber.android.library")
}

android {
    namespace = "cn.floriax.amber.domain.device"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    api(project(":domain:clock"))
    api(project(":domain:light"))
}
