package pro.devsvc.unitask.core.model

data class Plan(
    var id: Int,
    var name: String,
    var createdBy: String,
    var start: String,
    val projectIds: List<Int>,
    val modulesIds: List<Int>
)