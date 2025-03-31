package net.lashua.zonedit.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
fun GridDimensionField(
    gridCount: Int,
    onGridCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minGrids: Int = 1,
    maxGrids: Int = 500
) {
    var textValue by remember(gridCount) { mutableStateOf(gridCount.toString()) }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
            textValue = input // Always update the text
            if (input.isEmpty()) {
                onGridCountChange(minGrids)
            } else {
                input.toIntOrNull()?.let { value ->
                    if (value >= minGrids) {
                        onGridCountChange(value.coerceIn(minGrids, maxGrids))
                    }
                }
            }
        },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            Text(
                "grids",
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}