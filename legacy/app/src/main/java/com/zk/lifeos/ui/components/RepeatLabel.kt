package com.zk.lifeos.ui.components

import androidx.annotation.StringRes
import com.zk.lifeos.R
import com.zk.lifeos.model.RepeatRule

/**
 * Wording for the repeat intervals.
 *
 * Lives in the UI layer rather than on the enum itself: `model/` has no business knowing about `R`,
 * and the same rule reads differently in a chip (「每周」) than it would anywhere else.
 */
@get:StringRes
internal val RepeatRule.labelRes: Int
    get() = when (this) {
        RepeatRule.DAILY -> R.string.repeat_daily
        RepeatRule.WEEKLY -> R.string.repeat_weekly
        RepeatRule.MONTHLY -> R.string.repeat_monthly
        RepeatRule.YEARLY -> R.string.repeat_yearly
    }
