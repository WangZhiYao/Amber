import cn.floriax.amber.buildlogic.configureKotlinAndroidApplication
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Convention plugin for the Android application module.
 *
 * Configures com.android.application + Hilt + Compose, compileSdk/minSdk/targetSdk,
 * applicationId/versionCode/versionName, Java 21 and the shared test dependencies.
 *
 * Note: applying this plugin also brings Hilt and Compose, so the module does not need to
 * declare them separately.
 *
 * Usage: `plugins { id("amber.android.application") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "amber.hilt")
            apply(plugin = "amber.compose")

            configureKotlinAndroidApplication()
        }
    }
}
