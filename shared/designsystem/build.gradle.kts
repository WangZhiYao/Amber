plugins {
    id("amber.android.library")
    id("amber.compose")
}

android {
    namespace = "cn.floriax.amber.shared.designsystem"
}

dependencies {
    api(libs.androidx.core.ktx)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.material.icons.extended)
}
