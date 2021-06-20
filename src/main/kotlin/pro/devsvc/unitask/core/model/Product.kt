package pro.devsvc.unitask.core.model

class Product(
        var id: Int,
        var name: String,
        var owner: String,
) {

    val projects: MutableList<Project> = mutableListOf()

}