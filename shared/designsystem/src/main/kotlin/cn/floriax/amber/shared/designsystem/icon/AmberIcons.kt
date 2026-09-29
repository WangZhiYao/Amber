package cn.floriax.amber.shared.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * Single source of Amber app icons, so feature modules don't import
 * material-icons paths directly.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
object AmberIcons {
    /** Top bar back arrow. */
    val ArrowBack = Icons.AutoMirrored.Filled.ArrowBack
}
