package com.quizplanner.quizPlanner.model

import android.support.annotation.StringRes
import com.quizplanner.quizPlanner.R


/**
 *  Filter.kt
 *
 *  Created by Olga Cherepanova on 22.09.2020.
 *  Copyright © 2020 SocElectroProject. All rights reserved.
 */

enum class Filter(@StringRes val descriptionRes: Int) {
    ONLINE(R.string.online), OFFLINE(R.string.offline)
}