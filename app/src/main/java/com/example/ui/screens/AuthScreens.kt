package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DipuBorder
import com.example.ui.theme.DipuCyan
import com.example.ui.theme.DipuDarkBg
import com.example.ui.theme.DipuSurfaceCard
import com.example.ui.theme.DipuSurfaceElevated
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    onLoginAttempt: (String, String, (Boolean, String) -> Unit) -> Unit,
    onRegisterAttempt: (String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("poiuytrewqasdfghjklmnbvc234@gmail.com") }
    var password by remember { mutableStateOf("••••••••••••") }
    var name by remember { mutableStateOf("Dipu Administrator") }
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DipuDarkBg)
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "𝙳𝚒𝚙𝚞 𝙿𝚛𝚘𝚡𝚢 𝚉𝚘𝚗𝚎",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DipuCyan,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Multi-Terminal Monitoring Network",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Two Mobile Sync Callout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = DipuCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dual-Phone Realtime Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Use this account on Phone 1 and Phone 2. Both will receive identical payments in realtime.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Auth Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DipuBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DipuSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN -> "Account Access"
                            AuthMode.REGISTER -> "Create Terminal Account"
                            AuthMode.FORGOT_PASSWORD -> "Recover Access"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DipuCyan) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DipuCyan,
                                unfocusedBorderColor = DipuBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Admin Email", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = DipuCyan) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DipuCyan,
                            unfocusedBorderColor = DipuBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (mode != AuthMode.FORGOT_PASSWORD) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DipuCyan) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DipuCyan,
                                unfocusedBorderColor = DipuBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when (mode) {
                                AuthMode.LOGIN -> {
                                    onLoginAttempt(email, password) { success, msg ->
                                        if (success) {
                                            onLoginSuccess()
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    }
                                }
                                AuthMode.REGISTER -> {
                                    onRegisterAttempt(name, email, password) { success, msg ->
                                        if (success) {
                                            onLoginSuccess()
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    }
                                }
                                AuthMode.FORGOT_PASSWORD -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Password reset instructions sent to $email")
                                        mode = AuthMode.LOGIN
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DipuCyan, contentColor = Color(0xFF002227))
                    ) {
                        Text(
                            text = when (mode) {
                                AuthMode.LOGIN -> "SIGN IN TO TERMINAL"
                                AuthMode.REGISTER -> "REGISTER ACCOUNT"
                                AuthMode.FORGOT_PASSWORD -> "SEND RESET LINK"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (mode == AuthMode.LOGIN) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                color = DipuCyan,
                                modifier = Modifier.clickable { mode = AuthMode.FORGOT_PASSWORD }
                            )
                            Text(
                                text = "Register New Account",
                                fontSize = 12.sp,
                                color = DipuCyan,
                                modifier = Modifier.clickable { mode = AuthMode.REGISTER }
                            )
                        } else {
                            Text(
                                text = "Back to Sign In",
                                fontSize = 12.sp,
                                color = DipuCyan,
                                modifier = Modifier.clickable { mode = AuthMode.LOGIN }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
