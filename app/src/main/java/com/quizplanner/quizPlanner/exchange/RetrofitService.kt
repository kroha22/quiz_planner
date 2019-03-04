package com.quizplanner.quizPlanner.exchange

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Observable

/**
 * Created by Olga Cherepanova
 * on 28.01.2019.
 */

//---------------------------------------------------------------------------------------------------
class RetrofitService private constructor(context: Context) {
//---------------------------------------------------------------------------------------------------

    companion object {
        //-------------------------------------------------------------------------------------------
        private var sInstance: RetrofitService? = null

        //-------------------------------------------------------------------------------------------

        fun getInstance(context: Context): RetrofitService {
            if (sInstance == null) {
                synchronized(RetrofitService::class.java) {
                    if (sInstance == null) {
                        sInstance = RetrofitService(context)
                    }
                }
            }

            return sInstance!!
        }
        //-------------------------------------------------------------------------------------------

        private fun getOkHttpClientBuilder(logInterceptor: HttpLoggingInterceptor): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                    .addInterceptor(logInterceptor)
        }
    }
//---------------------------------------------------------------------------------------------------

    private val mRetrofit: Retrofit

    init {
        val logInterceptor = HttpLoggingInterceptor()
        logInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = getOkHttpClientBuilder(logInterceptor).build()

        mRetrofit = Retrofit.Builder()
                .baseUrl(Urls.getApiBaseUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build()
    }

    fun getQuizData(dateFrom: String, dateTo: String): Observable<List<Input.QuizData>> {
        return createApiService(Api::class.java)
                .getGames(dateFrom, dateTo)
    }

    fun getQuizData(gamesArray: List<String>): Observable<List<Input.QuizData>> {
        return createApiService(Api::class.java)
                .getGames(gamesArray)
    }

    internal fun <S> createApiService(apiClass: Class<S>): S {
        return mRetrofit.create(apiClass)
    }

}