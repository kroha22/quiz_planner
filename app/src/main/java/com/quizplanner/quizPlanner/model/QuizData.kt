package com.quizplanner.quizPlanner.model

import com.google.gson.annotations.SerializedName
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.quizplanner.quizPlanner.QuizPlanner.formatterISO

/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */
@DatabaseTable(tableName = QuizData.TABLE)
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
    companion object {
        const val TABLE = "quiz_data"
    }

    object Column {
        const val ID = "id"
        const val ORGANIZATION_NAME = "organization_name"
        const val GAME_THEME = "game_theme"
        const val DESCRIPTION = "description"
        const val DATE = "date"
        const val TIME = "time"
        const val LOCATION = "location"
        const val PRICE = "price"
        const val COUNT_OF_PLAYERS = "count_of_players"
        const val DIFFICULTY = "difficulty"
        const val REGISTRATION_LINK = "registration_link"
    }

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.ID, id = true)
    @SerializedName("_id")
    var id: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.ORGANIZATION_NAME)
    @SerializedName("organization_name")
    var organization: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.GAME_THEME)
    @SerializedName("game_theme")
    var gameTheme: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.DESCRIPTION)
    @SerializedName("desctiprtion")
    var description: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.DATE)
    @SerializedName("Date")
    var date: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.TIME)
    @SerializedName("time")
    var time: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.LOCATION)
    @SerializedName("location")
    var location: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.PRICE)
    @SerializedName("price")
    var price: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.COUNT_OF_PLAYERS)
    @SerializedName("count_of_players_in_team")
    var countOfPlayers: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.DIFFICULTY)
    @SerializedName("game_difficulty")
    var difficulty: String? = null
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.REGISTRATION_LINK)
    @SerializedName("regestration_link")
    var registrationLink: String? = null

    override fun toString(): String {
        return "QuizData(id=$id, organization=$organization, gameTheme=$gameTheme, description=$description, date=$date, time=$time, location=$location, price=$price, countOfPlayers=$countOfPlayers, difficulty=$difficulty, registrationLink=$registrationLink)"
    }

    constructor()

    fun getQuiz(): Quiz {
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

