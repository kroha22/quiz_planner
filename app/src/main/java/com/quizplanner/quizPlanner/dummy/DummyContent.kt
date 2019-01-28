package com.quizplanner.quizPlanner.dummy

import com.quizplanner.quizPlanner.model.Quiz
import java.util.ArrayList
import java.util.HashMap

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
object DummyContent {

    val ITEMS: MutableList<Quiz> = ArrayList()
    val ITEM_MAP: MutableMap<String, Quiz> = HashMap()

    private val COUNT = 25

    init {
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }

    private fun addItem(item: Quiz) {
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    private fun createDummyItem(position: Int): Quiz {
        return Quiz(position.toString(),
                "Item $position",
                "Бродячая собака",
                "300",
                "18:00",
                "6",
                "8",
                "https://vk.com/topic-18225558_39710592",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed convallis volutpat metus id tristique. Sed nec enim vitae lacus finibus ultricies. Nullam rhoncus, odio et mattis cursus, leo ante imperdiet tellus, at aliquam odio purus quis nunc. In fermentum, enim ac aliquam interdum, nisl dolor malesuada tellus, porta convallis ipsum dolor ac eros. Sed tincidunt metus diam, ac ultrices lorem condimentum quis. Fusce eget nisl vehicula, sagittis lorem a, tempor metus. Cras semper metus ac nulla suscipit rutrum. Suspendisse et quam justo.",
                "https://pp.userapi.com/c847217/v847217299/246cc/tJ4k-98T3yA.jpg?ava=1")
    }


}
