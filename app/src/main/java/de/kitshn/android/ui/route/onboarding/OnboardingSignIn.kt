package de.kitshn.android.ui.route.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.kitshn.android.R
import de.kitshn.android.api.tandoor.TandoorClient
import de.kitshn.android.api.tandoor.TandoorCredentials
import de.kitshn.android.ui.component.buttons.LoadingExtendedFloatingActionButton
import de.kitshn.android.ui.modifier.autofill
import de.kitshn.android.ui.route.RouteParameters
import de.kitshn.android.ui.state.ErrorLoadingSuccessState
import de.kitshn.android.ui.theme.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RouteOnboardingSignIn(
    p: RouteParameters
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()

    var instanceUrlValue by rememberSaveable { mutableStateOf("https://app.tandoor.dev") }
    var instanceUrlState by rememberSaveable { mutableStateOf(ErrorLoadingSuccessState.SUCCESS) }
    val instanceUrlFocusRequester = remember { FocusRequester() }

    LaunchedEffect(instanceUrlValue) {
        instanceUrlState = ErrorLoadingSuccessState.LOADING
        delay(1000)

        instanceUrlState = if(TandoorClient(context, TandoorCredentials(instanceUrlValue))
                .testConnection(ignoreAuth = true)
        ) {
            ErrorLoadingSuccessState.SUCCESS
        } else {
            ErrorLoadingSuccessState.ERROR
        }
    }

    var usernameValue by rememberSaveable { mutableStateOf("") }
    var passwordValue by rememberSaveable { mutableStateOf("") }
    var loginState by rememberSaveable { mutableStateOf<ErrorLoadingSuccessState?>(null) }

    LaunchedEffect(usernameValue, passwordValue) {
        loginState = null
    }

    fun done() {
        coroutineScope.launch {
            loginState = ErrorLoadingSuccessState.LOADING

            val credentials = TandoorCredentials(
                instanceUrl = instanceUrlValue,
                username = usernameValue,
                password = passwordValue
            )

            p.vm.tandoorClient = TandoorClient(
                context,
                credentials
            )

            val token = p.vm.tandoorClient!!.login()
            if(token != null) {
                credentials.token = token

                loginState = ErrorLoadingSuccessState.SUCCESS
                p.vm.settings.saveTandoorCredentials(credentials)

                p.vm.navHostController?.navigate("onboarding/welcome")
                return@launch
            }

            loginState = ErrorLoadingSuccessState.ERROR
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_sign_in_title)) }
            )
        },
        floatingActionButton = {
            LoadingExtendedFloatingActionButton(
                loading = loginState == ErrorLoadingSuccessState.LOADING,
                text = { Text(text = stringResource(R.string.action_sign_in)) },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Login,
                        stringResource(R.string.action_sign_in)
                    )
                },
                onClick = { done() }
            )
        }
    ) {
        LazyColumn(
            Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            item {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(instanceUrlFocusRequester),

                        label = { Text(stringResource(R.string.common_instance_url)) },
                        placeholder = { Text("https://app.tandoor.dev") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Web,
                                stringResource(R.string.common_instance_url)
                            )
                        },

                        trailingIcon = {
                            when(instanceUrlState) {
                                ErrorLoadingSuccessState.ERROR -> Icon(
                                    Icons.Rounded.ErrorOutline,
                                    stringResource(
                                        R.string.common_not_reachable
                                    ), Modifier, MaterialTheme.colorScheme.error
                                )

                                ErrorLoadingSuccessState.LOADING -> Icon(
                                    Icons.Rounded.Refresh,
                                    stringResource(
                                        R.string.common_loading_short
                                    )
                                )

                                ErrorLoadingSuccessState.SUCCESS -> Icon(
                                    Icons.Rounded.Check,
                                    stringResource(
                                        R.string.common_reachable
                                    ), Modifier, Success
                                )
                            }
                        },
                        isError = instanceUrlState == ErrorLoadingSuccessState.ERROR,
                        supportingText = {
                            if(instanceUrlState == ErrorLoadingSuccessState.ERROR)
                                Text(stringResource(R.string.onboarding_sign_in_error_instance_not_reachable))
                        },

                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),

                        value = instanceUrlValue,
                        onValueChange = { value ->
                            instanceUrlValue = value
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    HorizontalDivider()

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxSize()
                            .autofill(
                                autofillTypes = listOf(AutofillType.Username),
                                onFill = { v -> usernameValue = v }
                            ),

                        enabled = instanceUrlState == ErrorLoadingSuccessState.SUCCESS,
                        label = { Text(stringResource(R.string.common_username)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.AccountCircle,
                                stringResource(R.string.common_username)
                            )
                        },

                        isError = loginState == ErrorLoadingSuccessState.ERROR,
                        supportingText = {
                            if(loginState == ErrorLoadingSuccessState.ERROR)
                                Text(stringResource(R.string.onboarding_sign_in_error_sign_in_failed))
                        },

                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),

                        value = usernameValue,
                        onValueChange = { value ->
                            usernameValue = value
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxSize()
                            .autofill(
                                autofillTypes = listOf(AutofillType.Password),
                                onFill = { v -> passwordValue = v }
                            ),

                        enabled = instanceUrlState == ErrorLoadingSuccessState.SUCCESS,
                        label = { Text(stringResource(R.string.common_password)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Password,
                                stringResource(R.string.common_password)
                            )
                        },

                        isError = loginState == ErrorLoadingSuccessState.ERROR,
                        supportingText = {
                            if(loginState == ErrorLoadingSuccessState.ERROR)
                                Text(stringResource(R.string.onboarding_sign_in_error_sign_in_failed))
                        },

                        visualTransformation = PasswordVisualTransformation(),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                done()
                                focusManager.clearFocus()
                            }
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),

                        value = passwordValue,
                        onValueChange = { value ->
                            passwordValue = value
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        instanceUrlFocusRequester.requestFocus()
    }
}