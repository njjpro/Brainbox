package com.njjpro.brainbox.data

/**
 * 五类记录。id 持久化在数据库里，不要随意改动。
 */
enum class Category(
    val id: String,
    val label: String,
    val emoji: String,
    val hint: String,
) {
    REMINDER("reminder", "提醒", "⏰", "几点去做什么"),
    VIBE("vibe", "Vibe Coding", "🧪", "写代码时冒出的想法"),
    THOUGHT("thought", "思考感想", "💭", "读书、生活、自我复盘"),
    RESEARCH("research", "科研 Idea", "🔬", "课题、实验、论文"),
    EXPENSE("expense", "每月报销", "🧾", "按月记账，标记是否已报"),
    ;

    companion object {
        fun of(id: String?): Category = entries.firstOrNull { it.id == id } ?: THOUGHT
    }
}
