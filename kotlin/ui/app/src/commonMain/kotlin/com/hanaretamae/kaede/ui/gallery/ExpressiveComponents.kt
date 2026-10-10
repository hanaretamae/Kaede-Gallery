@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun GalleryPlaybackActions(
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 560.dp) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) { leading() }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) { trailing() }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading()
                Spacer(Modifier.weight(1f))
                trailing()
            }
        }
    }
}

// Keep speed selection in the player scene: native popup windows contend with Swing video interop.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryPlaybackSpeedChoices(
    selected: Float,
    onSelect: (Float) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                    ToggleButton(
                        checked = speed == selected,
                        onCheckedChange = { onSelect(speed) },
                    ) { Text("${speed}x") }
                }
            }
        }
    }
}

/** Material 3 Expressive menu: a popup holding one rounded group of segmented items. */
@Composable
fun KaedeMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1), content = content)
    }
}

@Composable
fun KaedeMenuItem(
    text: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    DropdownMenuItem(
        onClick = onClick,
        text = { Text(text) },
        shape = MenuDefaults.itemShape(index, count).shape,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
    )
}

@Composable
fun KaedeSelectableMenuItem(
    text: String,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    SelectableDropdownMenuItem(
        selected = selected,
        onClick = onClick,
        text = { Text(text) },
        shapes = MenuDefaults.itemShape(index, count),
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        selectedLeadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaedeTooltip(
    label: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        content()
    }
}

@Composable
fun KaedeIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    KaedeTooltip(label) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
        ) { content() }
    }
}

/** Connected button group for single choice (Expressive replacement for segmented buttons). */
@Composable
internal fun <T> ConnectedChoiceGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    iconFor: ((T) -> ImageVector?)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                iconFor?.invoke(option)?.let { icon ->
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = ButtonDefaults.IconSpacing)
                            .size(ButtonDefaults.IconSize),
                    )
                }
                AnimatedVisibility(visible = isSelected && iconFor == null) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = ButtonDefaults.IconSpacing)
                            .size(ButtonDefaults.IconSize),
                    )
                }
                Text(label(option), maxLines = 1)
            }
        }
    }
}
