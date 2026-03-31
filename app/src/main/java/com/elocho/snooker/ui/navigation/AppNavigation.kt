package com.elocho.snooker.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.elocho.snooker.ElOchoApp
import com.elocho.snooker.ui.auth.AuthViewModel
import com.elocho.snooker.ui.auth.LoginScreen
import com.elocho.snooker.ui.auth.SplashScreen
import com.elocho.snooker.ui.match.*
import com.elocho.snooker.ui.menu.MainMenuScreen
import com.elocho.snooker.ui.menu.SyncViewModel
import com.elocho.snooker.ui.players.PlayersScreen
import com.elocho.snooker.ui.players.PlayersViewModel
import com.elocho.snooker.ui.settings.SettingsScreen
import com.elocho.snooker.ui.settings.SettingsViewModel
import com.elocho.snooker.ui.statistics.StatisticsScreen
import com.elocho.snooker.ui.statistics.StatisticsViewModel
import com.elocho.snooker.ui.tournament.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object MainMenu : Screen("main_menu")
    object Players : Screen("players")
    object SelectPlayers : Screen("select_players/{mode}") {
        fun createRoute(mode: String) = "select_players/$mode"
    }
    object Scoreboard : Screen("scoreboard")
    object Result : Screen("result")
    object Tournaments : Screen("tournaments")
    object CreateTournament : Screen("create_tournament")
    object TournamentBracket : Screen("bracket/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "bracket/$tournamentId"
    }
    object TournamentScoreboard : Screen("tournament_scoreboard/{matchId}") {
        fun createRoute(matchId: Long) = "tournament_scoreboard/$matchId"
    }
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: ElOchoApp
) {
    // Shared view models
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(app.settingsRepository))
    val authState by authViewModel.uiState.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            app.settingsRepository,
            app.dataBackupRepository,
            app.applicationContext
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsState()

    val playerViewModel: PlayersViewModel = viewModel(
        factory = PlayersViewModel.Factory(app.playerRepository, app.matchRepository)
    )
    val quickMatchViewModel: QuickMatchViewModel = viewModel(
        factory = QuickMatchViewModel.Factory(
            app.matchRepository,
            app.playerRepository,
            app.liveMatchSnapshotRepository,
            app.supabaseSyncRepository
        )
    )
    val tournamentViewModel: TournamentViewModel = viewModel(factory = TournamentViewModel.Factory(app.tournamentRepository, app.playerRepository, app.matchRepository))
    val syncViewModel: SyncViewModel = viewModel(
        factory = SyncViewModel.Factory(
            syncRepository = app.supabaseSyncRepository,
            settingsRepository = app.settingsRepository
        )
    )
    val syncState by syncViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen {
                val dest = if (authState.isLoggedIn) Screen.MainMenu.route else Screen.Login.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Login.route) {
            LaunchedEffect(authState.isLoggedIn) {
                if (authState.isLoggedIn) {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            LoginScreen(
                uiState = authState,
                onUsernameChange = { authViewModel.updateUsername(it) },
                onPasswordChange = { authViewModel.updatePassword(it) },
                onLoginClick = { authViewModel.login() }
            )
        }

        composable(Screen.MainMenu.route) {
            LaunchedEffect(Unit) {
                syncViewModel.autoRefreshIfNeeded()
            }
             MainMenuScreen(
                onQuickMatchClick = { navController.navigate(Screen.SelectPlayers.createRoute("quick")) },
                onTournamentsClick = { navController.navigate(Screen.Tournaments.route) },
                onPlayersClick = { navController.navigate(Screen.Players.route) },
                onStatisticsClick = { navController.navigate(Screen.Statistics.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onSyncClick = { syncViewModel.syncNow() },
                isSyncing = syncState.isSyncing,
                syncMessage = syncState.transientMessage,
                lastSyncedAt = syncState.lastSyncAt,
                lastSyncStatus = syncState.lastSyncStatus,
                lastSyncError = syncState.lastSyncError,
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Players.route) {
            val state by playerViewModel.uiState.collectAsState()
            PlayersScreen(
                uiState = state,
                onSearchChange = { playerViewModel.updateSearchQuery(it) },
                onAddPlayer = { playerViewModel.showAddForm() },
                onEditPlayer = { playerViewModel.showEditForm(it) },
                onDeletePlayer = { playerViewModel.confirmDelete(it) },
                onConfirmDelete = { playerViewModel.deletePlayer() },
                onCancelDelete = { playerViewModel.cancelDelete() },
                onNavigateBack = { navController.navigateUp() },
                onFormNameChange = { playerViewModel.updateFormName(it) },
                onFormImageChange = { playerViewModel.updateFormImageUri(it) },
                onSavePlayer = { playerViewModel.savePlayer() },
                onHideForm = { playerViewModel.hideForm() }
            )
        }

        composable(
            route = Screen.SelectPlayers.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "quick"
            val state by playerViewModel.uiState.collectAsState()
            
            PlayerSelectionScreen(
                players = state.players,
                onPlayersSelected = { ids ->
                    if (ids.size == 2) {
                        quickMatchViewModel.setupMatch(
                            player1Id = ids[0],
                            player2Id = ids[1]
                        )
                        navController.navigate(Screen.Scoreboard.route) {
                            popUpTo(Screen.SelectPlayers.route) { inclusive = true }
                        }
                    }
                },
                onNavigateBack = { navController.navigateUp() },
                onAddPlayer = {
                    navController.navigate(Screen.Players.route)
                }
            )
        }

        composable(Screen.Scoreboard.route) {
            val state by quickMatchViewModel.uiState.collectAsState()

            LaunchedEffect(state.isFinished) {
                if (state.isFinished) {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(Screen.Scoreboard.route) { inclusive = true }
                    }
                }
            }

            ScoreboardScreen(
                uiState = state,
                onScoreAction = { playerNumber, action -> quickMatchViewModel.addScore(playerNumber, action) },
                onCorrection = { quickMatchViewModel.applyScoreCorrection(it) },
                onUndo = { quickMatchViewModel.undoLastScoreAction() },
                remoteRedKeyCode = settingsState.remoteRedKeyCode,
                remoteUndoKeyCode = settingsState.remoteUndoKeyCode,
                remoteYellowKeyCode = settingsState.remoteYellowKeyCode,
                remoteGreenKeyCode = settingsState.remoteGreenKeyCode,
                remoteBrownKeyCode = settingsState.remoteBrownKeyCode,
                remoteBlueKeyCode = settingsState.remoteBlueKeyCode,
                remotePinkKeyCode = settingsState.remotePinkKeyCode,
                remoteBlackKeyCode = settingsState.remoteBlackKeyCode,
                remoteErrorKeyCode = settingsState.remoteErrorKeyCode,
                onRemoteScoreAction = { action -> quickMatchViewModel.addScoreForActivePlayer(action) },
                onEndGame = { quickMatchViewModel.endGame() },
                onTvPlayerSelect = { quickMatchViewModel.selectTvPlayer(it) },
                onDismissEndMatchConfirmation = { quickMatchViewModel.dismissEndMatchConfirmation() }
            )
        }

        composable(Screen.Result.route) {
            val state by quickMatchViewModel.uiState.collectAsState()
            ResultScreen(
                uiState = state,
                onDone = {
                    quickMatchViewModel.reset()
                    navController.navigateUp()
                }
            )
        }

        // Tournaments Nav
        composable(Screen.Tournaments.route) {
            val state by tournamentViewModel.listState.collectAsState()
            TournamentsScreen(
                tournaments = state.tournaments,
                onCreateClick = {
                    tournamentViewModel.loadAvailablePlayers()
                    navController.navigate(Screen.CreateTournament.route)
                },
                onTournamentClick = { id -> 
                    tournamentViewModel.loadBracket(id)
                    navController.navigate(Screen.TournamentBracket.createRoute(id)) 
                },
                onDeleteClick = { tournamentViewModel.deleteTournament(it) },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.CreateTournament.route) {
            val state by tournamentViewModel.createState.collectAsState()
            CreateTournamentScreen(
                uiState = state,
                onNameChange = { tournamentViewModel.updateTournamentName(it) },
                onPlayerCountChange = { tournamentViewModel.setSelectedPlayerCount(it) },
                onAssignPlayerToSlot = { slotIndex, playerId -> tournamentViewModel.assignPlayerToSlot(slotIndex, playerId) },
                onCreate = {
                    tournamentViewModel.createTournament { newId ->
                        navController.navigateUp()
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.TournamentBracket.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tId = backStackEntry.arguments?.getLong("tournamentId") ?: return@composable
            val state by tournamentViewModel.bracketState.collectAsState()

            TournamentBracketScreen(
                uiState = state,
                onMatchClick = { match ->
                    quickMatchViewModel.setupMatch(
                        player1Id = match.player1Id!!,
                        player2Id = match.player2Id!!,
                        tournamentId = match.tournamentId,
                        tournamentRound = match.roundNumber,
                        tournamentMatchId = match.id
                    )
                    navController.navigate(Screen.TournamentScoreboard.createRoute(match.id))
                },
                onNavigateBack = { navController.navigateUp() },
                onResolveDrawWinner = { tournamentViewModel.resolveDrawWinner(it) },
                onDismissDrawDialog = { tournamentViewModel.dismissDrawDialog() }
            )
        }

        composable(
            route = Screen.TournamentScoreboard.route,
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: return@composable
            val state by quickMatchViewModel.uiState.collectAsState()

            ScoreboardScreen(
                uiState = state,
                onScoreAction = { playerNumber, action -> quickMatchViewModel.addScore(playerNumber, action) },
                onCorrection = { quickMatchViewModel.applyScoreCorrection(it) },
                onUndo = { quickMatchViewModel.undoLastScoreAction() },
                remoteRedKeyCode = settingsState.remoteRedKeyCode,
                remoteUndoKeyCode = settingsState.remoteUndoKeyCode,
                remoteYellowKeyCode = settingsState.remoteYellowKeyCode,
                remoteGreenKeyCode = settingsState.remoteGreenKeyCode,
                remoteBrownKeyCode = settingsState.remoteBrownKeyCode,
                remoteBlueKeyCode = settingsState.remoteBlueKeyCode,
                remotePinkKeyCode = settingsState.remotePinkKeyCode,
                remoteBlackKeyCode = settingsState.remoteBlackKeyCode,
                remoteErrorKeyCode = settingsState.remoteErrorKeyCode,
                onRemoteScoreAction = { action -> quickMatchViewModel.addScoreForActivePlayer(action) },
                onEndGame = { },
                isTournamentMatch = true,
                onTvPlayerSelect = { quickMatchViewModel.selectTvPlayer(it) },
                onDismissEndMatchConfirmation = { quickMatchViewModel.dismissEndMatchConfirmation() },
                onEndTournamentGame = {
                    val matchResult = quickMatchViewModel.endTournamentGame()
                    if (matchResult != null) {
                        tournamentViewModel.completeMatch(matchId, matchResult) { drawMatchId ->
                           // Need Draw Resolution
                        }
                    }
                    quickMatchViewModel.reset()
                    navController.navigateUp()
                }
            )
        }

        // Stats & Settings
        composable(Screen.Statistics.route) {
            val statsViewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory(app.playerRepository, app.matchRepository, app.tournamentRepository))
            val state by statsViewModel.uiState.collectAsState()
            StatisticsScreen(uiState = state, onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                uiState = settingsState,
                onNavigateBack = { navController.navigateUp() },
                onBeginAssignment = { settingsViewModel.beginAssignment(it) },
                onClearAssignment = { settingsViewModel.clearAssignment(it) },
                onResetRemoteMappings = { settingsViewModel.resetRemoteMappings() },
                onRemoteKeyEvent = { keyCode, isKeyUp -> settingsViewModel.handleRemoteKeyEvent(keyCode, isKeyUp) },
                onExportPlayers = { settingsViewModel.exportPlayers(it) },
                onExportTournaments = { settingsViewModel.exportTournaments(it) },
                onExportAll = { settingsViewModel.exportAll(it) },
                onImportPlayers = { settingsViewModel.importPlayers(it) },
                onImportTournaments = { settingsViewModel.importTournaments(it) },
                onImportAll = { settingsViewModel.importAll(it) }
            )
        }
    }
}
