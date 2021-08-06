package pro.devsvc.unitask.core.model

import kotlinx.serialization.Serializable
import pro.devsvc.unitask.core.serial.KZonedDateTimeSerializer
import java.time.ZonedDateTime
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.serializers.DateTimePeriodIso8601Serializer
import kotlinx.serialization.Serializer

//typealias UniTask = Task

@Serializable
data class Task(
    var title: String,
    var type: TaskType = TaskType.TASK
) {

    var desc: String = ""
    var status: String = ""
    /** the user id */
    var assignedUserId: String = ""
    var assignedUserName: String = ""
    @Serializable(with = KZonedDateTimeSerializer::class)
    var estStarted: ZonedDateTime? = null
    @Serializable(with = KZonedDateTimeSerializer::class)
    var deadline: ZonedDateTime? = null
    @Serializable(with = DateTimePeriodIso8601Serializer::class)
    var estimate: DateTimePeriod? = null
    @Serializable(with = KZonedDateTimeSerializer::class)
    var createTime: ZonedDateTime? = null
    @Serializable(with = KZonedDateTimeSerializer::class)
    var lastEditTime: ZonedDateTime? = null
    var lastEditBy: String = ""

    var projectName: String? = null
    var planName: String? = null
    var productName: String? = null

    var customProperties = mutableMapOf<String, String?>()
}

enum class TaskType(name: String) {
    TASK("Task"),
    PROJECT("Project"),
    BUG("Bug"),
    PERSON("Person")
}

enum class TaskStatus(val code: String, name: String, val color: String) {
    WAIT("1", "Wait", "red"),
    DOING("2", "Doing", "yellow"),
    DONE("3", "Done", "green"),
    CLOSED("4", "Closed", "gray")
}
