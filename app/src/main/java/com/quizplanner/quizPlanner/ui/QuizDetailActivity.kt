package com.quizplanner.quizPlanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterTime
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Quiz
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_quiz_detail.*
import kotlinx.android.synthetic.main.quiz_detail.*


class QuizDetailActivity : AppCompatActivity() {

    companion object {
        const val QUIZ_ITEM_CODE = "quiz_item"
    }

    private lateinit var item: Quiz

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
            detail_difficulty.text = item.difficulty
            detail_count.text = item.countOfPlayers.toString()
            detail_price.text = item.price.toString()
            detail_description.text = item.description

            if (item.registrationLink!!.isEmpty()) {
                detail_link_label.visibility = View.INVISIBLE
                detail_link.visibility = View.INVISIBLE
            } else {
                detail_link_label.visibility = View.VISIBLE
                detail_link.visibility = View.VISIBLE
                detail_link.text = item.registrationLink
                detail_link.setOnClickListener { onLinkClick(item) }
            }

            if (!item.getImgUrl().isEmpty()) {
                val apiUrl = getString(R.string.base_api_img_url)

                Picasso.get()
                        .load(apiUrl + item.getImgUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_broken_image)
                        .into(detail_img)
            }

            toolbar_calendar.setOnClickListener {
                createNotify(item)
            }
        }

        detail_title.requestFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    NavUtils.navigateUpTo(this, Intent(this, MainActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }


    private fun onLinkClick(quiz: Quiz) {
        var url = quiz.registrationLink!!
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        requestLink(url)
    }

    private fun requestLink(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    private fun createNotify(quiz: Quiz) {
        val intent = Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, quiz.date)
                .putExtra(Events.TITLE, quiz.gameTheme)
                .putExtra(Events.DESCRIPTION, quiz.description)
                .putExtra(Events.EVENT_LOCATION, quiz.location)
                .putExtra(Intent.EXTRA_EMAIL, quiz.registrationLink)
        startActivity(intent)
    }
}
