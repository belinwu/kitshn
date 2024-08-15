package de.kitshn.android.ui.dialog.select

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.kitshn.android.R
import de.kitshn.android.api.tandoor.TandoorClient
import de.kitshn.android.api.tandoor.TandoorRequestStateState
import de.kitshn.android.api.tandoor.model.TandoorKeyword
import de.kitshn.android.api.tandoor.rememberTandoorRequestState
import de.kitshn.android.reachedBottom
import de.kitshn.android.ui.TandoorRequestErrorHandler
import de.kitshn.android.ui.component.alert.FullSizeAlertPane
import de.kitshn.android.ui.layout.ResponsiveSideBySideLayout
import de.kitshn.android.ui.view.home.search.HOME_SEARCH_PAGING_SIZE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberSelectMultipleKeywordsDialogState(): SelectMultipleKeywordsDialogState {
    return remember {
        SelectMultipleKeywordsDialogState()
    }
}

class SelectMultipleKeywordsDialogState(
    val shown: MutableState<Boolean> = mutableStateOf(false)
) {
    val selectedKeywords = mutableStateListOf<TandoorKeyword>()

    fun open(selectedKeywords: List<TandoorKeyword>) {
        this.selectedKeywords.clear()
        this.selectedKeywords.addAll(selectedKeywords)

        this.shown.value = true
    }

    fun dismiss() {
        this.shown.value = false
    }
}

@Composable
fun SelectMultipleKeywordsDialog(
    client: TandoorClient,
    state: SelectMultipleKeywordsDialogState,
    prepend: @Composable () -> Unit = {},
    hideKeywordCreation: Boolean = false,
    onSubmit: (keywords: List<TandoorKeyword>) -> Unit
) {
    if(!state.shown.value) return

    AlertDialog(
        modifier = Modifier.padding(16.dp),
        onDismissRequest = {
            state.dismiss()
        },
        icon = {
            Icon(Icons.Rounded.Tag, stringResource(R.string.search_tags_filter))
        },
        title = {
            Text(stringResource(R.string.search_tags_filter))
        },
        text = {
            Column {
                prepend()

                BoxWithConstraints(
                    Modifier.padding(16.dp)
                ) {
                    ResponsiveSideBySideLayout(
                        showDivider = true,

                        leftMinWidth = 200.dp,
                        rightMinWidth = 200.dp,

                        maxHeight = 800.dp,

                        leftLayout = { enoughSpace ->
                            Box(
                                Modifier.height(
                                    if(enoughSpace)
                                        this@BoxWithConstraints.maxHeight
                                    else
                                        (this@BoxWithConstraints.maxHeight - 32.dp) / 2f
                                ),
                            ) {
                                KeywordSearchBar(
                                    modifier = Modifier.fillMaxHeight(),
                                    client = client,
                                    hideKeywordCreation = hideKeywordCreation,
                                    selectedKeywords = state.selectedKeywords
                                ) { keyword, keywordId, value ->
                                    if(value) {
                                        state.selectedKeywords.add(0, keyword)
                                    } else {
                                        state.selectedKeywords.removeIf { it.id == keywordId }
                                    }
                                }
                            }
                        }
                    ) {
                        Box(
                            Modifier.fillMaxHeight()
                        ) {
                            if(state.selectedKeywords.size == 0) {
                                FullSizeAlertPane(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = stringResource(R.string.search_tags_filter_empty),
                                    text = stringResource(R.string.search_tags_filter_empty)
                                )
                            } else {
                                LazyColumn(
                                    Modifier.clip(RoundedCornerShape(16.dp))
                                ) {
                                    items(
                                        state.selectedKeywords.size,
                                        key = { state.selectedKeywords[it].id }) {
                                        val keyword = state.selectedKeywords[it]

                                        KeywordCheckedListItem(
                                            checked = true,
                                            keyword = keyword
                                        ) {
                                            state.selectedKeywords.remove(keyword)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                state.dismiss()
                onSubmit(state.selectedKeywords)
            }) {
                Text(stringResource(id = R.string.action_apply))
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
}

@Composable
fun KeywordCheckedListItem(
    modifier: Modifier = Modifier,
    checked: Boolean,
    keyword: TandoorKeyword,
    onCheckedChange: (value: Boolean) -> Unit
) {
    ListItem(
        modifier = modifier
            .clickable {
                onCheckedChange(!checked)
            },
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    onCheckedChange(it)
                }
            )
        },
        headlineContent = {
            Text(keyword.label)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordSearchBar(
    modifier: Modifier = Modifier,
    client: TandoorClient,
    selectedKeywords: List<TandoorKeyword>,
    hideKeywordCreation: Boolean = false,
    onCheckedChange: (keyword: TandoorKeyword, keywordId: Int, value: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var query by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(250)
        search = query
    }

    val selectedKeywordIds = remember { mutableStateListOf<Int>() }
    LaunchedEffect(selectedKeywords.toList()) {
        selectedKeywordIds.clear()
        selectedKeywordIds.addAll(selectedKeywords.map { it.id })
    }

    val searchRequestState = rememberTandoorRequestState()
    val extendedSearchRequestState = rememberTandoorRequestState()

    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var nextPageExists by rememberSaveable { mutableStateOf(false) }

    val searchResults = remember { mutableStateListOf<TandoorKeyword>() }
    LaunchedEffect(search) {
        currentPage = 1

        searchRequestState.wrapRequest {
            client.keyword.list(
                query = search,
                pageSize = HOME_SEARCH_PAGING_SIZE,
            )
        }?.let {
            nextPageExists = it.next != null

            searchResults.clear()
            searchResults.addAll(it.results)
        }
    }

    val searchLazyListState = rememberLazyListState()
    val reachedBottom by remember { derivedStateOf { searchLazyListState.reachedBottom(buffer = 3) } }

    LaunchedEffect(reachedBottom) {
        if(searchRequestState.state != TandoorRequestStateState.SUCCESS) return@LaunchedEffect
        if(extendedSearchRequestState.state == TandoorRequestStateState.LOADING) return@LaunchedEffect
        if(!nextPageExists) return@LaunchedEffect

        currentPage++
        extendedSearchRequestState.wrapRequest {
            client.keyword.list(
                query = search,
                pageSize = HOME_SEARCH_PAGING_SIZE,
                page = currentPage
            )
        }?.let {
            nextPageExists = it.next != null
            searchResults.addAll(it.results)
        }
    }

    var showKeywordCreationItem by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(searchResults.toList()) {
        showKeywordCreationItem = if(query.length > 2) {
            searchResults.getOrNull(0).let { keyword ->
                when(keyword) {
                    null -> true
                    else -> keyword.name.lowercase() != query.lowercase()
                }
            }
        } else {
            false
        }
    }

    LaunchedEffect(search) {
        // hide keyword creation item when changing query
        showKeywordCreationItem = false
    }

    val createKeywordRequestState = rememberTandoorRequestState()
    fun createKeyword(name: String) = coroutineScope.launch {
        createKeywordRequestState.wrapRequest {
            client.keyword.create(name, name).let {
                onCheckedChange(it, it.id, true)
            }
        }
    }

    DockedSearchBar(
        modifier = modifier,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            dividerColor = Color.Transparent
        ),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = {
                    keyboardController?.hide()
                    search = it
                },
                leadingIcon = { Icon(Icons.Rounded.Search, stringResource(R.string.search_tags)) },
                placeholder = { Text(stringResource(R.string.search_tags)) },
                expanded = true,
                onExpandedChange = { }
            )
        },
        expanded = true,
        onExpandedChange = { }
    ) {
        LazyColumn(
            state = searchLazyListState,
        ) {
            if(showKeywordCreationItem && !hideKeywordCreation) {
                item {
                    ListItem(
                        modifier = Modifier.clickable {
                            createKeyword(query)
                        },
                        leadingContent = {
                            IconButton(onClick = {
                                createKeyword(query)
                            }) {
                                Icon(Icons.Rounded.Add, stringResource(id = R.string.action_add))
                            }
                        },
                        headlineContent = {
                            Text(stringResource(R.string.common_create_argument, query))
                        }
                    )
                }
            }

            items(searchResults.size, key = { searchResults[it].id }) {
                val keyword = searchResults[it]

                KeywordCheckedListItem(
                    checked = selectedKeywordIds.contains(keyword.id),
                    keyword = keyword
                ) {
                    keyboardController?.hide()
                    onCheckedChange(keyword, keyword.id, !selectedKeywordIds.contains(keyword.id))
                }
            }
        }
    }

    TandoorRequestErrorHandler(state = createKeywordRequestState)
}