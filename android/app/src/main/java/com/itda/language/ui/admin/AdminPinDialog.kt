package com.itda.language.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itda.language.ui.theme.*

@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("관리자 인증", color = BaseText) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("PIN 입력") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrangeCoral,
                        cursorColor = WarmOrangeCoral,
                    ),
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, color = Error, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 4 && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("확인", color = WarmOrangeCoral)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
