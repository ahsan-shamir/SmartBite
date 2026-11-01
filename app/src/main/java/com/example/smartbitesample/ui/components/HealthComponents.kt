package com.example.smartbitesample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartbitesample.ui.theme.DeepHeritageOrange

// ─── Canonical 4 dietary constraints used across the entire app ───
val DIETARY_CONSTRAINTS = listOf(
    "Blood Pressure (BP)",
    "Diabetes",
    "High Cholesterol",
    "Alpha-gal Syndrome"
)

@Composable
fun DiseaseSelectionDialog(
    selectedDiseases: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // Local draft — only committed to state when "Update" is tapped
    var currentSelection by remember(selectedDiseases) { mutableStateOf(selectedDiseases) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colors.surface,
            elevation = 24.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── Header row with X (reset + close) on the RIGHT ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spacer to balance the row symmetrically
                    Spacer(modifier = Modifier.size(36.dp))

                    Text(
                        "Health Constraints",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepHeritageOrange
                    )

                    // X icon — resets to original selection and closes without saving
                    IconButton(
                        onClick = {
                            currentSelection = selectedDiseases // reset
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel and reset",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Apply filters to avoid foods that trigger your condition",
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // ─── 4 Constraint checkboxes ───
                DIETARY_CONSTRAINTS.forEach { option ->
                    val isChecked = currentSelection.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                currentSelection = if (isChecked)
                                    currentSelection - option
                                else
                                    currentSelection + option
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom checkbox box
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isChecked) DeepHeritageOrange
                                    else MaterialTheme.colors.onSurface.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        when (option) {
                            "Diabetes" -> {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Diabetes ")
                                        withStyle(style = SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
                                            append("(sugar)")
                                        }
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                            "Blood Pressure (BP)" -> {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Blood Pressure ")
                                        withStyle(style = SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
                                            append("(BP)")
                                        }
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                            "Alpha-gal Syndrome" -> {
                                Column {
                                    Text(
                                        text = "Alpha-gal Syndrome",
                                        fontSize = 18.sp,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                    Text(
                                        text = "(Red meat allergy)",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = option,
                                    fontSize = 18.sp,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                        }
                    }
                    Divider(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.07f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ─── Update button — horizontally centered and short ───
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { onConfirm(currentSelection) },
                        modifier = Modifier
                            .width(200.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DeepHeritageOrange)
                    ) {
                        Text(
                            "Update",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
