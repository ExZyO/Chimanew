@file:Suppress("PackageDirectoryMismatch")

package androidx.preference

import android.os.Bundle
import android.view.View

/**
 * Returns package-private [EditTextPreference.getOnBindEditTextListener]
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun EditTextPreference.getOnBindEditTextListener(): EditTextPreference.OnBindEditTextListener? {
    return onBindEditTextListener
}

class SafeEditTextPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        val pref = runCatching { preference }.getOrNull()
        if (pref == null) {
            dismissAllowingStateLoss()
            return
        }
        super.onBindDialogView(view)
    }

    companion object {
        fun newInstance(key: String): SafeEditTextPreferenceDialogFragment {
            return SafeEditTextPreferenceDialogFragment().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_KEY, key)
                }
            }
        }
    }
}
