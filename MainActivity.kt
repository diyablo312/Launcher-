package com.example.blocklauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A game app this launcher can open. The user must install and own it separately. */
data class GameTarget(
    val label: String,
    val description: String,
    val packageName: String,
    val getUrl: String
)

val gameTargets = listOf(
    GameTarget(
        label = "Minecraft (Bedrock)",
        description = "Official app from Google Play",
        packageName = "com.mojang.minecraftpe",
        getUrl = "https://play.google.com/store/apps/details?id=com.mojang.minecraftpe"
    ),
    GameTarget(
        label = "Minecraft Java (PojavLauncher)",
        description = "Open-source launcher, you need to own Java Edition",
        packageName = "net.kdt.pojavlaunch",
        getUrl = "https://github.com/PojavLauncherTeam/PojavLauncher/releases"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF58B947),
                    background = Color(0xFF1B1B1F),
                    surface = Color(0xFF26262B)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LauncherScreen()
                }
            }
        }
    }
}

private const val PREFS = "launcher_prefs"
private const val KEY_NAME = "player_name"
private const val KEY_TARGET = "target_index"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var playerName by remember { mutableStateOf(prefs.getString(KEY_NAME, "Player") ?: "Player") }
    var selected by remember { mutableStateOf(prefs.getInt(KEY_TARGET, 0).coerceIn(0, gameTargets.lastIndex)) }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Block Launcher", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        Text(
            "Pick your game and press Play. This launcher only opens a game you installed yourself.",
            color = Color(0xFFB0B0B8)
        )

        OutlinedTextField(
            value = playerName,
            onValueChange = {
                playerName = it
                prefs.edit().putString(KEY_NAME, it).apply()
            },
            label = { Text("Nickname") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Game", fontSize = 18.sp)

        gameTargets.forEachIndexed { index, target ->
            val installed = isInstalled(context, target.packageName)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selected = index
                        prefs.edit().putInt(KEY_TARGET, index).apply()
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = if (selected == index) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == index, onClick = {
                        selected = index
                        prefs.edit().putInt(KEY_TARGET, index).apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.label, fontSize = 16.sp)
                        Text(target.description, fontSize = 13.sp, color = Color(0xFFB0B0B8))
                        Text(
                            if (installed) "Installed" else "Not installed",
                            fontSize = 12.sp,
                            color = if (installed) MaterialTheme.colorScheme.primary else Color(0xFFE57373)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val target = gameTargets[selected]
                status = launchOrGet(context, target)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                if (isInstalled(context, gameTargets[selected].packageName)) "PLAY" else "GET GAME",
                fontSize = 18.sp
            )
        }

        if (status.isNotEmpty()) {
            Text(status, color = Color(0xFFB0B0B8))
        }
    }
}

private fun isInstalled(context: Context, packageName: String): Boolean =
    context.packageManager.getLaunchIntentForPackage(packageName) != null

/** Opens the game if installed, otherwise opens its download page. Returns a status message. */
private fun launchOrGet(context: Context, target: GameTarget): String {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(target.packageName)
    return try {
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Launching ${target.label}..."
        } else {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse(target.getUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(web)
            "${target.label} is not installed. Opening the download page."
        }
    } catch (e: Exception) {
        "Could not open: ${e.message}"
    }
}
