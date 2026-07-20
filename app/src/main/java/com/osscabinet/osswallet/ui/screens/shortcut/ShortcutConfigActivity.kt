package com.osscabinet.ossWallet.ui.screens.shortcut

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.UiDensity
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class ShortcutConfigActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val theme by settingsRepository.theme
                .collectAsStateWithLifecycle(initialValue = "System")
            val selectedFont by settingsRepository.selectedFont
                .collectAsStateWithLifecycle(initialValue = "MANROPE")
            val glassSettings by settingsRepository.glassSettings
                .collectAsStateWithLifecycle(initialValue = GlassSettings())
            val uiDensityName by settingsRepository.uiDensity
                .collectAsStateWithLifecycle(initialValue = UiDensity.DEFAULT.name)
            val uiDensity = UiDensity.fromName(uiDensityName)
            val glassmorphismEnabled by settingsRepository.glassmorphismEnabled
                .collectAsStateWithLifecycle(initialValue = true)

            var templates by remember { mutableStateOf<List<Template>>(emptyList()) }
            var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

            LaunchedEffect(Unit) {
                val profileId = settingsRepository.currentProfileId.first()
                templates = templateRepository.getAllTemplatesSync(profileId)
                categories = categoryRepository.getAllCategoriesSync()
            }

            OssWalletTheme(
                theme = theme,
                fontName = selectedFont,
                glassSettings = glassSettings,
                uiDensity = uiDensity,
                setWindowBackground = true
            ) {
                CompositionLocalProvider(LocalGlassmorphismEnabled provides glassmorphismEnabled) {
                    ShortcutConfigScreen(
                        templates = templates,
                        categories = categories,
                        onTemplateSelected = { template, iconBitmap ->
                            createPinnedShortcut(template, iconBitmap)
                            finish()
                        },
                        onDismiss = ::finish
                    )
                }
            }
        }
    }

    private fun createPinnedShortcut(template: Template, iconBitmap: Bitmap) {
        createTemplateHomeScreenShortcut(this, template, iconBitmap)
    }
}

@Composable
private fun ShortcutConfigScreen(
    templates: List<Template>,
    categories: List<Category>,
    onTemplateSelected: (Template, Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(templates, query) {
        if (query.isBlank()) templates
        else templates.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.shortcut_template_config_title),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.shortcut_template_config_search),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.onBackground),
                cursorBrush = SolidColor(AppColors.primary),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.shortcut_template_config_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { template ->
                    val category = categories.find { it.id == template.categoryId }
                    TemplateRow(
                        template = template,
                        category = category,
                        onClick = { bitmap -> onTemplateSelected(template, bitmap) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: Template,
    category: Category?,
    onClick: (Bitmap) -> Unit
) {
    val iconName = template.icon ?: category?.iconName
    val iconVector = getIconByName(iconName)
    val painter = rememberVectorPainter(iconVector)
    val density = LocalDensity.current

    val bgColor = template.colorHex?.let {
        runCatching { android.graphics.Color.parseColor(it) }.getOrNull()
    }?.let { Color(it) }
        ?: category?.let { Color(it.colorHex) }
        ?: AppColors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val bitmap = buildShortcutIconBitmap(
                    painter = painter,
                    density = density,
                    bgColor = bgColor
                )
                onClick(bitmap)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (category != null) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

