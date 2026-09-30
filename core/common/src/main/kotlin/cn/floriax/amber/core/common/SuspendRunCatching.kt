package cn.floriax.amber.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Coroutine-safe runCatching: rethrows CancellationException instead of
 * swallowing cancellation; everything else becomes Result.failure.
 * The common building block of the "repositories never throw business
 * exceptions" contract.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
suspend inline fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
