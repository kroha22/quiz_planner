package com.quizplanner.quizPlanner.model

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.exchange.Input
import java.util.*


/**
 * Created by Olga
 * on 17.01.2017.
 */
object Db {
    //--------------------------------------------------------------------------------------------
    private const val DB_VERSION = 3

    //--------------------------------------------------------------------------------------------
    class DbHelper(context: Context) : OrmLiteSqliteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        private var quizDao: Dao<Quiz, String>? = null
        private var checkedGamesDao: Dao<CheckedGames, String>? = null
        private var filtersListDao: Dao<FiltersList, String>? = null

        override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
            try {
                TableUtils.createTable(connectionSource, Quiz::class.java)
                TableUtils.createTable(connectionSource, CheckedGames::class.java)
                TableUtils.createTable(connectionSource, FiltersList::class.java)
            } catch (e: java.sql.SQLException) {
                e.printStackTrace()
            }

        }

        override fun onUpgrade(db: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {
            if (oldVersion == 1 && newVersion == 2) {
                getGamesDateDao().executeRaw("ALTER TABLE ${Quiz.TABLE} ADD COLUMN ${Quiz.Column.GAME_POSTPONED} INTEGER;")
            } else if (oldVersion < 3 && newVersion == 3) {
                getGamesDateDao().executeRaw("ALTER TABLE ${Quiz.TABLE} ADD COLUMN ${Quiz.Column.ONLINE} INTEGER;")
                TableUtils.createTable(connectionSource, FiltersList::class.java)
            }
        }

        @Throws(SQLException::class, java.sql.SQLException::class)
        internal fun getGamesDateDao(): Dao<Quiz, String> {
            if (quizDao == null) {
                quizDao = getDao(Quiz::class.java)
            }
            return quizDao!!
        }

        @Throws(SQLException::class, java.sql.SQLException::class)
        internal fun getCheckedGamesDao(): Dao<CheckedGames, String> {
            if (checkedGamesDao == null) {
                checkedGamesDao = getDao(CheckedGames::class.java)
            }
            return checkedGamesDao!!
        }

        @Throws(SQLException::class, java.sql.SQLException::class)
        internal fun getFiltersListDao(): Dao<FiltersList, String> {
            if (filtersListDao == null) {
                filtersListDao = getDao(FiltersList::class.java)
            }
            return filtersListDao!!
        }

        companion object {
            private const val DB_NAME = "quiz_planner"

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
        //--------------------------------------------------------------------------------------------
        companion object {
            private val TAG: String = DAO::class.java.name
            private fun log(msg: String) {
                QuizPlanner.log(TAG, msg)
            }
        }
        //--------------------------------------------------------------------------------------------

        private val mDbHelper: DbHelper = DbHelper.getInstance(context)

        fun getGames(): List<Quiz> {
            val games = getQuizList(mDbHelper.getGamesDateDao().queryForAll())
            log("getGames ${games.map { it.id }.toList()}")
            return games
        }

        fun getGames(author: String): List<Quiz> {
            val games = getQuizList(mDbHelper.getGamesDateDao().queryBuilder().where().eq(Quiz.Column.ORGANIZATION_NAME, author).query())
            log("getGames ${games.map { it.id }.toList()}")
            return games
        }

        fun saveGames(games: List<Input.QuizData>): List<Quiz> {
            val dao = mDbHelper.getGamesDateDao()

            var newGames = 0
            var updatedGames = 0
            for (quiz in games) {
                val status = dao.createOrUpdate(Quiz.fromQuizData(quiz))
                if (status.isCreated) {
                    newGames++
                }
                if (status.isUpdated) {
                    updatedGames++
                }
            }
            log("saveGames, count =${games.size}, newGames = $newGames, updatedGames = $updatedGames")

            return getQuizList(games.map { Quiz.fromQuizData(it) })
        }

        fun setCheckedGame(game: Quiz) {
            mDbHelper.getCheckedGamesDao().create(CheckedGames(game.id))
            log("setCheckedGame, game =${game.id}")
        }

        fun setFiltersList(filtersList: List<Filter>) {
            val old =  mDbHelper.getFiltersListDao().queryForAll()
            if(old.isNotEmpty()) {
                mDbHelper.getFiltersListDao().delete(old)
                log("setFiltersList, clear old entry, count ${old.size}")
            }

            if (filtersList.isEmpty()){
                log("setFiltersList, filtersList empty")
                return
            }

            val str = filtersList.map { it.code }.joinToString(";")
            mDbHelper.getFiltersListDao().create(FiltersList(str))
            log("setFiltersList, filtersList =${str}")
        }

        fun getFiltersList(): List<Filter>{
            val all = mDbHelper.getFiltersListDao().queryForAll()
            if(all.isEmpty()){
                log("getFiltersList, table is empty")
                return emptyList()
            }

            var str = all.last().list
            log("getFiltersList, filtersList =${str}")

            if(str == null){
                return emptyList()
            }

            return try {
                if(str.takeLast(1) == ";"){
                    str = str.substring(0, str.length - 1)
                }

                str.split(";").map { Filter.getByCode(it.toInt()) }
            } catch (e: Exception) {
                Log.e("QuizPlanner", "Restore filters error", e)
                emptyList()
            }
        }

        fun isChecked(id: String): Boolean {
            val isChecked = !mDbHelper.getCheckedGamesDao().queryForEq(CheckedGames.Column.ID, id).isEmpty()

            log("game =$id isChecked = $isChecked")

            return isChecked
        }

        fun setUncheckedGame(game: Quiz) {
            mDbHelper.getCheckedGamesDao().deleteById(game.id)
            log("setUncheckedGame, game =${game.id}")
        }

        fun delete(gamesToDel: ArrayList<Quiz>): Int {
            val dao = mDbHelper.getGamesDateDao()

            return delete(dao, gamesToDel)
        }

        fun clearGames(date: Date): Int {
            val dao = mDbHelper.getGamesDateDao()
            val gamesToDel = dao.queryBuilder().where().lt(Quiz.Column.DATE, date.time).query()

            return delete(dao, gamesToDel)
        }

        private fun delete(dao: Dao<Quiz, String>, gamesToDel: MutableList<Quiz>): Int {
            val checkedGamesDao = mDbHelper.getCheckedGamesDao()
            for (g in gamesToDel) {
                dao.deleteById(g.id)
                checkedGamesDao.deleteById(g.id)
            }
            log("clearGames, count =${gamesToDel.size}")

            return gamesToDel.size
        }

        private fun getQuizList(games: List<Quiz>): MutableList<Quiz> {
            val checkedGamesDao = mDbHelper.getCheckedGamesDao()
            return games.map { getQuiz(it, checkedGamesDao.queryForId(it.id) != null) }.toMutableList()
        }

        private fun getQuiz(quiz: Quiz, isCheckedGame: Boolean): Quiz {
            quiz.isChecked = isCheckedGame
            return quiz
        }
    }
}
