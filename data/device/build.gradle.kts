plugins {
    id("amber.android.library")
    id("amber.hilt")
}

android {
    namespace = "cn.floriax.amber.data.device"
}

dependencies {
    implementation(project(":core:ble"))
    implementation(project(":core:common"))

    api(project(":domain:device"))
    api(project(":domain:clock"))
    api(project(":domain:light"))
}
