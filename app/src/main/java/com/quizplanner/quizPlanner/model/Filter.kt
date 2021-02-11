package com.quizplanner.quizPlanner.model

import androidx.annotation.StringRes
import com.quizplanner.quizPlanner.R


/**
 *  Filter.kt
 *
 *  Created by Olga Cherepanova on 22.09.2020.
 *  Copyright © 2020 SocElectroProject. All rights reserved.
 */

enum class Filter(@StringRes val descriptionRes: Int, val code: Int) {
    ONLINE(R.string.online, 1), OFFLINE(R.string.offline, 2);

    companion object {
        fun getByCode(code: Int): Filter {
            for (m in values()) {
                if (m.code == code) {
                    return m
                }
            }

            throw AssertionError("Error Filter code $code")
        }
    }
}