package dev.hyuwah.composelab.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.hyuwah.composelab.ui.experiments.ExperimentListScreen
import dev.hyuwah.composelab.ui.experiments.experiments

@Composable
fun ComposeLabNavHost(modifier: Modifier = Modifier) {
    // Back stack survives process death (keys are @Serializable).
    val backStack = rememberNavBackStack(ExperimentList)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Keeps rememberSaveable/scroll state per entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Gives every entry its own ViewModelStore.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ExperimentList> {
                ExperimentListScreen(
                    experiments = experiments,
                    onOpenExperiment = { experiment -> backStack.add(Experiment(experiment.id)) },
                )
            }
            entry<Experiment> { key ->
                // One branch for every experiment; the registry decides what to render.
                val experiment = experiments.firstOrNull { it.id == key.id }
                if (experiment == null) {
                    UnknownExperimentScreen(id = key.id)
                } else {
                    experiment.Content(onBack = { backStack.removeLastOrNull() })
                }
            }
        },
    )
}

@Composable
private fun UnknownExperimentScreen(id: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No experiment registered for \"$id\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
