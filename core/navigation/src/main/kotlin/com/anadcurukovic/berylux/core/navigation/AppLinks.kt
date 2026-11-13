package com.anadcurukovic.berylux.core.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Intents that open the app at a place, from outside it: the morning docket and the home screen
 * widgets. They name the app's launcher activity, never its class, so a widget module needs no
 * dependency on the app.
 */
object AppLinks {
    const val OPEN_TODAY = "com.anadcurukovic.berylux.OPEN_TODAY"

    // The launcher's shortcuts name these two in res/xml/shortcuts.xml.
    const val OPEN_CASES = "com.anadcurukovic.berylux.OPEN_CASES"
    const val WRITE_CASE = "com.anadcurukovic.berylux.WRITE_CASE"
    const val CLOSE_CASE = "com.anadcurukovic.berylux.CLOSE_CASE"

    /** Opens the app as the launcher does: the welcome screen before there's a player. */
    fun open(context: Context): Intent = launcher(context, Intent.ACTION_MAIN)

    /** Opens the app on Today, over a running one or as a fresh start. */
    fun openToday(context: Context): Intent = launcher(context, OPEN_TODAY)

    /**
     * Opens Cases with "Close it?" asked for [caseId]. The id goes in the data, not an extra:
     * Android tells intents apart by their data and ignores extras. Each case gets its own link.
     */
    fun closeCase(context: Context, caseId: Long): Intent =
        launcher(context, CLOSE_CASE).setData(Uri.parse("$SCHEME://$CASE/$caseId"))

    /** The case a [closeCase] link is for, or null for any other intent. */
    fun caseToClose(intent: Intent): Long? =
        intent.data?.takeIf { intent.action == CLOSE_CASE && it.host == CASE }?.lastPathSegment?.toLongOrNull()

    private fun launcher(context: Context, action: String): Intent {
        val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) {
            "the app has no launcher activity"
        }
        return Intent(action)
            .setComponent(launch.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private const val SCHEME = "berylux"
    private const val CASE = "case"
}
