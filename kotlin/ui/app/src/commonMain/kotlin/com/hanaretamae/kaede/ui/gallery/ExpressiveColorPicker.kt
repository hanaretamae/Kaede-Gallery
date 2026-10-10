package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
internal fun ExpressiveColorPicker(
    color: Color,
    label: String,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberColorPickerController()
    LaunchedEffect(controller, color) {
        controller.selectByColor(color, fromUser = false)
    }
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            HsvColorPicker(
                modifier = Modifier.size(216.dp),
                controller = controller,
                initialColor = color,
                onColorChanged = { onColorChanged(it.color) },
            )
            BrightnessSlider(
                modifier = Modifier.size(width = 216.dp, height = 48.dp),
                controller = controller,
                initialColor = color,
                onColorChanged = { onColorChanged(it.color) },
            )
        }
    }
}
