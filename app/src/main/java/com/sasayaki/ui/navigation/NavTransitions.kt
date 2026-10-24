package com.sasayaki.ui.navigation

import androidx.activity.BackEventCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

// NavTransitions fade through when switching tabs and slide everywhere else; going back
// always slides, including during the predictive back gesture, whose NavHost default
// would otherwise shrink the screen towards the centre. Slides use the non-bouncy effects springs:
// a whole screen overshooting its edge reads as a glitch, not as expressive motion.
@Immutable
class NavTransitions(
    private val fade: FiniteAnimationSpec<Float>,
    private val fastFade: FiniteAnimationSpec<Float>,
    private val slide: FiniteAnimationSpec<IntOffset>
) {
    val enter: Scope.() -> EnterTransition = {
        if (isBetweenTabs()) fadeIn(fade) + scaleIn(fade, initialScale = 0.96f)
        else slideIntoContainer(SlideDirection.Start, slide)
    }

    val exit: Scope.() -> ExitTransition = {
        if (isBetweenTabs()) fadeOut(fastFade)
        else slideOutOfContainer(SlideDirection.Start, slide) { it / 4 }
    }

    val popEnter: Scope.() -> EnterTransition = {
        slideIntoContainer(SlideDirection.End, slide) { it / 4 }
    }

    val popExit: Scope.() -> ExitTransition = {
        slideOutOfContainer(SlideDirection.End, slide)
    }

    // The back gesture can start from either edge; the screen leaves the way the finger moves.
    val predictivePopEnter: Scope.(Int) -> EnterTransition = { edge ->
        slideIntoContainer(edge.direction(), slide) { it / 4 }
    }

    val predictivePopExit: Scope.(Int) -> ExitTransition = { edge ->
        slideOutOfContainer(edge.direction(), slide)
    }

    private fun Int.direction() =
        if (this == BackEventCompat.EDGE_RIGHT) SlideDirection.Start else SlideDirection.End

    private fun Scope.isBetweenTabs(): Boolean =
        initialState.destination.topLevelDestination() != null &&
            targetState.destination.topLevelDestination() != null
}

@Composable
fun rememberNavTransitions(): NavTransitions {
    val motion = MaterialTheme.motionScheme
    val fade = motion.defaultEffectsSpec<Float>()
    val fastFade = motion.fastEffectsSpec<Float>()
    val slide = motion.defaultEffectsSpec<IntOffset>()
    return remember(fade, fastFade, slide) { NavTransitions(fade, fastFade, slide) }
}
