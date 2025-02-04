package net.k1ra.kotlin_qr_scanner.kmpImagePicker

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


fun Modifier.clickableSingle(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    var enabled by remember { mutableStateOf(true) }
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        enabled = enabled
    ) {
        if (enabled) {
            onClick()
            enabled = false

            CoroutineScope(Dispatchers.Default).launch {
                delay(350)
                enabled = true
            }
        }
    }
}

inline fun Modifier.noRippleClickable(
    isSingleClickable: Boolean = false,
    crossinline onClick: () -> Unit
): Modifier = composed {
    if (isSingleClickable) {
        clickableSingle(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onClick()
        }
    } else {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onClick()
        }
    }
}