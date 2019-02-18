package com.quizplanner.quizPlanner.exchange

import com.google.gson.annotations.SerializedName

/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */
object Input {
    //--------------------------------------------------------------------------------
    class QuizData {

        @SerializedName("_id")
        var id: String? = null
        @SerializedName("gameTheme")
        var gameTheme: String? = null
        @SerializedName("description")
        var description: String? = null
        @SerializedName("date")
        var date: String? = null
        @SerializedName("location")
        var location: String? = null
        @SerializedName("price")
        var price: Int? = null
        @SerializedName("countOfPlayers")
        var countOfPlayers: Int? = null
        @SerializedName("gameDifficulty")
        var difficulty: String? = null
        @SerializedName("registrationLink")
        var registrationLink: String? = null
        @SerializedName("gameImgFilename")
        var gameImgFilename: String? = null
        @SerializedName("gameImgPath")
        var gameImgPath: String? = null
        @SerializedName("author")
        var author: Author? = null

        constructor()

        override fun toString(): String {
            return "QuizData(id=$id, gameTheme=$gameTheme, description=$description, date=$date, location=$location, price=$price, countOfPlayers=$countOfPlayers, difficulty=$difficulty, registrationLink=$registrationLink, gameImgFilename=$gameImgFilename, gameImgPath=$gameImgPath, author=$author)"
        }

    }

    //--------------------------------------------------------------------------------
    class Author {

        @SerializedName("_id")
        var id: String? = null
        @SerializedName("organisationName")
        var organisationName: String? = null
        @SerializedName("organisationLogoFilename")
        var organisationLogoFilename: String? = null
        @SerializedName("organisationLogoPath")
        var organisationLogoPath: String? = null

        constructor()

        override fun toString(): String {
            return "Author(id=$id, organisationName=$organisationName, organisationLogoFilename=$organisationLogoFilename, organisationLogoPath=$organisationLogoPath)"
        }

    }
    //--------------------------------------------------------------------------------

}

