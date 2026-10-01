plugins {
    id("amber.android.library")
    id("amber.hilt")
    id("amber.room")
}

android {
    namespace = "cn.floriax.amber.core.database"

    room {
        // Exported schemas: the versioned migration source of truth.
        schemaDirectory("$projectDir/schemas")
    }
}
