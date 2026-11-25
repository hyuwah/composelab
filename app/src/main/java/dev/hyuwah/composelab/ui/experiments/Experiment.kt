package dev.hyuwah.composelab.ui.experiments

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * One experiment = one object implementing this, living in its own file
 * under `ui/playground/<name>/`. Registering it is a single line in [experiments].
 */
interface Experiment {
    val id: String
    val title: String
    val description: String

    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier = Modifier)
}
