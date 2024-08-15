package de.kitshn.android.ui.route.main.subroute.mealplan

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.kitshn.android.api.tandoor.model.TandoorMealPlan
import de.kitshn.android.api.tandoor.rememberTandoorRequestState
import de.kitshn.android.ui.TandoorRequestErrorHandler
import de.kitshn.android.ui.component.alert.LoadingErrorAlertPaneWrapper
import de.kitshn.android.ui.dialog.mealplan.MealPlanCreationAndEditDialog
import de.kitshn.android.ui.dialog.mealplan.MealPlanDetailsBottomSheet
import de.kitshn.android.ui.dialog.mealplan.rememberMealPlanCreationDialogState
import de.kitshn.android.ui.dialog.mealplan.rememberMealPlanDetailsBottomSheetState
import de.kitshn.android.ui.dialog.mealplan.rememberMealPlanEditDialogState
import de.kitshn.android.ui.route.RouteParameters
import de.kitshn.android.ui.selectionMode.rememberSelectionModeState
import de.kitshn.android.ui.state.ErrorLoadingSuccessState
import de.kitshn.android.ui.state.rememberErrorLoadingSuccessState
import de.kitshn.android.ui.view.ViewParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMainSubrouteMealplan(
    p: RouteParameters
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var pageLoadingState by rememberErrorLoadingSuccessState()
    val selectionModeState = rememberSelectionModeState<Int>()

    val detailsBottomSheetState = rememberMealPlanDetailsBottomSheetState()
    val creationDialogState =
        rememberMealPlanCreationDialogState(key = "RouteMainSubrouteMealplan/mealPlanCreationDialogState")
    val editDialogState =
        rememberMealPlanEditDialogState(key = "RouteMainSubrouteMealplan/mealPlanEditDialogState")

    val mainFetchRequestState = rememberTandoorRequestState()
    mainFetchRequestState.LoadingStateAdapter { pageLoadingState = it }

    val moveRequestState = rememberTandoorRequestState()
    val deleteRequestState = rememberTandoorRequestState()

    var startDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val endDate = startDate.plusDays(6)

    val mealPlanList = remember { mutableStateListOf<TandoorMealPlan>() }
    var lastMealPlanUpdate by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lastMealPlanUpdate, startDate) {
        pageLoadingState = ErrorLoadingSuccessState.LOADING

        mainFetchRequestState.wrapRequest {
            p.vm.tandoorClient?.mealPlan?.fetch(
                startDate,
                startDate.plusDays(7)
            )?.let {
                pageLoadingState = ErrorLoadingSuccessState.SUCCESS

                mealPlanList.clear()
                mealPlanList.addAll(
                    it
                )
            }
        }
    }

    Scaffold(
        topBar = {
            if(p.vm.tandoorClient == null) return@Scaffold
            RouteMainSubrouteMealplanTopAppBar(
                client = p.vm.tandoorClient!!,
                selectionModeState = selectionModeState,
                scrollBehavior = scrollBehavior,
                mealPlanEditDialogState = editDialogState,
                mealPlanMoveRequestState = moveRequestState,
                mealPlanDeleteRequestState = deleteRequestState
            ) { lastMealPlanUpdate = System.currentTimeMillis() }
        }
    ) {
        LoadingErrorAlertPaneWrapper(
            loadingState = pageLoadingState
        ) {
            RouteMainSubrouteMealplanScaffoldContent(
                pv = it,
                scrollBehavior = scrollBehavior,
                startDate = startDate,
                endDate = endDate,
                list = mealPlanList,
                pageLoadingState = pageLoadingState,
                selectionModeState = selectionModeState,
                detailsBottomSheetState = detailsBottomSheetState,
                creationDialogState = creationDialogState
            ) { date ->
                coroutineScope.launch {
                    pageLoadingState = ErrorLoadingSuccessState.LOADING
                    delay(300)
                    startDate = date
                }
            }
        }
    }

    if(p.vm.tandoorClient != null) {
        MealPlanCreationAndEditDialog(
            client = p.vm.tandoorClient!!,
            creationState = creationDialogState,
            editState = editDialogState
        ) { lastMealPlanUpdate = System.currentTimeMillis() }
    }

    MealPlanDetailsBottomSheet(
        p = ViewParameters(
            vm = p.vm,
            back = p.onBack
        ),
        state = detailsBottomSheetState,
        reopenOnLaunchKey = "RouteMainSubrouteMealplan/mealPlanDetailsBottomSheet",
        onUpdateList = { lastMealPlanUpdate = System.currentTimeMillis() }
    ) {
        editDialogState.open(it)
    }

    TandoorRequestErrorHandler(state = moveRequestState)
    TandoorRequestErrorHandler(state = deleteRequestState)
}