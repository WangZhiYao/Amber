plugins {
    id("amber.android.library")
    id("amber.hilt")
}

android {
    namespace = "cn.floriax.amber.core.ble"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.nordic.ble.client)
    implementation(libs.nordic.ble.scanner)
}
