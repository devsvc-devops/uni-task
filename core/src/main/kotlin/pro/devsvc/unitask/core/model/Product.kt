package pro.devsvc.unitask.core.model

class Product(
        var id: Int,
        var name: String,
        var owner: String,
        val projectIds: List<Int>,
        val modulesIds: List<Int>
) {

    // val projects: MutableList<Project> = mutableListOf()

}