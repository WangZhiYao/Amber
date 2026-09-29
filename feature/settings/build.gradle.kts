plugins {
    id("amber.android.feature")
    id("amber.serialization")
}

android {
    namespace = "cn.floriax.amber.feature.settings"
}

dependencies {
    implementation(libs.androidx.paging.compose)
}
