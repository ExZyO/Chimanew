// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.debug.ChimanewDiagnosticLogViewerScreen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.core.common.i18n.stringResource as contextStringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioDiagnosticsScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioDiagnosticsScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_chimanew_diagnostics_group

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val preferences = remember { Injekt.get<ChimanewPreferences>() }
        val enableDiagnostics by preferences.enableDiagnostics().collectPrefAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_chimanew_diagnostics_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.enableDiagnostics(),
                        title = stringResource(MR.strings.pref_chimanew_enable_diagnostics),
                        subtitle = stringResource(MR.strings.pref_chimanew_enable_diagnostics_summary),
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.diagnosticCategories(),
                        entries = persistentMapOf(
                            "network" to stringResource(MR.strings.pref_chimanew_diag_cat_network),
                            "artwork" to stringResource(MR.strings.pref_chimanew_diag_cat_artwork),
                            "hero" to stringResource(MR.strings.pref_chimanew_diag_cat_hero),
                            "details" to stringResource(MR.strings.pref_chimanew_diag_cat_details),
                            "shelves" to stringResource(MR.strings.pref_chimanew_diag_cat_shelves),
                            "updates" to stringResource(MR.strings.pref_chimanew_diag_cat_updates),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_diagnostic_categories),
                        subtitle = stringResource(MR.strings.pref_chimanew_diagnostic_categories_summary),
                        enabled = enableDiagnostics,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Log Inspection & Tools",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_chimanew_view_diagnostics),
                        subtitle = stringResource(MR.strings.pref_chimanew_view_diagnostics_summary),
                        enabled = enableDiagnostics,
                        onClick = {
                            navigator.push(ChimanewDiagnosticLogViewerScreen())
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_chimanew_copy_diagnostics),
                        subtitle = stringResource(MR.strings.pref_chimanew_copy_diagnostics_summary),
                        enabled = enableDiagnostics,
                        onClick = {
                            val text = ChimanewDiagnostics.exportText()
                            if (text.isBlank()) {
                                context.toast(MR.strings.pref_chimanew_diagnostics_empty)
                            } else {
                                val count = ChimanewDiagnostics.getLogs().size
                                context.copyToClipboard("Nuvio Diagnostics", text)
                                context.toast(context.contextStringResource(MR.strings.pref_chimanew_diagnostics_copied, count))
                            }
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_chimanew_clear_diagnostics),
                        subtitle = stringResource(MR.strings.pref_chimanew_clear_diagnostics_summary),
                        enabled = enableDiagnostics,
                        onClick = {
                            ChimanewDiagnostics.clear()
                            context.toast(MR.strings.pref_chimanew_diagnostics_cleared)
                        },
                    ),
                ),
            ),
        )
    }
}
