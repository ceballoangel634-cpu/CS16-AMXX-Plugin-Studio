package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AmxxTemplates
import com.example.domain.PluginTemplate
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBgObsidian
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun TemplatesScreen(
    onApplyTemplate: (PluginTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val all = AmxxTemplates.allTemplates
    val categories = remember(all) { listOf("Todas") + all.map { it.category }.distinct() }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(selectedCategory, searchQuery) {
        all.filter { tpl ->
            val matchCat = selectedCategory == "Todas" || tpl.category == selectedCategory
            val matchText = searchQuery.isBlank() ||
                tpl.title.contains(searchQuery, ignoreCase = true) ||
                tpl.description.contains(searchQuery, ignoreCase = true) ||
                tpl.fileName.contains(searchQuery, ignoreCase = true)
            matchCat && matchText
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Creador de Plugins CS 1.6",
                subtitle = "13 plantillas Pawn completas listas para editar y compilar",
                badge = "${filtered.size} plantillas"
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                placeholder = { Text("Buscar plantilla (VIP, tienda, HUD, respawn...)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("templates_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { tpl ->
            val isExpanded = expandedId == tpl.id
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = tpl.fileName,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioCyanAccent
                            )
                        }
                        StatusPill(text = tpl.category, color = StudioBluePrimary)
                    }

                    Text(
                        text = tpl.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tpl.includes.forEach { inc ->
                            StatusPill(text = "<$inc>", color = StudioTextSecondary)
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioBgObsidian,
                            border = BorderStroke(1.dp, StudioBorderSubtle)
                        ) {
                            Text(
                                text = tpl.code,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onApplyTemplate(tpl) },
                            modifier = Modifier.testTag("template_use_${tpl.id}")
                        ) {
                            Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir en Editor SMA")
                        }
                        OutlinedButton(onClick = {
                            expandedId = if (isExpanded) null else tpl.id
                        }) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isExpanded) "Ocultar" else "Ver código")
                        }
                    }
                }
            }
        }
    }
}
