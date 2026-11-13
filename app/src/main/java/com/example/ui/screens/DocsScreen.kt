package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AmxxDocumentation
import com.example.domain.DocCategory
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
fun DocsScreen(
    onInsertSnippetToSma: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<DocCategory?>(null) }
    var search by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val filtered = remember(selectedCategory, search) {
        AmxxDocumentation.entries.filter { entry ->
            val catOk = selectedCategory == null || entry.category == selectedCategory
            val qOk = search.isBlank() ||
                entry.title.contains(search, true) ||
                entry.summary.contains(search, true) ||
                entry.includeFile.contains(search, true)
            catOk && qOk
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Documentación Pawn & AMXX",
                subtitle = "Natives, forwards, includes, ejemplos y tutoriales",
                badge = "${filtered.size} entradas"
            )
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar documentación") },
                placeholder = { Text("Buscar native, forward o include...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("docs_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todo") }
                    )
                }
                items(DocCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { doc ->
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
                                text = doc.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = doc.includeFile,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioCyanAccent
                            )
                        }
                        StatusPill(text = doc.category.label, color = StudioBluePrimary)
                    }

                    Text(
                        text = doc.signature,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioBluePrimary
                    )

                    Text(
                        text = doc.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioBgObsidian,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        Text(
                            text = doc.exampleSnippet,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onInsertSnippetToSma(doc.exampleSnippet) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Insertar en SMA")
                        }
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(doc.exampleSnippet)) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copiar")
                        }
                    }
                }
            }
        }
    }
}
