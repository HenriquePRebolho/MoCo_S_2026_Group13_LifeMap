package com.example.livemap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.livemap.ui.theme.LiveMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveMapTheme() {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

private enum class TopDest(val route: String, val label: String, val icon: Int) {
    Events("events", "Events", R.drawable.event),
    Map("map", "Map", R.drawable.map),
    New("new", "New", R.drawable.add_circle),
    Friends("friends", "Friends", R.drawable.person_add),
    Profile("profile", "Profile", R.drawable.account_circle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val vm: CounterViewModel = viewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TopDest.Events.route
    val tabs = listOf(TopDest.Events, TopDest.Map, TopDest.New, TopDest.Friends, TopDest.Profile)
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            Column {
                TabRow(selectedTabIndex = selectedIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            text = { Text(tab.label) },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.icon),
                                    tint = Color.LightGray,
                                    contentDescription = null,
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Events.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDest.Events.route) { EventsScreen(vm) }
            composable(TopDest.Map.route) { MapScreen(vm) }
            composable(TopDest.New.route) { NewScreen(vm) }
            composable(TopDest.Friends.route) { FriendsScreen(vm) }
            composable(TopDest.Profile.route) { ProfileScreen(vm) }
        }
    }
}