package com.quizplanner.quizPlanner.model

import java.io.Serializable
import java.util.*

/**
 * Created by Olga Cherepanova
 * on 20.01.2019.
 */
data class Quiz(val id: String,
                val organization: String,
                val gameTheme: String,
                val description: String,
                val date: Date,
                val time: String,
                val location: String,
                val price: String,
                val countOfPlayers: String,
                val difficulty: String,
                val registrationLink: String,
                val imgUrl: String,
                var isChecked: Boolean): Serializable {

    override fun toString(): String {
        return "Quiz(id='$id', organization='$organization', gameTheme='$gameTheme', description='$description', date=$date, time='$time', location='$location', price='$price', countOfPlayers='$countOfPlayers', difficulty='$difficulty', registrationLink='$registrationLink', imgUrl='$imgUrl')"
    }
}