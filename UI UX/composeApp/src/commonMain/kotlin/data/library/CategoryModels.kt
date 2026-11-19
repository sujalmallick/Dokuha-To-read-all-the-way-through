package data.library

/**
 * Representation of a custom user-defined or default library category.
 */
data class LibraryCategory(
    val id: String,
    val name: String,
    val order: Int,
    val isDefault: Boolean = false
)

/**
 * Predefined default category IDs.
 */
object DefaultCategories {
    const val ALL = "cat-all"
    const val READING = "cat-reading"
    const val PLAN_TO_READ = "cat-plan-to-read"
    const val COMPLETED = "cat-completed"
    const val ON_HOLD = "cat-on-hold"
}
