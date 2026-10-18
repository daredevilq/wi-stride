package io.github.daredevilq.wistride.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme

/** Section title exposed to accessibility services as a heading. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .padding(vertical = WiStrideTheme.spacing.s)
                .semantics { heading() },
    )
}

@PreviewLightDark
@Composable
private fun SectionHeaderPreview() {
    WiStrideTheme(dynamicColor = false) {
        SectionHeader(text = "Connection")
    }
}
