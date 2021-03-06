package com.quizplanner.quizPlanner.model

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.exchange.Input
import java.io.Serializable
import java.util.*

@DatabaseTable(tableName = Quiz.TABLE)
class Quiz : Serializable {

    companion object {
        const val TABLE = "game_data"

        fun fromQuizData(quizData: Input.QuizData): Quiz {
            val game = Quiz()

            game.id = quizData.id
            game.gameTheme = quizData.gameTheme
            game.description = quizData.description
            game.date = QuizPlanner.formatterISO().parse(quizData.date!!).time
            game.location = quizData.location
            game.price = quizData.price
            game.countOfPlayers = quizData.countOfPlayers
            game.registrationLink = quizData.registrationLink
            game.gameImgFilename = quizData.gameImgFilename
            game.gameImgPath = quizData.gameImgPath

            if (quizData.organisationName != null && quizData.organisationLogoFilename != null && quizData.organisationLogoPath != null) {
                game.organisationName = quizData.organisationName
                game.organisationLogoFilename = quizData.organisationLogoFilename
                game.organisationLogoPath = quizData.organisationLogoPath
            } else {
                game.organisationName = quizData.author!!.organisationName
                game.organisationLogoFilename = quizData.author!!.organisationLogoFilename
                game.organisationLogoPath = quizData.author!!.organisationLogoPath
            }

            val isGamePostponed = quizData.isGamePostponed?:false
            if (isGamePostponed) {
                game.gamePostponed = 1
            } else {
                game.gamePostponed = 0
            }

            val isOnlineGame = quizData.isOnlineGame?:false
            if (isOnlineGame) {
                game.isOnlineGame = 1
            } else {
                game.isOnlineGame = 0
            }

            return game
        }
    }

    object Column {
        const val ID = "id"
        const val GAME_THEME = "game_theme"
        const val DESCRIPTION = "description"
        const val DATE = "date"
        const val LOCATION = "location"
        const val PRICE = "price"
        const val COUNT_OF_PLAYERS = "count_of_players"
        const val REGISTRATION_LINK = "registration_link"
        const val GAME_IMG_FILENAME = "gameImgFilename"
        const val GAME_IMG_PATH = "gameImgPath"
        const val ORGANIZATION_NAME = "organization_name"
        const val ORGANIZATION_LOGO_FILENAME = "organization_logo_filename"
        const val ORGANIZATION_LOGO_PATH = "organization_logo_path"
        const val GAME_POSTPONED = "game_postponed"
        const val ONLINE = "online"
    }

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.ID, id = true)
    var id: String? = null

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.GAME_THEME)
    var gameTheme: String? = null

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.DESCRIPTION)
    var description: String? = null

    @DatabaseField(canBeNull = false, dataType = DataType.LONG_OBJ, columnName = Column.DATE)
    var date: Long? = null

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.LOCATION)
    var location: String? = null

    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER_OBJ, columnName = Column.PRICE)
    var price: Int? = null

    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER_OBJ, columnName = Column.COUNT_OF_PLAYERS)
    var countOfPlayers: Int? = null

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.REGISTRATION_LINK)
    var registrationLink: String? = null

    @DatabaseField(canBeNull = true, dataType = DataType.STRING, columnName = Column.GAME_IMG_FILENAME)
    var gameImgFilename: String? = null

    @DatabaseField(canBeNull = true, dataType = DataType.STRING, columnName = Column.GAME_IMG_PATH)
    var gameImgPath: String? = null

    @DatabaseField(canBeNull = true, dataType = DataType.STRING, columnName = Column.ORGANIZATION_NAME)
    var organisationName: String? = null

    @DatabaseField(canBeNull = true, dataType = DataType.STRING, columnName = Column.ORGANIZATION_LOGO_FILENAME)
    var organisationLogoFilename: String? = null

    @DatabaseField(canBeNull = true, dataType = DataType.STRING, columnName = Column.ORGANIZATION_LOGO_PATH)
    var organisationLogoPath: String? = null

    @DatabaseField(canBeNull = true, dataType = DataType.INTEGER_OBJ, columnName = Column.GAME_POSTPONED)
    var gamePostponed: Int? = null

    @DatabaseField(canBeNull = true, dataType = DataType.INTEGER_OBJ, columnName = Column.ONLINE)
    var isOnlineGame: Int? = null

    var isChecked = false

    constructor()

    fun isGamePostponed() = gamePostponed == 1

    fun isOnlineGame() = isOnlineGame == 1

    override fun toString(): String {
        return "Quiz(id=$id, gameTheme=$gameTheme, description=$description, " +
                "date=$date, location=$location, price=$price, " +
                "countOfPlayers=$countOfPlayers, registrationLink=$registrationLink, " +
                "gameImgFilename=$gameImgFilename, gameImgPath=$gameImgPath," +
                " organisationName=$organisationName, organisationLogoFilename=$organisationLogoFilename, " +
                " gamePostponed=${isGamePostponed()}, isOnlineGame=${isOnlineGame()}, " +
                "organisationLogoPath=$organisationLogoPath)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Quiz

        return id.equals(other.id)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    fun getImgUrl() = if (gameImgPath != null && gameImgFilename != null) {
        "${gameImgPath!!}/${gameImgFilename!!}"
    } else {
        ""
    }

    fun getLogoUrl() = if (organisationLogoPath != null && organisationLogoFilename != null) {
        "${organisationLogoPath!!}/${organisationLogoFilename!!}"
    } else {
        ""
    }

    fun getDate() = Date(date!!)

    fun identical(quiz: Quiz): Boolean {
        return id == quiz.id &&
                gameTheme == quiz.gameTheme &&
                description == quiz.description &&
                date == quiz.date &&
                location == quiz.location &&
                price == quiz.price &&
                countOfPlayers == quiz.countOfPlayers &&
                registrationLink == quiz.registrationLink &&
                gameImgFilename == quiz.gameImgFilename &&
                gameImgPath == quiz.gameImgPath &&
                organisationName == quiz.organisationName &&
                organisationLogoFilename == quiz.organisationLogoFilename &&
                organisationLogoPath == quiz.organisationLogoPath &&
                isChecked == quiz.isChecked
    }

    fun equalQuizData(quizData: Input.QuizData): Boolean {
        return id == quizData.id &&
                gameTheme == quizData.gameTheme &&
                description == quizData.description &&
                date == QuizPlanner.formatterISO().parse(quizData.date!!).time &&
                location == quizData.location &&
                price == quizData.price &&
                countOfPlayers == quizData.countOfPlayers &&
                registrationLink == quizData.registrationLink &&
                gameImgFilename == quizData.gameImgFilename &&
                gameImgPath == quizData.gameImgPath &&
                equalOrganisationData(quizData)
    }

    private fun equalOrganisationData(quizData: Input.QuizData): Boolean {

        return if (quizData.organisationName != null && quizData.organisationLogoFilename != null && quizData.organisationLogoPath != null) {
            organisationName == quizData.organisationName &&
                    organisationLogoFilename == quizData.organisationLogoFilename &&
                    organisationLogoPath == quizData.organisationLogoPath
        } else {
            organisationName == quizData.author!!.organisationName &&
                    organisationLogoFilename == quizData.author!!.organisationLogoFilename &&
                    organisationLogoPath == quizData.author!!.organisationLogoPath
        }
    }

}

//--------------------------------------------------------------------------------------------
@DatabaseTable(tableName = CheckedGames.TABLE)
class CheckedGames {

    companion object {
        const val TABLE = "checked_games"
    }

    object Column {
        const val ID = "id"
    }

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.ID, id = true)
    var id: String? = null

    constructor()

    constructor(id: String?) {
        this.id = id
    }

}

//--------------------------------------------------------------------------------------------
@DatabaseTable(tableName = FiltersList.TABLE)
class FiltersList {

    companion object {
        const val TABLE = "filters_list"
    }

    object Column {
        const val LIST = "list"
    }

    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.LIST, id = true)
    var list: String? = null

    constructor()

    constructor(list: String?) {
        this.list = list
    }

}

//--------------------------------------------------------------------------------------------
