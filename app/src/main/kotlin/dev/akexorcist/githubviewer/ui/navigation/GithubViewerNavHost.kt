package dev.akexorcist.githubviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.akexorcist.githubviewer.presentation.navigation.RepositoryDetailRoute
import dev.akexorcist.githubviewer.presentation.navigation.SearchRoute
import dev.akexorcist.githubviewer.presentation.navigation.UserProfileRoute
import dev.akexorcist.githubviewer.presentation.profile.UserProfileViewModel
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailViewModel
import dev.akexorcist.githubviewer.presentation.search.SearchViewModel
import dev.akexorcist.githubviewer.ui.screen.profile.UserProfileScreen
import dev.akexorcist.githubviewer.ui.screen.repository.RepositoryDetailScreen
import dev.akexorcist.githubviewer.ui.screen.search.SearchScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GithubViewerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = SearchRoute,
        modifier = modifier,
    ) {
        composable<SearchRoute> {
            val viewModel: SearchViewModel = koinViewModel()
            SearchScreen(
                viewModel = viewModel,
                onUserClick = { login -> navController.navigate(UserProfileRoute(login)) },
                onRepoClick = { owner, repo -> navController.navigate(RepositoryDetailRoute(owner, repo)) },
            )
        }

        composable<UserProfileRoute> { backStackEntry ->
            val route: UserProfileRoute = backStackEntry.toRoute()
            val viewModel: UserProfileViewModel = koinViewModel(
                parameters = { parametersOf(route.login) },
            )
            UserProfileScreen(
                viewModel = viewModel,
                onRepoClick = { owner, repo -> navController.navigate(RepositoryDetailRoute(owner, repo)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<RepositoryDetailRoute> { backStackEntry ->
            val route: RepositoryDetailRoute = backStackEntry.toRoute()
            val viewModel: RepositoryDetailViewModel = koinViewModel(
                parameters = { parametersOf(route.owner, route.repo) },
            )
            RepositoryDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
