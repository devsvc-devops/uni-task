package pro.devsvc.unitask.core.model

import java.time.Period
import java.time.ZonedDateTime

data class Task(
        var id: Long,
        var title: String,
        var desc: String = "",
        /** the user id */
        var assignedTo: Long = -1,
        var estStarted: ZonedDateTime? = null,
        var estimate: Period? = null,
        var createTime: ZonedDateTime? = null,
        var lastEditTime: ZonedDateTime? = null,
        var lastEditBy: Long = -1,
        var project: Long = -1,
        var planId: Long = -1,
        var productId: Long = -1,
) {


}