package com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Diálogo de gestión de cuenta y perfil en Supabase Cloud (SM-036).
 * Permite iniciar sesión, registrarse con email/contraseña/usuario, editar el perfil o cerrar sesión.
 */
@Composable
fun CloudAccountDialog(
    authState: AuthState,
    onDismiss: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, username: String, displayName: String?) -> Unit,
    onUpdateProfile: (username: String, displayName: String?) -> Unit,
    onSignOut: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember(authState) {
        mutableStateOf((authState as? AuthState.Authenticated)?.session?.user?.username.orEmpty())
    }
    var displayName by remember(authState) {
        mutableStateOf((authState as? AuthState.Authenticated)?.session?.user?.displayName.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StitchMeshSurfaceContainer,
        title = {
            Text(
                text = when (authState) {
                    is AuthState.Authenticated -> "Perfil de Cuenta Supabase"
                    else -> if (isRegisterMode) "Crear Cuenta en Supabase" else "Iniciar Sesión en Supabase"
                },
                style = MaterialTheme.typography.titleMedium,
                color = StitchMeshTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (authState) {
                    is AuthState.Authenticated -> {
                        Text(
                            text = "Conectado como ${authState.session.user.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchMeshSageGreen
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Nombre de usuario") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_profile_username_input")
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nombre visible") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_profile_display_name_input")
                        )
                    }

                    else -> {
                        if (authState is AuthState.Error) {
                            Text(
                                text = authState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchMeshCoralRed
                            )
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Nombre de usuario (@handle)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_username_input")
                            )
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Nombre para mostrar (opcional)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_display_name_input")
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        TextButton(
                            onClick = { isRegisterMode = !isRegisterMode },
                            modifier = Modifier.testTag("auth_toggle_mode_button")
                        ) {
                            Text(
                                text = if (isRegisterMode) {
                                    "¿Ya tienes cuenta? Inicia sesión"
                                } else {
                                    "¿No tienes cuenta? Regístrate"
                                },
                                color = StitchMeshTextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (authState) {
                is AuthState.Authenticated -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("auth_sign_out_button")
                        ) {
                            Text("Cerrar sesión", color = StitchMeshCoralRed)
                        }
                        Button(
                            onClick = { onUpdateProfile(username, displayName.ifBlank { null }) },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta),
                            modifier = Modifier.testTag("auth_save_profile_button")
                        ) {
                            Text("Guardar perfil")
                        }
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                onSignUp(email, password, username, displayName.ifBlank { null })
                            } else {
                                onSignIn(email, password)
                            }
                        },
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta),
                        modifier = Modifier.testTag("auth_submit_button")
                    ) {
                        Text(if (isRegisterMode) "Registrar" else "Entrar")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
