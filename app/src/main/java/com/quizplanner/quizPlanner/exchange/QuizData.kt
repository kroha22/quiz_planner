package com.quizplanner.quizPlanner.exchange

import com.google.gson.annotations.SerializedName
import com.quizplanner.quizPlanner.QuizPlanner.formatterISO
import com.quizplanner.quizPlanner.model.Quiz

/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */
class QuizData {

    /*
    [{
    "_id":"5c492c6ede5aa1159efa07c6",
    "organization_name":"Алалала",
    "game_theme":"Олололо",
    "desctiprtion":"Описание",
    "Date":"2019-01-25T00:00:00.000Z",
    "time":"22:00",
    "location":"Новосибирск",
    "price":2000,
    "count_of_players_in_team":4,
    "game_difficulty":"HARD",
    "regestration_link":"www.ya.ru",
    "__v":0}]
     */
    @SerializedName("_id")
    var id: String? = null
    @SerializedName("organization_name")
    var organization: String? = null
    @SerializedName("game_theme")
    var gameTheme: String? = null
    @SerializedName("desctiprtion")
    var description: String? = null
    @SerializedName("Date")
    var date: String? = null
    @SerializedName("time")
    var time: String? = null
    @SerializedName("location")
    var location: String? = null
    @SerializedName("price")
    var price: String? = null
    @SerializedName("count_of_players_in_team")
    var countOfPlayers: String? = null
    @SerializedName("game_difficulty")
    var difficulty: String? = null
    @SerializedName("regestration_link")
    var registrationLink: String? = null

    override fun toString(): String {
        return "QuizData(id=$id, organization=$organization, gameTheme=$gameTheme, description=$description, date=$date, time=$time, location=$location, price=$price, countOfPlayers=$countOfPlayers, difficulty=$difficulty, registrationLink=$registrationLink)"
    }

    fun getQuiz(): Quiz{
        return Quiz(id!!,
                organization!!,
                gameTheme!!,
                description!!,
                formatterISO.parse(date),
                time!!,
                location!!,
                price!!,
                countOfPlayers!!,
                difficulty!!,
                registrationLink!!,
                "")
    }

}

