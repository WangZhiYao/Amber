plugins {
    id("amber.android.library")
    id("amber.compose")
}

android {
    namespace = "cn.floriax.amber.shared.ui"
}

dependencies {
    api(project(":shared:designsystem"))

    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.navigation3.runtime)

    // BLE permission helpers (ContextCompat). Deliberately business-free:
    // this module is UI infrastructure and must not know about any domain.
    implementation(libs.androidx.core.ktx)
}
