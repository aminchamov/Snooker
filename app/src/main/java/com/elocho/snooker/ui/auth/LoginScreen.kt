package com.elocho.snooker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.ElOchoTextField
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.theme.*

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        DiagonalStripeBackground()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                AppLogo(height = 60.dp)

                Text(
                    text = "SNOOKER LOUNGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 6.sp,
                    color = Gold.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface.copy(alpha = 0.88f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SIGN IN",
                            style = MaterialTheme.typography.titleLarge,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        ElOchoTextField(
                            value = uiState.username,
                            onValueChange = onUsernameChange,
                            label = "Username",
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Gold.copy(alpha = 0.7f))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ElOchoTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChange,
                            label = "Password",
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Gold.copy(alpha = 0.7f))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = LightGray.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                }
                            )
                        )

                        if (uiState.loginError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.loginError,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        ElOchoButton(
                            text = "LOGIN",
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Burgundy
                        )
                    }
                }
            }
        }
    }
}
