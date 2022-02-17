package pro.devsvc.unitask.core.model


import java.time.ZonedDateTime

class Project(
    var title: String,
    var estStarted: ZonedDateTime? = null,
    var deadline: ZonedDateTime? = null
): Model()