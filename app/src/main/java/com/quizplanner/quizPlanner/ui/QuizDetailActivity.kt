package com.quizplanner.quizPlanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.quizplanner.quizPlanner.QuizPlanner
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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val item = intent.getSerializableExtra(QUIZ_ITEM_CODE)
                    ?: throw AssertionError()

            this.item = item as Quiz
            detail_title.text = item.organization
            detail_theme.text = item.gameTheme
            detail_date.text = QuizPlanner.formatterDateMonth.format(item.date)
            detail_time.text = item.time
            detail_location.text = item.location
            detail_difficulty.text = item.difficulty
            detail_count.text = item.countOfPlayers
            detail_price.text = item.price
            detail_description.text = item.description
            detail_link.text = item.registrationLink
            if (item.registrationLink.isEmpty()) {
                detail_link.visibility = View.INVISIBLE
            } else {
                detail_link.visibility = View.VISIBLE
                detail_link.setOnClickListener { onLinkClick(item) }
            }

            if(!item.imgUrl.isEmpty()){
                Picasso.get()
                        .load(item.imgUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_broken_image)
                        .into(detail_img)
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
        var url = quiz.registrationLink
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        requestLink(url)
    }

    private fun requestLink(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }
}
