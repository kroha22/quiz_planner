package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterTime
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import com.squareup.picasso.Picasso
import com.universalvideoview.UniversalVideoView
import kotlinx.android.synthetic.main.activity_quiz_detail.*
import kotlinx.android.synthetic.main.quiz_detail.*
import rx.Subscription


//---------------------------------------------------------------------------------------------

@StateStrategyType(SkipStrategy::class)
interface QuizDetailView : MvpView {
    fun requestLink(link: String)

    fun createNotify(quiz: Quiz)

    fun updateFavoritesView()

    fun inverseFavorites(quiz: Quiz)

    fun showAuthorGames()
}

//---------------------------------------------------------------------------------------------

class QuizDetailActivity : MvpAppCompatActivity(), QuizDetailView {

    companion object {
        val TAG = QuizDetailActivity::javaClass.name
        const val QUIZ_ITEM_CODE = "quiz_item"
        const val SOURCE_CODE = "source_code"
        const val MAIN = "main"
        const val AUTHOR = "author"
        private const val QUIZ_DETAIL: String = "quiz_detail"
        private const val SEEK_POSITION_KEY = "SEEK_POSITION_KEY"
    }

    private lateinit var item: Quiz

    private var seekPosition: Int = -1
    private var cachedHeight: Int = -1
    private var isFullscreen = false

    @InjectPresenter(tag = QUIZ_DETAIL)
    lateinit var presenter: QuizDetailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_detail)
        setSupportActionBar(detail_toolbar)
        detail_toolbar.setNavigationOnClickListener { this.onBackPressed() }

        if (savedInstanceState == null) {
            val item = intent.getSerializableExtra(QUIZ_ITEM_CODE)
                    ?: throw AssertionError()

            this.item = item as Quiz

            detail_title.text = item.organisationName
            detail_theme.text = item.gameTheme
            detail_date.text = QuizPlanner.formatterDateMonth.format(item.date)
            detail_time.text = formatterTime.format(item.date)
            detail_location.text = item.location

            if (item.countOfPlayers != null) {
                detail_count.text = item.countOfPlayers.toString()
                detail_count.visibility = View.VISIBLE
            } else {
                detail_count.visibility = View.GONE
            }

            detail_price.text = item.price.toString()
            detail_description.text = item.description

            if (item.registrationLink!!.isEmpty()) {
                detail_link_label.visibility = View.INVISIBLE
                detail_link.visibility = View.INVISIBLE
            } else {
                detail_link_label.visibility = View.VISIBLE
                detail_link.visibility = View.VISIBLE
                detail_link.text = item.registrationLink
                detail_link.setOnClickListener { presenter.onLinkClick() }
            }

            if (item.getImgUrl().isNotEmpty()) {
                val apiUrl = getString(R.string.base_api_img_url)

                Picasso.get()
                        .load(apiUrl + item.getImgUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .fit()
                        .centerInside()
                        .error(R.drawable.ic_broken_image)
                        .into(detail_img)
            }

            toolbar_calendar.setOnClickListener { presenter.onCalendarClick() }
            detail_author_games.setOnClickListener { presenter.onAuthorClick() }

            updateFavoritesView()

            detail_favorites.setOnClickListener {
                presenter.onGameCheckChanged()
            }

            if (QuizPlanner.isLast(item.getDate())) {
                detail_time_img.setColorFilter(ContextCompat.getColor(this, R.color.medium_grey))
            } else {
                detail_time_img.setColorFilter(ContextCompat.getColor(this, R.color.red))
            }

            if (intent.getStringExtra(SOURCE_CODE) == AUTHOR) {
                detail_author_games.visibility = View.GONE
            }

            setVideoAreaSize()

            val videoSource = "https://vk.com/video-77462734_456239022"
            detail_video_layout.post {detail_video_view.setVideoPath(videoSource)}

            detail_media_controller.setOnErrorView(layoutInflater.inflate(R.layout.video_error_view, null, false) as LinearLayout)
            detail_video_view.setMediaController(detail_media_controller)
            detail_video_view.setVideoViewCallback(getVideoViewCallback())
        }

    }

    override fun inverseFavorites(quiz: Quiz) {
        item.isChecked = quiz.isChecked
        updateFavoritesView()
    }

    override fun updateFavoritesView() {
        if (item.isChecked) {
            detail_favorites_text.text = getText(R.string.in_favorites)
            detail_favorites_img.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
        } else {
            detail_favorites_text.text = getText(R.string.add_to_favorites)
            detail_favorites_img.colorFilter = null
        }
    }

    override fun onResume() {
        super.onResume()

        if (presenter.isStarted()) {
            presenter.onResume()
            return
        }
        presenter.init(this)
        presenter.start(item)
    }

    override fun requestLink(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    override fun createNotify(quiz: Quiz) {
        val intent = Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, quiz.date)
                .putExtra(Events.TITLE, quiz.gameTheme)
                .putExtra(Events.DESCRIPTION, quiz.description)
                .putExtra(Events.EVENT_LOCATION, quiz.location)
                .putExtra(Intent.EXTRA_EMAIL, quiz.registrationLink)
        startActivity(intent)
    }

    override fun showAuthorGames() {
        val intent = Intent(this, AuthorActivity::class.java).apply {
            putExtra(AuthorActivity.AUTHOR_CODE, item.organisationName)
        }
        startActivity(intent)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState Position=" + detail_video_view.currentPosition)
        outState.putInt(SEEK_POSITION_KEY, seekPosition)
    }

    override fun onRestoreInstanceState(outState: Bundle) {
        super.onRestoreInstanceState(outState)
        seekPosition = outState.getInt(SEEK_POSITION_KEY)
        Log.d(TAG, "onRestoreInstanceState Position=$seekPosition")
    }

    override fun onBackPressed() {
        if (this.isFullscreen) {
            detail_video_view.setFullscreen(false)
        } else {
            super.onBackPressed()
        }
    }

    private fun getVideoViewCallback(): UniversalVideoView.VideoViewCallback {
        return object : UniversalVideoView.VideoViewCallback {
            override fun onBufferingStart(mediaPlayer: MediaPlayer?) {}

            override fun onBufferingEnd(mediaPlayer: MediaPlayer?) {}

            override fun onPause(mediaPlayer: MediaPlayer?) {
                if (detail_video_view.isPlaying) {
                    seekPosition = detail_video_view.currentPosition
                    Log.d(TAG, "onPause seekPosition=$seekPosition")
                    detail_video_view.pause()
                }
            }

            override fun onScaleChange(isFullscreen: Boolean) {
                this@QuizDetailActivity.isFullscreen = isFullscreen
                if (isFullscreen) {
                    val layoutParams = detail_video_layout.layoutParams
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    detail_video_layout.layoutParams = layoutParams
                    detail_media_controller.visibility = View.GONE

                } else {
                    val layoutParams = detail_video_layout.layoutParams
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    layoutParams.height = this@QuizDetailActivity.cachedHeight
                    detail_video_layout.layoutParams = layoutParams
                    detail_media_controller.visibility = View.VISIBLE
                }

                switchTitleBar(!isFullscreen)
            }

            override fun onStart(mediaPlayer: MediaPlayer?) {}
        }
    }

    private fun setVideoAreaSize() {
        detail_video_layout.post {
            val width = detail_video_layout.width
            cachedHeight = (width * 405f / 720f).toInt()
//                cachedHeight = (int) (width * 3f / 4f);
//                cachedHeight = (int) (width * 9f / 16f);

            val videoLayoutParams = detail_video_layout.layoutParams
            videoLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            videoLayoutParams.height = cachedHeight
            detail_video_layout.layoutParams = videoLayoutParams
            detail_video_view.requestFocus()
        }
    }

    private fun switchTitleBar(show: Boolean) {
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            if (show) {
                supportActionBar.show()
            } else {
                supportActionBar.hide()
            }
        }
    }
}

//---------------------------------------------------------------------------------------------
@InjectViewState
class QuizDetailPresenter : MvpPresenter<QuizDetailView>() {

    private lateinit var quiz: Quiz
    private lateinit var dao: Db.DAO
    private var isStarted = false
    private var subscription: Subscription? = null

    fun init(context: Context) {
        this.dao = Db.DAO(context)
    }

    fun start(quiz: Quiz) {
        isStarted = true
        this.quiz = quiz
    }

    fun onResume() {
        val isCheckedNew = dao.isChecked(quiz.id!!)
        val isCheckedCurr = quiz.isChecked
        if (isCheckedNew != isCheckedCurr) {
            quiz.isChecked = isCheckedNew
            viewState.inverseFavorites(quiz)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
    }

    fun onLinkClick() {
        var url = quiz.registrationLink!!
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        viewState.requestLink(url)
    }

    fun onCalendarClick() {
        viewState.createNotify(quiz)
    }

    fun onGameCheckChanged() {
        val isChecked = !quiz.isChecked
        quiz.isChecked = isChecked

        if (isChecked) {
            dao.setCheckedGame(quiz)
        } else {
            dao.setUncheckedGame(quiz)
        }

        viewState.updateFavoritesView()
    }

    fun onAuthorClick() {
        viewState.showAuthorGames()
    }

    fun isStarted() = isStarted


}
