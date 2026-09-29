plugins {
    id("amber.android.feature")
    id("amber.serialization")
}

android {
    namespace = "cn.floriax.amber.feature.settings"
}

dependencies {
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.paging.compose)
}
