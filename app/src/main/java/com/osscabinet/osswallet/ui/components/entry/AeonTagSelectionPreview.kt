package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun AeonTagSelectionPreview() {
    val mockTags = listOf(
        Tag(1, "Personal", 0xFFEF5350.toInt()),
        Tag(2, "Work", 0xFF42A5F5.toInt()),
        Tag(3, "Shopping", 0xFFFFCA28.toInt()),
        Tag(4, "Entertainment", 0xFFAB47BC.toInt()),
        Tag(5, "Health", 0xFF66BB6A.toInt()),
        Tag(6, "Travel", 0xFF26C6DA.toInt()),
        Tag(7, "Food", 0xFFFF7043.toInt()),
        Tag(8, "Bills", 0xFF5C6BC0.toInt())
    )
    
    OssWalletTheme {
        AeonTagSelectionContent(
            tags = mockTags,
            selectedTags = setOf("Personal", "Shopping"),
            onTagsSelected = {},
            onAddTag = { _, _ -> },
            onEditTag = {},
            onDeleteTag = {},
            onDismiss = {}
        )
    }
}
