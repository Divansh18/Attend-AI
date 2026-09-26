package com.divanshgandhi.attendai.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.model.UserRole

@Composable
fun LoginScreen(
    state: LoginUiState,
    onRoleChange: (UserRole) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    var passwordVisible by remember(state.role) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSignIn()
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.sign_in_subtitle), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = state.role == role,
                            onClick = { onRoleChange(role) },
                            enabled = !state.isSubmitting,
                            label = {
                                Text(stringResource(if (role == UserRole.ADMIN) R.string.admin else R.string.staff))
                            },
                        )
                    }
                }
                if (state.role == UserRole.STAFF && state.error != LoginError.STAFF_UNAVAILABLE) {
                    Text(stringResource(R.string.staff_sign_in_unavailable))
                }
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(if (state.role == UserRole.ADMIN) R.string.username else R.string.employee_id))
                    },
                    enabled = !state.isSubmitting,
                    singleLine = true,
                    isError = state.isUsernameMissing,
                    supportingText = if (state.isUsernameMissing) {
                        { Text(stringResource(R.string.username_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password)) },
                    enabled = !state.isSubmitting,
                    singleLine = true,
                    isError = state.isPasswordMissing,
                    supportingText = if (state.isPasswordMissing) {
                        { Text(stringResource(R.string.password_required)) }
                    } else null,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                state.error?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                LoginError.INVALID_CREDENTIALS -> R.string.invalid_credentials
                                LoginError.STAFF_UNAVAILABLE -> R.string.staff_sign_in_unavailable
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Button(
                    onClick = submit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (state.isSubmitting) R.string.signing_in else R.string.sign_in))
                }
            }
        }
    }
}
