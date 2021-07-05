package pro.devsvc.unitask.core.model

import java.time.Period
import java.time.ZonedDateTime

data class Task(
    /** id from notion is uuid, so have to be string */
    var id: String,
    var title: String,
    ) {

    var desc: String = ""

    /** the user id */
    var assignedTo: String = ""
    var estStarted: ZonedDateTime? = null
    var estimate: Period? = null
    var createTime: ZonedDateTime? = null
    var lastEditTime: ZonedDateTime? = null
    var lastEditBy: String = ""
    var project: String? = null
    var planId: String? = null
    var productId: String? = null
}