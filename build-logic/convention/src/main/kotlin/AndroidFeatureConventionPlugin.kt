import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for feature modules.
 *
 * Configures Android Library + Hilt + Compose, plus the dependencies shared by every feature
 * module (core:common, core:navigation, shared:ui).
 *
 * Design note: feature modules are numerous, uniform in shape, and always need DI
 * (@AndroidEntryPoint / @HiltViewModel) and Compose, so both are bundled here.
 * Every feature also contributes navigation entries to the app's Nav3 entry
 * provider, so core:navigation is bundled here as well.
 * `amber.android.library` stays minimal and core/shared libraries opt in per module.
 *
 * Usage: `plugins { id("amber.android.feature") }`
 *
 * @author WangZhiYao
 * @since 2026/9/23
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "amber.android.library")
            apply(plugin = "amber.hilt")
            apply(plugin = "amber.compose")

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":shared:ui"))
            }
        }
    }
}
