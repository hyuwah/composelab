package dev.hyuwah.composelab.ui.playground.toggle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.experiments.Experiment
import dev.hyuwah.composelab.ui.experiments.ExperimentScaffold
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme

/**
 * Experiment: a single switch. Everything for this playground lives in this file.
 * Start here: animated thumb/color, custom thumb content, Switch + state hoisting.
 */
object ToggleExperiment : Experiment {
    override val id = "toggle"
    override val title = "Toggle playground"
    override val description = "A single switch. Toggle motion, colors and state experiments go here."

    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        ExperimentScaffold(
            title = title,
            description = description,
            onBack = onBack,
            modifier = modifier,
        ) {
            var checked by rememberSaveable { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(checked = checked, onCheckedChange = { checked = it })
                    Text(text = "Switch", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = if (checked) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToggleExperimentPreview() {
    ComposeLabTheme {
        ToggleExperiment.Content(onBack = {})
    }
}
