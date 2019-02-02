package com.quizplanner.quizPlanner.model

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.DatabaseTable
import com.j256.ormlite.table.TableUtils
import com.quizplanner.quizPlanner.QuizPlanner
import java.util.*


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
        private var gamesDateDao: Dao<GamesDate, String>? = null
        private var checkedGamesDao: Dao<CheckedGames, String>? = null

        override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
            try {
                TableUtils.createTable(connectionSource, QuizData::class.java)
                TableUtils.createTable(connectionSource, GamesDate::class.java)
                TableUtils.createTable(connectionSource, CheckedGames::class.java)
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


        @Throws(SQLException::class, java.sql.SQLException::class)
        internal fun getGamesDateDao(): Dao<GamesDate, String> {
            if (gamesDateDao == null) {
                gamesDateDao = getDao(GamesDate::class.java)
            }
            return gamesDateDao!!
        }

        @Throws(SQLException::class, java.sql.SQLException::class)
        internal fun getCheckedGamesDao(): Dao<CheckedGames, String> {
            if (checkedGamesDao == null) {
                checkedGamesDao = getDao(CheckedGames::class.java)
            }
            return checkedGamesDao!!
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
    @DatabaseTable(tableName = GamesDate.TABLE)
    class GamesDate() {

        companion object {
            const val TABLE = "games_date"
        }

        object Column {
            const val ID = "id"
            const val DATE_MS = "date_ms"
        }

        @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = Column.ID, id = true)
        var id: String? = null

        @DatabaseField(canBeNull = false, dataType = DataType.LONG_OBJ, columnName = Column.DATE_MS)
        var dateMs: Long? = null

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

    class DAO(context: Context) {
        private val mDbHelper: DbHelper = DbHelper.getInstance(context)

        fun getGames(): List<Quiz> {
            return getQuizList(mDbHelper.getQuizDataDao().queryForAll())
        }

        fun saveGames(games: List<QuizData>): List<Quiz> {
            val dao = mDbHelper.getQuizDataDao()

            for (f in games) {
                dao.createOrUpdate(f)
            }

            return getQuizList(games)
        }

        fun setCheckedGame(game: Quiz) {
            mDbHelper.getCheckedGamesDao().create(CheckedGames(game.id))
        }

        fun setUncheckedGame(game: Quiz) {
            mDbHelper.getCheckedGamesDao().deleteById(game.id)
        }

        fun clearGames(date: Date): Int {
            val dao = mDbHelper.getGamesDateDao()
            val gamesToDel = dao.queryBuilder().where().lt(GamesDate.Column.DATE_MS, date.time).query()

            val quizDao = mDbHelper.getQuizDataDao()
            val checkedGamesDao = mDbHelper.getCheckedGamesDao()
            for (g in gamesToDel) {
                quizDao.deleteById(g.id)
                checkedGamesDao.deleteById(g.id)
            }

            return gamesToDel.size
        }

        private fun getQuizList(games: List<QuizData>): MutableList<Quiz> {
            val checkedGamesDao = mDbHelper.getCheckedGamesDao()
            return games.map { getQuiz(it, checkedGamesDao.contains(CheckedGames(it.id))) }.toMutableList()
        }

        private fun getQuiz(quizData: QuizData, isCheckedGame: Boolean): Quiz {
            return Quiz(quizData.id!!,
                    quizData.organization!!,
                    quizData.gameTheme!!,
                    quizData.description!!,
                    QuizPlanner.formatterISO.parse(quizData.date),
                    quizData.time!!,
                    quizData.location!!,
                    quizData.price!!,
                    quizData.countOfPlayers!!,
                    quizData.difficulty!!,
                    quizData.registrationLink!!,
                    "",
                    isCheckedGame)
        }
    }
}
