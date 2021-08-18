package pro.devsvc.unitask.core.model

import kotlinx.serialization.Serializable
import pro.devsvc.unitask.core.serial.KZonedDateTimeSerializer
import java.time.ZonedDateTime
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.serializers.DateTimePeriodIso8601Serializer

// typealias UniTask = Task

@Serializable
data class Task(
    var title: String,
    var type: TaskType = TaskType.TASK
) {

    var desc: String = ""
    var status: TaskStatus = TaskStatus.WAIT
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

    var priority: TaskPriority = TaskPriority.NORMAL

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
    CLOSED("4", "Closed", "gray");

    companion object {
        fun getByName(name: String): TaskStatus {
            return values().first { it.name.equals(name, ignoreCase = true) }
        }
        fun getByCode(code: String): TaskStatus {
            return values().first { it.code == code }
        }
    }
}

enum class TaskPriority(name: String) {
    URGENT("紧急"),
    IMPORTANT("重要"),
    NORMAL("一般"),
    UNIMPORTANT("不重要");

    companion object {
        fun getByName(name: String): TaskPriority {
            return values().first { it.name == name}
        }
        fun getById(id: Int): TaskPriority {
            return values()[id]
        }
    }
}