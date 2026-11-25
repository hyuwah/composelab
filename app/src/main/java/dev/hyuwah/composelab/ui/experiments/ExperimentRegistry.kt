package dev.hyuwah.composelab.ui.experiments

import dev.hyuwah.composelab.ui.playground.button.ButtonExperiment
import dev.hyuwah.composelab.ui.playground.toggle.ToggleExperiment

/**
 * The only central file. Adding an experiment: create its file, add one line here.
 * Nothing else in the app needs to change.
 */
val experiments: List<Experiment> = listOf(
    ButtonExperiment,
    ToggleExperiment,
)
