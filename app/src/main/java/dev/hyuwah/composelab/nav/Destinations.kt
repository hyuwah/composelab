package dev.hyuwah.composelab.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Main screen: the list of experiments. */
@Serializable
data object ExperimentList : NavKey

/** One experiment, resolved by [id] against the catalog. */
@Serializable
data class Experiment(val id: String) : NavKey
