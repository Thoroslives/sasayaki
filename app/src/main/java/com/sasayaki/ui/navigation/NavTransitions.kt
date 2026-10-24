package com.sasayaki.ui.navigation

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

// NavTransitions fade through between tabs and slide on a shared X axis into and out of
// detail screens. Slides use the non-bouncy effects springs: a whole screen overshooting
// its edge reads as a glitch, not as expressive motion.
@Immutable
class NavTransitions(
    private val fade: FiniteAnimationSpec<Float>,
    private val fastFade: FiniteAnimationSpec<Float>,
    private val slide: FiniteAnimationSpec<IntOffset>
) {
    val enter: Scope.() -> EnterTransition = {
        if (isBetweenTabs()) fadeIn(fade) + scaleIn(fade, initialScale = 0.96f)
        else slideIntoContainer(SlideDirection.Start, slide) + fadeIn(fade)
    }

    val exit: Scope.() -> ExitTransition = {
        if (isBetweenTabs()) fadeOut(fastFade)
        else slideOutOfContainer(SlideDirection.Start, slide) { it / 4 } + fadeOut(fastFade)
    }

    val popEnter: Scope.() -> EnterTransition = {
        if (isBetweenTabs()) fadeIn(fade) + scaleIn(fade, initialScale = 0.96f)
        else slideIntoContainer(SlideDirection.End, slide) { it / 4 } + fadeIn(fade)
    }

    val popExit: Scope.() -> ExitTransition = {
        if (isBetweenTabs()) fadeOut(fastFade)
        else slideOutOfContainer(SlideDirection.End, slide) + fadeOut(fastFade)
    }

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
