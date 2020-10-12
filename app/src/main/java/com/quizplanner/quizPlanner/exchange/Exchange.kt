package com.quizplanner.quizPlanner.exchange

import android.content.Context
import com.quizplanner.quizPlanner.R
import okhttp3.RequestBody
import retrofit2.http.*
import rx.Observable


/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */

//---------------------------------------------------------------------------------------------------
class Favorites(private var gamesArray: List<String>)

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
    @POST(Urls.REQUEST.GAMES)
    fun getGames(@Field("dateFrom") dateFrom: String, @Field("dateTo") dateTo: String, @Field("isOnlineGame") isOnlineGame: Int): Observable<List<Input.QuizData>>

    @POST(Urls.REQUEST.FAVORITES)
    fun getGames(@Body gamesArray: Favorites): Observable<List<Input.QuizData>>
}