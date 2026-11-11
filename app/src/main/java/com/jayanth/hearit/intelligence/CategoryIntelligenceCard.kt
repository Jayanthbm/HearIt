package com.jayanth.hearit.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.settings.GeneralSettingsUiState
import com.jayanth.hearit.settings.SettingsCard

@Composable
internal fun CategoryIntelligenceCard(
    state: GeneralSettingsUiState,
    actions: IntelligenceActions,
    modifier: Modifier = Modifier,
) {
    var showCarrierPhrasesDialog by remember { mutableStateOf(false) }
    var showAppCategoriesDialog by remember { mutableStateOf(false) }

    val emailCount = state.appCategories.count { it.value == AppCategory.EMAIL }
    val msgCount = state.appCategories.count { it.value == AppCategory.MESSAGING }
    val smsCount = state.appCategories.count { it.value == AppCategory.SMS }

    SettingsCard(
        title = "Category Intelligence",
        subtitle = "Tailored rules for Messaging, SMS/Bank Alerts, and Email apps",
        icon = Icons.Rounded.Category,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NlpToggleRow(
                title = "Messaging & Media Polish",
                subtitle = "Converts videos, PDFs, contacts, group prefixes & silences sync noise",
                checked = state.isMessagingIntelligenceEnabled,
                onCheckedChange = actions.onToggleMessagingIntelligence,
            )
            DividerLine()

            NlpToggleRow(
                title = "Bank & Transactional SMS Cleaner",
                subtitle = "Condenses INR/card debit alerts to crisp spoken summaries",
                checked = state.isSmsBankCleanerEnabled,
                onCheckedChange = actions.onToggleSmsBankCleaner,
            )
            DividerLine()

            NlpToggleRow(
                title = "Carrier & Promo SMS Filter",
                subtitle = "Skips recharge promos, dial ads, and configurable spam phrases",
                checked = state.isCarrierSpamFilterEnabled,
                onCheckedChange = actions.onToggleCarrierSpamFilter,
            )
            if (state.isCarrierSpamFilterEnabled) {
                OutlinedButton(
                    onClick = { showCarrierPhrasesDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configure Promo Phrases (${state.carrierSpamPhrases.size})")
                }
            }
            DividerLine()

            NlpToggleRow(
                title = "Email Subject & Disclaimer Cleanup",
                subtitle = "Speaks 'Reply:'/'Forward:' and strips sign-offs & legal disclaimers",
                checked = state.isEmailCleanupEnabled,
                onCheckedChange = actions.onToggleEmailCleanup,
            )
            DividerLine()

            OutlinedButton(
                onClick = { showAppCategoriesDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("App Categories (Email $emailCount · Msg $msgCount · SMS $smsCount)")
            }
        }
    }

    if (showCarrierPhrasesDialog) {
        CarrierSpamPhrasesDialog(
            phrases = state.carrierSpamPhrases,
            onAddPhrase = actions.onAddCarrierSpamPhrase,
            onRemovePhrase = actions.onRemoveCarrierSpamPhrase,
            onRestoreDefaults = actions.onLoadDefaultCarrierSpamPhrases,
            onDismiss = { showCarrierPhrasesDialog = false },
        )
    }

    if (showAppCategoriesDialog) {
        AppCategoriesDialog(
            apps = state.installedApps,
            appCategories = state.appCategories,
            onSetAppCategory = actions.onSetAppCategory,
            onResetAppCategory = actions.onResetAppCategory,
            onDismiss = { showAppCategoriesDialog = false },
        )
    }
}
