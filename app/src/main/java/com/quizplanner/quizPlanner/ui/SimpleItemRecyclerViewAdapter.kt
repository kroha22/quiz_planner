package com.quizplanner.quizPlanner.ui

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Quiz
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.quiz_list_item.view.*


//------------------------------------------------------------------------------------------------
class SimpleItemRecyclerViewAdapter(private val showDate: Boolean) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {
    //------------------------------------------------------------------------------------------------
    interface ItemClickListener {
        fun onItemClick(quiz: Quiz)

        fun onItemCheckChanged(quiz: Quiz)
    }

    //------------------------------------------------------------------------------------------------

    private var onClickListener: ItemClickListener? = null
    private var values: MutableList<Quiz> = ArrayList()
    private val onItemCheckChanged: MutableMap<Quiz, () -> Unit> = HashMap()

    fun setValues(values: List<Quiz>) {
        this.values = ArrayList(values)
        notifyDataSetChanged()
    }

    fun removeItem(item: Quiz) {
        val pos = values.indexOf(item)
        values.remove(item)
        notifyItemRemoved(pos)
    }

    fun isEmpty(): Boolean {
        return values.isEmpty()
    }

    fun setOnClickListener(onClickListener: ItemClickListener?) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.quiz_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.title.text = item.organisationName
        holder.theme.text = item.gameTheme
        holder.location.text = item.location

        if (showDate) {
            holder.date.text = QuizPlanner.formatterDateMonth.format(item.date)
            holder.dateLine.visibility = View.VISIBLE
        } else {
            holder.dateLine.visibility = View.GONE
        }

        if (item.countOfPlayers != null) {
            holder.count.text = item.countOfPlayers.toString()
            holder.countLine.visibility = View.VISIBLE
        } else {
            holder.countLine.visibility = View.GONE
        }

        holder.time.text = QuizPlanner.formatterTime.format(item.date)

        holder.setChecked(item.isChecked)

        onItemCheckChanged[item] = { holder.setChecked(item.isChecked) }

        if (!item.getLogoUrl().isEmpty()) {
            val apiUrl = holder.title.context.getString(R.string.base_api_img_url)
            Picasso.get()
                    .load(apiUrl + item.getLogoUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(holder.img)
        }

        holder.check.setOnClickListener {
            val curr = item.isChecked
            item.isChecked = !curr
            holder.setChecked(item.isChecked)
            onClickListener?.onItemCheckChanged(item)
        }

        with(holder.itemView) {
            tag = item
            setOnClickListener { onClickListener?.onItemClick(item) }
        }
    }

    override fun getItemCount() = values.size

    //------------------------------------------------------------------------------------------------

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.item_title
        val theme: TextView = view.item_theme
        val location: TextView = view.item_location
        val count: TextView = view.item_count
        val time: TextView = view.item_time
        val img: ImageView = view.item_img
        val check: ImageView = view.item_check
        val date: TextView = view.item_date
        val dateLine: LinearLayout = view.item_date_line
        val countLine: LinearLayout = view.item_count_line

        fun setChecked(isChecked: Boolean) {
            if (isChecked) {
                check.setColorFilter(ContextCompat.getColor(check.context, R.color.colorAccent))
            } else {
                check.colorFilter = null
            }
        }
    }
}