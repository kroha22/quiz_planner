package com.quizplanner.quizPlanner.exchange

import android.content.Context
import com.quizplanner.quizPlanner.R
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import rx.Observable


/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */

//---------------------------------------------------------------------------------------------------

object Urls {

    object REQUEST {
        const val GAMES = "monthGamesList"
        const val FAVORITES = "favouriteGamesList"
    }

    fun getApiBaseUrl(context: Context): String {
        return context.getString(R.string.base_api_url)
    }
}

//---------------------------------------------------------------------------------------------------

interface Api {

    @FormUrlEncoded
    @POST(Urls.REQUEST.GAMES)
    fun getGames(@Field("dateFrom") dateFrom: String, @Field("dateTo") dateTo: String): Observable<List<Input.QuizData>>

    @FormUrlEncoded
    @POST(Urls.REQUEST.FAVORITES)
    fun getGames(@Field("gamesArray") gamesArray: List<String>): Observable<List<Input.QuizData>>
}