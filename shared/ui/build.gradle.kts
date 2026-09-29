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
}
