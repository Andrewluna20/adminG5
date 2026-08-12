package com.theextramile.admin.ui.blog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.BlogPost
import com.theextramile.admin.data.model.MAX_NAV_LABEL
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*

/**
 * Blog — port de admin-html/blog.html + admin-js/blog.js.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun BlogScreen(
    viewModel: BlogViewModel,
    siteBaseUrl: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val counts by viewModel.counts.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val editing by viewModel.editing.collectAsState()

    var pendingDelete by remember { mutableStateOf<BlogPost?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    val pullState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            SectionHeader(
                title = "Blog",
                subtitle = "${posts.size} entrada(s)",
                onBack = onBack
            )

            Column(Modifier.padding(horizontal = 16.dp)) {
                SearchField(query, viewModel::onQueryChange, "Buscar entradas…")
            }
            Spacer(Modifier.height(12.dp))
            FilterChipRow(
                options = listOf(
                    "" to "Todas",
                    "published" to "Publicadas",
                    "draft" to "Borradores"
                ),
                selected = filter,
                onSelect = viewModel::onFilterChange,
                counts = counts
            )
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
                when {
                    uiState.isLoading -> SectionPlaceholder("Cargando…", isLoading = true)

                    posts.isEmpty() -> SectionPlaceholder(
                        message = if (counts[""] == 0) "Aún no hay entradas en el blog"
                        else "Sin resultados con estos filtros",
                        icon = Icons.Default.Article,
                        actionLabel = if (counts[""] == 0) "Crear la primera" else null,
                        onAction = if (counts[""] == 0) ({ viewModel.startNew() }) else null
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(posts, key = { it.id }) { post ->
                            BlogPostCard(
                                post = post,
                                siteBaseUrl = siteBaseUrl,
                                onEdit = { viewModel.startEdit(post) },
                                onTogglePublished = { viewModel.togglePublished(post) },
                                onDelete = { pendingDelete = post }
                            )
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgLight,
                    contentColor = CyanLight
                )

                AddFab(
                    onClick = { viewModel.startNew() },
                    contentDescription = "Nueva entrada",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                )
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }

    if (editing != null) {
        BlogEditorSheet(
            post = editing!!,
            siteBaseUrl = siteBaseUrl,
            isSaving = uiState.isSaving,
            isUploading = uiState.isUploading,
            onChange = viewModel::updateDraft,
            onPickCover = viewModel::uploadCover,
            onSave = viewModel::save,
            onDismiss = viewModel::cancelEdit
        )
    }

    pendingDelete?.let { post ->
        ConfirmDialog(
            title = "Eliminar entrada",
            message = "Se eliminará «${post.title}» del blog y del sitio público. No se puede deshacer.",
            onConfirm = {
                viewModel.delete(post)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun BlogPostCard(
    post: BlogPost,
    siteBaseUrl: String,
    onEdit: () -> Unit,
    onTogglePublished: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(onClick = onEdit, contentPadding = 0.dp) {
        Column {
            if (post.hasCover) {
                AsyncImage(
                    model = absoluteUrl(post.coverImage, siteBaseUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )
            }
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TonePill(
                        post.statusLabel,
                        if (post.published) GreenLight else Yellow
                    )
                    if (post.showInNav) {
                        Spacer(Modifier.width(6.dp))
                        TonePill("Barra", CyanLight)
                    }
                    if (post.showInFooter) {
                        Spacer(Modifier.width(6.dp))
                        TonePill("Pie", PurpleLight)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(post.date, color = TextDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    post.title.ifBlank { "Sin título" },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.excerpt.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        post.excerpt,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("/${post.id}", color = TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onTogglePublished) {
                        Text(
                            if (post.published) "Despublicar" else "Publicar",
                            color = if (post.published) Yellow else GreenLight,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlogEditorSheet(
    post: BlogPost,
    siteBaseUrl: String,
    isSaving: Boolean,
    isUploading: Boolean,
    onChange: ((BlogPost) -> BlogPost) -> Unit,
    onPickCover: (android.net.Uri) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onPickCover(uri) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (post.id.isBlank()) "Nueva entrada" else "Editar entrada",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            AdminField("Título", post.title, { v -> onChange { it.copy(title = v) } })

            AdminField(
                "Resumen", post.excerpt, { v -> onChange { it.copy(excerpt = v) } },
                hint = "Las dos líneas que se ven en la tarjeta del blog",
                singleLine = false, minLines = 2
            )

            AdminField(
                "Contenido", post.content, { v -> onChange { it.copy(content = v) } },
                hint = "Acepta HTML, igual que en el panel web",
                singleLine = false, minLines = 6
            )

            FormSectionTitle("Portada")
            if (post.hasCover) {
                AsyncImage(
                    model = absoluteUrl(post.coverImage, siteBaseUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(8.dp))
            }
            GradientButton(
                text = if (post.hasCover) "Cambiar portada" else "Subir portada",
                onClick = { picker.launch("image/*") },
                isLoading = isUploading,
                icon = Icons.Default.Image,
                height = 46.dp,
                modifier = Modifier.fillMaxWidth()
            )

            FormSectionTitle("Publicación")
            AdminField("Autor", post.author, { v -> onChange { it.copy(author = v) } })
            AdminField(
                "Fecha", post.date, { v -> onChange { it.copy(date = v) } },
                hint = "Formato AAAA-MM-DD. Si la dejas vacía, el servidor pone la de hoy."
            )
            AdminField(
                "Etiquetas", post.tags.joinToString(", "),
                { v -> onChange { it.copy(tags = v.split(",").map(String::trim).filter(String::isNotBlank)) } },
                hint = "Sepáralas con comas"
            )
            AdminSwitch(
                "Publicada", post.published,
                { v -> onChange { it.copy(published = v) } },
                hint = "Si está apagada queda como borrador y no sale en el sitio"
            )

            FormSectionTitle("Destacar en el sitio")
            AdminSwitch(
                "Mostrar en la barra", post.showInNav,
                { v -> onChange { it.copy(showInNav = v) } }
            )
            AdminSwitch(
                "Mostrar en el pie", post.showInFooter,
                { v -> onChange { it.copy(showInFooter = v) } }
            )
            AdminField(
                "Etiqueta corta", post.navLabel,
                { v -> onChange { it.copy(navLabel = v.take(MAX_NAV_LABEL)) } },
                hint = "Máximo $MAX_NAV_LABEL caracteres. Si la dejas vacía se usa el título."
            )

            FormSectionTitle("SEO de la entrada")
            AdminField("Meta título", post.metaTitle, { v -> onChange { it.copy(metaTitle = v) } })
            AdminField(
                "Meta descripción", post.metaDescription,
                { v -> onChange { it.copy(metaDescription = v) } },
                singleLine = false, minLines = 2
            )

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar entrada",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Las URLs de las imágenes vienen relativas ("uploads/blog/x.webp"), así que
 * hay que pegarles la raíz del sitio para que Coil las pueda cargar.
 */
fun absoluteUrl(path: String, siteBaseUrl: String): String = when {
    path.isBlank() -> ""
    path.startsWith("http://") || path.startsWith("https://") -> path
    else -> siteBaseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
