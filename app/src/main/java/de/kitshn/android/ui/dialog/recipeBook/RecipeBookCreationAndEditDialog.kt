package de.kitshn.android.ui.dialog.recipeBook

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.kitshn.android.R
import de.kitshn.android.api.tandoor.TandoorClient
import de.kitshn.android.api.tandoor.model.TandoorRecipeBook
import de.kitshn.android.api.tandoor.rememberTandoorRequestState
import de.kitshn.android.model.form.KitshnForm
import de.kitshn.android.model.form.KitshnFormSection
import de.kitshn.android.model.form.item.field.KitshnFormTextFieldItem
import de.kitshn.android.ui.TandoorRequestErrorHandler
import de.kitshn.android.ui.dialog.AdaptiveFullscreenDialog
import de.kitshn.android.ui.state.foreverRememberNotSavable
import kotlinx.coroutines.launch

data class RecipeBookCreationAndEditDefaultValues(
    val name: String = "",
    val description: String = ""
)

@Composable
fun rememberRecipeBookEditDialogState(
    key: String
): RecipeBookEditDialogState {
    val value by foreverRememberNotSavable(
        key = key,
        initialValue = RecipeBookEditDialogState()
    )

    return value
}

class RecipeBookEditDialogState(
    val shown: MutableState<Boolean> = mutableStateOf(false)
) {

    var defaultValues = RecipeBookCreationAndEditDefaultValues()
    var recipeBook by mutableStateOf<TandoorRecipeBook?>(null)

    fun open(recipeBook: TandoorRecipeBook) {
        this.recipeBook = recipeBook

        this.defaultValues = RecipeBookCreationAndEditDefaultValues(
            name = recipeBook.name,
            description = recipeBook.description
        )

        this.shown.value = true
    }

    fun dismiss() {
        this.shown.value = false
    }
}

@Composable
fun rememberRecipeBookCreationDialogState(
    key: String
): RecipeBookCreationDialogState {
    val value by foreverRememberNotSavable(
        key = key,
        initialValue = RecipeBookCreationDialogState()
    )

    return value
}

class RecipeBookCreationDialogState(
    val shown: MutableState<Boolean> = mutableStateOf(false)
) {

    var defaultValues = RecipeBookCreationAndEditDefaultValues()

    fun open(values: RecipeBookCreationAndEditDefaultValues = RecipeBookCreationAndEditDefaultValues()) {
        this.defaultValues = values
        this.shown.value = true
    }

    fun dismiss() {
        this.shown.value = false
    }
}

@Composable
fun RecipeBookCreationAndEditDialog(
    client: TandoorClient,
    creationState: RecipeBookCreationDialogState? = null,
    editState: RecipeBookEditDialogState? = null,
    onRefresh: () -> Unit
) {
    if(creationState?.shown?.value != true && editState?.shown?.value != true) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultValues =
        if(creationState?.shown?.value == true) creationState.defaultValues else editState?.defaultValues
    if(defaultValues == null) return

    val isEditDialog = editState?.shown?.value == true

    // form values
    var name by rememberSaveable { mutableStateOf(defaultValues.name) }
    var description by rememberSaveable { mutableStateOf(defaultValues.description) }

    val requestRecipeBookState = rememberTandoorRequestState()

    val form = remember {
        KitshnForm(
            sections = listOf(
                KitshnFormSection(
                    listOf(
                        KitshnFormTextFieldItem(
                            value = { name },
                            onValueChange = {
                                name = it
                            },

                            label = { Text(stringResource(id = R.string.common_name)) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Label,
                                    stringResource(id = R.string.common_name)
                                )
                            },

                            optional = false,

                            check = {
                                if(it.length > 128) {
                                    context.getString(R.string.common_name)
                                } else {
                                    null
                                }
                            }
                        )
                    )
                ),
                KitshnFormSection(
                    listOf(
                        KitshnFormTextFieldItem(
                            value = { description },
                            onValueChange = {
                                description = it
                            },

                            label = { Text(stringResource(id = R.string.common_description)) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Notes,
                                    stringResource(id = R.string.common_description)
                                )
                            },

                            optional = true,

                            check = { null }
                        )
                    )
                ),
            ),
            submitButton = {
                Button(onClick = it) {
                    Text(
                        text = if(isEditDialog) {
                            stringResource(id = R.string.action_save)
                        } else {
                            stringResource(id = R.string.action_create)
                        }
                    )
                }
            },
            onSubmit = {
                coroutineScope.launch {
                    if(isEditDialog) {
                        requestRecipeBookState.wrapRequest {
                            editState?.recipeBook?.partialUpdate(
                                name = name,
                                description = description
                            )
                        }

                        onRefresh()
                        editState?.dismiss()
                    } else {
                        val recipeBook = requestRecipeBookState.wrapRequest {
                            client.recipeBook.create(
                                name = name,
                                description = description
                            )
                        }

                        if(recipeBook != null) {
                            onRefresh()
                            creationState?.dismiss()
                        }
                    }
                }
            }
        )
    }

    AdaptiveFullscreenDialog(
        onDismiss = {
            creationState?.dismiss()
            editState?.dismiss()
        },
        title = {
            Text(
                text = if(isEditDialog) {
                    stringResource(R.string.action_edit_recipe_book)
                } else {
                    stringResource(R.string.action_create_recipe_book)
                }
            )
        },
        actions = {
            form.RenderSubmitButton()
        }
    ) { it, _ ->
        form.Render(it)
    }

    TandoorRequestErrorHandler(state = requestRecipeBookState)
}