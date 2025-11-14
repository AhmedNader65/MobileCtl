package com.mobilectl.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*

/**
 * Minimal bordered container - no elevation, just a subtle border
 */
@Composable
fun MinimalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Compact section header
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.sp
        )
        action?.invoke()
    }
}

/**
 * Compact stat display
 */
@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            ),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

/**
 * Simple table row
 */
@Composable
fun TableRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Status badge - minimal pill
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Compact form field
 */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        modifier = modifier,
        singleLine = singleLine,
        shape = RoundedCornerShape(8.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Compact dropdown
 */
@Composable
fun CompactDropdown(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by androidx.compose.runtime.remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label, fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.onSurface
            ),
            trailingIcon = {
                androidx.compose.material.icons.Icons.Default.ArrowDropDown.let { icon ->
                    Icon(icon, null, modifier = Modifier.size(20.dp))
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
