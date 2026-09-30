plugins {
    id("amber.android.library")
    id("amber.hilt")
}

android {
    namespace = "cn.floriax.amber.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
}
