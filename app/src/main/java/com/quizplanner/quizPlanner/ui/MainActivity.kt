package com.quizplanner.quizPlanner.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner.formatterDate
import com.quizplanner.quizPlanner.QuizPlanner.formatterDay
import com.quizplanner.quizPlanner.QuizPlanner.formatterMonth
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Quiz
import com.quizplanner.quizPlanner.ui.QuizDetailActivity.Companion.QUIZ_ITEM_CODE
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.quiz_list_item.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


/**
 * Created by Olga Cherepanova
 * on 20.01.2019.
 */

//---------------------------------------------------------------------------------------------
@StateStrategyType(SkipStrategy::class)
interface MainView : MvpView {

    companion object {
        const val PROGRESS_TAG = "ERROR_TAG"
    }

    fun showMessage(msg: String)

    fun showDialog(dialogBuilder: DialogBuilder)

    fun setContent(mTabItems: LinkedHashMap<Date, List<Quiz>>)

    fun showQuizView(quiz: Quiz)

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun showLoadProgress()

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun hideLoadProgress()

}

//---------------------------------------------------------------------------------------------
private const val MAIN: String = "MainActivity"
//-------------------------------------------------------------------------------------------

class MainActivity : MvpAppCompatActivity(), MainView, DateFragment.ItemClickListener {

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    @InjectPresenter(tag = MAIN)
    lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.adapter = sectionsPagerAdapter
        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))

        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        tabs.setupWithViewPager(container)
        tabs.setSelectedTabIndicatorColor(Color.TRANSPARENT)

        fab.setOnClickListener { presenter.onRefreshClick() }
    }

    override fun onResume() {
        super.onResume()

        if (!presenter.isInitialized()) {
            presenter.init(this)
            presenter.setEscapeHandler { finish() }
        }
        presenter.start()
    }

    override fun showQuizView(quiz: Quiz) {
        val intent = Intent(this, QuizDetailActivity::class.java).apply {
            putExtra(QUIZ_ITEM_CODE, quiz)
        }
        startActivity(intent)
    }

    override fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun showDialog(dialogBuilder: DialogBuilder) {
        dialogBuilder.show(this)
    }

    override fun showLoadProgress() {
        wait_view.visibility = View.VISIBLE
    }

    override fun hideLoadProgress() {
        wait_view.visibility = View.GONE
    }

    override fun setContent(mTabItems: LinkedHashMap<Date, List<Quiz>>) {
        sectionsPagerAdapter.setItems(mTabItems)

        for (i in 0..tabs.tabCount) {
            val tab = tabs.getTabAt(i)
            if (tab != null) {
                tab.customView = sectionsPagerAdapter.getTabView(i)
                tab.select()

                if (DateUtils.isToday(sectionsPagerAdapter.getItemDate(i).time)) {
                    container.setCurrentItem(i, true)
                }
            }
        }
        if (tabs.tabCount == 1) {
            val tab = tabs.getTabAt(0)
            val view = tab?.customView
            if (view != null) {
                view.isSelected = true
            }
        }

    }

    override fun onItemClick(quiz: Quiz) {
        presenter.onQuizSelected(quiz)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
    //------------------------------------------------------------------------------------------------

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val tabItems: MutableList<Date> = ArrayList()
        private val pages: MutableMap<Date, List<Quiz>> = LinkedHashMap()

        fun setItems(items: Map<Date, List<Quiz>>) {
            tabItems.clear()
            pages.clear()

            pages.putAll(items)
            tabItems.addAll(items.keys)

            notifyDataSetChanged()
        }

        fun getItemDate(position: Int): Date {
            return tabItems[position]
        }

        override fun getItem(position: Int): Fragment {
            val fragment = DateFragment()
            val date = tabItems[position]
            fragment.setValues(pages[date]!!)
            return fragment
        }

        override fun getCount(): Int {
            return tabItems.size
        }

        @SuppressLint("InflateParams")
        fun getTabView(position: Int): View {
            val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_custom_tab, null)

            val title = view.findViewById<TextView>(R.id.tab_title)
            val subtitle = view.findViewById<TextView>(R.id.tab_sub_title)

            val item = tabItems[position]
            title.text = formatterDay.format(item)
            subtitle.text = formatterDate.format(item)

            toolbar_month.text = formatterMonth.format(item)

            return view
        }
    }

}
//------------------------------------------------------------------------------------------------

class DateFragment : Fragment() {
    //------------------------------------------------------------------------------------------------
    interface ItemClickListener {
        fun onItemClick(quiz: Quiz)
    }
    //------------------------------------------------------------------------------------------------

    private val adapter = SimpleItemRecyclerViewAdapter()
    private var clickListener: ItemClickListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val recyclerView: RecyclerView = inflater.inflate(R.layout.quiz_list, container, false) as RecyclerView
        adapter.setOnClickListener { clickListener?.onItemClick(it) }
        recyclerView.adapter = adapter
        return recyclerView
    }

    fun setValues(values: List<Quiz>) {
        adapter.setValues(values)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            clickListener = context as ItemClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement OnListItemSelectedListener")
        }

    }

    //------------------------------------------------------------------------------------------------
    class SimpleItemRecyclerViewAdapter :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private var onClickListener: (Quiz) -> Unit = {}
        private var values: MutableList<Quiz> = ArrayList()

        fun setValues(values: List<Quiz>) {
            this.values.clear()
            this.values.addAll(0, values)
            notifyDataSetChanged()
        }

        fun setOnClickListener(onClickListener: (Quiz) -> Unit) {
            this.onClickListener = onClickListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.quiz_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.name.text = item.organization
            holder.place.text = item.location
            holder.price.text = item.price
            holder.count.text = item.countOfPlayers
            holder.time.text = item.time

            if (!item.imgUrl.isEmpty()) {
                Picasso.get()
                        .load(item.imgUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(holder.img)
            }


            with(holder.itemView) {
                tag = item
                setOnClickListener { onClickListener.invoke(item) }
            }
        }

        override fun getItemCount() = values.size

        //------------------------------------------------------------------------------------------------

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.item_name
            val place: TextView = view.item_place
            val price: TextView = view.item_price
            val count: TextView = view.item_count
            val time: TextView = view.item_time
            val img: ImageView = view.item_img
        }
    }
    //------------------------------------------------------------------------------------------------
}
