plugins {
    id("amber.android.library")
    id("amber.hilt")
}

android {
    namespace = "cn.floriax.amber.data.light"
}

dependencies {
    implementation(project(":core:database"))

    api(project(":domain:light"))
}
