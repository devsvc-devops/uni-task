package pro.devsvc.unitask.core.model

import java.time.ZonedDateTime

data class Bug (
    var id: Int,
    var title: String,
    var assigned: Person,
    var deadline: ZonedDateTime? = null
) : Model()