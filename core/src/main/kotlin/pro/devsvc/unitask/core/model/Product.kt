package pro.devsvc.unitask.core.model

data class Product(
    var id: Int,
    var name: String,
    var owner: String,
    var createdBy: String,
    val projectIds: List<Int>,
    val modulesIds: List<Int>
) {

    // val projects: MutableList<Project> = mutableListOf()
}