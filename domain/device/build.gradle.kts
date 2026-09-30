plugins {
    id("amber.android.domain")
}

android {
    namespace = "cn.floriax.amber.domain.device"
}

dependencies {
    api(project(":domain:clock"))
    api(project(":domain:light"))
}
