plugins {
    id("amber.android.application")
    id("amber.serialization")
}

dependencies {
    debugImplementation(libs.leakcanary.android)

    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(project(":shared:ui"))

    implementation(project(":data:device"))
    implementation(project(":data:light"))

    implementation(project(":feature:light"))
    implementation(project(":feature:clock"))
    implementation(project(":feature:settings"))

}
