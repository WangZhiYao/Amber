plugins {
    id("amber.android.feature")
    id("amber.serialization")
}

android {
    namespace = "cn.floriax.amber.feature.clock"
}

dependencies {
    implementation(libs.androidx.paging.compose)

    implementation(project(":domain:device"))
    implementation(project(":domain:clock"))
}
