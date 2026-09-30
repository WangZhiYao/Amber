import cn.floriax.amber.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for domain modules.
 *
 * Configures Android Library plus the two dependencies every domain module needs:
 * - kotlinx-coroutines: repository contracts expose `Flow`/`StateFlow` in their
 *   signatures, so it is `api` (consumers must resolve those types);
 * - javax.inject: use cases are constructor-injected. Only the annotations are
 *   pulled in — the domain layer stays free of any DI framework.
 *
 * Design note: no Hilt, no Compose, no Android UI. Domain modules are pure
 * business code; `amber.android.library` stays minimal and layers opt in.
 *
 * Usage: `plugins { id("amber.android.domain") }`
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class AndroidDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "amber.android.library")

            dependencies {
                "api"(libs.findLibrary("kotlinx-coroutines-core").get())
                "api"(libs.findLibrary("javax-inject").get())
            }
        }
    }
}
