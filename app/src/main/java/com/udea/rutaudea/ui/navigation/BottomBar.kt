package com.udea.rutaudea.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

@Composable
fun BottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = BottomBarItem.values()
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Text(text = item.icon, fontSize = 24.sp) },
                label = { Text(
                    text = item.labelRes,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ) },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                alwaysShowLabel = true
            )
        }
    }
}

private enum class BottomBarItem(
    val icon: String,
    val labelRes: String
) {
    SIMULACRO("🧪", "Simulacro"),
    PRACTICA("📝", "Práctica"),
    PROGRESO("📈", "Progreso"),
    INFO("📚", "Info"),
    PERFIL("👤", "Perfil")
}