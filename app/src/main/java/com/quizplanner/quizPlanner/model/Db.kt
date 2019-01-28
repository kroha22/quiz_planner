package com.quizplanner.quizPlanner.model

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils

/**
 * Created by Olga
 * on 17.01.2017.
 */
object Db {
    //--------------------------------------------------------------------------------------------
    private const val DB_VERSION = 1

    //--------------------------------------------------------------------------------------------
    class DbHelper(context: Context) : OrmLiteSqliteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        private var quizDataDao: Dao<QuizData, String>? = null

        override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
            try {
                TableUtils.createTable(connectionSource, QuizData::class.java)
            } catch (e: java.sql.SQLException) {
                e.printStackTrace()
            }

        }

        override fun onUpgrade(db: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {
            /**/
        }

        @Throws(SQLException::class, java.sql.SQLException::class)
        internal fun getQuizDataDao(): Dao<QuizData, String> {
            if (quizDataDao == null) {
                quizDataDao = getDao(QuizData::class.java)
            }
            return quizDataDao!!
        }

        companion object {
            private val DB_NAME = "sib_weather"

            private var helper: DbHelper? = null

            fun getInstance(context: Context): DbHelper {
                if (helper == null) {
                    synchronized(DbHelper::class.java) {
                        if (helper == null) {
                            helper = OpenHelperManager.getHelper(context, DbHelper::class.java)
                        }
                    }
                }

                return helper!!
            }
        }

    }
    //--------------------------------------------------------------------------------------------

    class DAO(context: Context) {
        private val mDbHelper: DbHelper = DbHelper.getInstance(context)

        //todo clear old data
        fun getGames(): List<QuizData>? {
            return try {
                mDbHelper.getQuizDataDao().queryForAll()

            } catch (e: java.sql.SQLException) {
                e.printStackTrace()
                null
            }
        }

        fun saveGames(games: List<QuizData>) {
            try {
                val forecastsDao = mDbHelper.getQuizDataDao()
                forecastsDao.deleteBuilder().delete()

                for (f in games) {
                    forecastsDao.createOrUpdate(f)
                }
            } catch (e: java.sql.SQLException) {
                e.printStackTrace()
            }

        }

    }
}
