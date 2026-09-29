plugins {
    id("amber.android.application")
    id("amber.serialization")
}

dependencies {
    debugImplementation(libs.leakcanary.android)

    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(project(":shared:ui"))

    implementation(project(":feature:light"))
    implementation(project(":feature:clock"))
    implementation(project(":feature:settings"))

}
