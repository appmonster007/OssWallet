package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.discovery.RecurringSuggestions
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecurringNudgeViewModel @Inject constructor(
    private val suggestions: RecurringSuggestions
) : ViewModel() {
    /** Repeated payments the user hasn't looked at yet. */
    val unseen: StateFlow<List<DiscoveredPattern>> = suggestions.observeUnseen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** "Not now": counts as seen, so the banner stays away until something new turns up. */
    fun hide() {
        viewModelScope.launch { suggestions.markSeen(unseen.value) }
    }
}

/** A slim prompt at the top of the dashboard when new repeated payments were found. */
@Composable
fun RecurringNudgeBanner(onOpen: () -> Unit, viewModel: RecurringNudgeViewModel = hiltViewModel()) {
    val unseen by viewModel.unseen.collectAsStateWithLifecycle()
    if (unseen.isEmpty()) return
    val names = unseen.map { it.payee.ifBlank { it.name } }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.accentContainer
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AppColors.onAccentContainer, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (unseen.size == 1) "1 repeated payment found" else "${unseen.size} repeated payments found",
                    style = AppTextStyles.transactionTitle, color = AppColors.onAccentContainer
                )
                Text(
                    names.joinToString(", "),
                    style = AppTextStyles.caption, color = AppColors.onAccentContainer,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onOpen) { Text("Review") }
            IconButton(onClick = viewModel::hide, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Not now", tint = AppColors.onAccentContainer, modifier = Modifier.size(18.dp))
            }
        }
    }
}
