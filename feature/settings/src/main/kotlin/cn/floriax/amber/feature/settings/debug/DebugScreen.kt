package cn.floriax.amber.feature.settings.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.floriax.amber.core.ble.protocol.toHexDisplay
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.feature.settings.R
import cn.floriax.amber.shared.designsystem.component.AmberTopBar
import cn.floriax.amber.shared.ui.ext.collectSideEffect
import cn.floriax.amber.shared.ui.ext.collectState
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Debug panel: frame log (auto-scrolling) and manual frame sending.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = viewModel(),
) {
    val state by viewModel.collectState()
    val context = LocalContext.current
    val resources = LocalResources.current
    val listState = rememberLazyListState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is DebugSideEffect.Copied -> {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("amber_logs", effect.text))
                Toast.makeText(
                    context,
                    resources.getString(R.string.debug_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            DebugSideEffect.Sent ->
                Toast.makeText(
                    context,
                    resources.getString(R.string.debug_sent),
                    Toast.LENGTH_SHORT
                ).show()

            DebugSideEffect.SendFailed ->
                Toast.makeText(
                    context,
                    resources.getString(R.string.debug_send_failed),
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    // Follow the newest entry.
    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) listState.animateScrollToItem(state.logs.size - 1)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AmberTopBar(
                title = stringResource(R.string.debug_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.cd_back),
                actions = {
                    OutlinedButton(
                        onClick = viewModel::onCopyLogs,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(stringResource(R.string.debug_copy))
                    }
                },
            )
        },
        // Status bar inset is consumed by the outer Scaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    items(state.logs) { log ->
                        LogLine(log)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.debug_manual_send),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.input,
                            onValueChange = viewModel::onInputChange,
                            isError = state.inputError == true,
                            singleLine = true,
                            placeholder = {
                                // A complete example of the expected format
                                // (the query frame), forced single-line — the
                                // placeholder slot ignores singleLine on the
                                // field itself.
                                Text(
                                    stringResource(R.string.debug_input_placeholder),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                )
                            },
                            supportingText = {
                                Text(
                                    stringResource(R.string.debug_input_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Button(onClick = viewModel::onSend) {
                            Text(stringResource(R.string.debug_send))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One frame-log line: time, direction, hex, label — colored by direction.
 */
@Composable
private fun LogLine(log: FrameLog) {
    val time = SimpleDateFormat(
        "HH:mm:ss.SSS",
        LocalLocale.current.platformLocale
    ).format(Date(log.timestamp))
    val color = when (log.direction) {
        FrameLog.Direction.TX -> MaterialTheme.colorScheme.primary
        FrameLog.Direction.RX -> MaterialTheme.colorScheme.tertiary
        FrameLog.Direction.SYS -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        "$time ${log.direction} ${log.bytes?.toHexDisplay() ?: ""} ${log.text}",
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
