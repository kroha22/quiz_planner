package com.quizplanner.quizPlanner.exchange

import android.content.Context
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.QuizData
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
        const val GAMES = "getGamesList"
    }

    fun getApiBaseUrl(context: Context): String {
        return context.getString(R.string.base_api_url)
    }
}

//---------------------------------------------------------------------------------------------------

interface Api {

    @FormUrlEncoded
    @POST(Urls.REQUEST.GAMES)
    fun getGames(@Field("dateFrom") dateFrom: String, @Field("dateTo") dateTo: String): Observable<List<QuizData>>
}