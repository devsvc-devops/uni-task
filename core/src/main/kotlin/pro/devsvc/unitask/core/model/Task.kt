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
        var createTime: ZonedDateTime? = null
)