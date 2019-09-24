package com.quizplanner.quizPlanner.exchange

import com.google.gson.annotations.SerializedName

/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */
object Input {
    //--------------------------------------------------------------------------------
    class QuizData {

        /*
        [{
        "organisationName":"QUIZ-CLUB",
        "organisationLogoPath":"/admin/693e0b1da9f67c7b6e80041bb623b254",
        "organisationLogoFilename":"NASTYA.jpg",
        "_id":"5ca10c1182cd87098b900b50",
        "gameTheme":"Чёрный квиз",
        "description":"Традиционно, весной и осенью, уже 9-й год, мы проводим Черные игры.  1.2, 3 и 4 апреля ждём вас на Черные квизы - самые популярные игры в нашем квиз-клубе. Немедленно регистрируйтесь!  Черный квиз - это 60 вопросов о том, что вы боялись говорить в компании приличных людей! Черный квиз - это 4 часа ада и смеха сквозь слезы! Черный квиз - это больные скулы на утро, а у кого-то и голова! Черный квиз - это когда самые не трезвые игроки занимают самые первые места! Черный квиз - это игра, где тебе дозволено все, кроме ханжества!  Людям, глубоко верующих, что в СССР секса не было, просьба воздержаться от этой игры) Людей, пришедших на игру без чувства юмора - рекомендуем выпить перед игрой!) Людей, думающих, что в смерти и сексе нет юмора - переубедим))  Вход строго 18+!!! Вопросы \"ниже пояса\" и ненормативная лексика будут обязательно! Будьте осторожны! ______________________________________________ ДРЕСС-КОД - КРАСНОЕ И ЧЕРНОЕ! БДСМ атрибутика приветствуется",
        "date":"2019-04-01T12:00:00.000Z",
        "location":"Кабаре-кафе \"Бродячая Собака\", ул. Каменская, 32",
        "price":350,
        "countOfPlayers":8,
        "registrationLink":"https://vk.com/topic-18225558_39910715",
        "author":{
            "organisationName":"UNKNOWN",
            "organisationLogoPath":"/admin/default",
            "organisationLogoFilename":"defaultLogo.png",
            "_id":"5ca0c179523b4c06e4f3b938",
            "username":"admin"},
        "gameImgPath":"/admin/693e0b1da9f67c7b6e80041bb623b254",
        "gameImgFilename":"BLACKQUIZ.jpg","__v":0},
         */
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
        @SerializedName("registrationLink")
        var registrationLink: String? = null
        @SerializedName("gameImgFilename")
        var gameImgFilename: String? = null
        @SerializedName("gameImgPath")
        var gameImgPath: String? = null
        @SerializedName("author")
        var author: Author? = null
        @SerializedName("organisationName")
        var organisationName: String? = null
        @SerializedName("organisationLogoFilename")
        var organisationLogoFilename: String? = null
        @SerializedName("organisationLogoPath")
        var organisationLogoPath: String? = null
        @SerializedName("isGamePostponed")
        var isGamePostponed: Boolean? = null

        constructor()

        override fun toString(): String {
            return "QuizData(id=$id, gameTheme=$gameTheme, description=$description, date=$date, location=$location, price=$price, countOfPlayers=$countOfPlayers, registrationLink=$registrationLink, gameImgFilename=$gameImgFilename, gameImgPath=$gameImgPath, author=$author, isGamePostponed=$isGamePostponed)"
        }

    }

    //--------------------------------------------------------------------------------
    class Author {
        /*
        "author":{
            "organisationName":"UNKNOWN",
            "organisationLogoPath":"/admin/default",
            "organisationLogoFilename":"defaultLogo.png",
            "_id":"5ca0c179523b4c06e4f3b938",
            "username":"admin"},
            */
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

