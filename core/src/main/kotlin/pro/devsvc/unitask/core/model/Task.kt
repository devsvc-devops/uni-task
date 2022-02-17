package pro.devsvc.unitask.core.model

import java.time.ZonedDateTime

// typealias UniTask = Task

data class Task(
    var title: String,
    var type: TaskType = TaskType.TASK
) : Model() {

    var desc: String = ""
    var status: TaskStatus = TaskStatus.WAIT
    /** the user id */
    var assignedUserId: String = ""
    var assignedUserName: String? = null
    var estStarted: ZonedDateTime? = null
    var deadline: ZonedDateTime? = null
    // var estimate: DateTimePeriod? = null
    var createTime: ZonedDateTime? = null

    var lastEditBy: String = ""

    var priority: TaskPriority = TaskPriority.NORMAL

    var projectName: String? = null
    var planName: String? = null
    var productName: String? = null

    var from: String? = null

    var uProductId: Long? = null
    var uProjectId: Long? = null
    var uPlanId: Long? = null
    var uPersonId: Long? = null
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

enum class TaskPriority(val cname: String) {
    URGENT("紧急"),
    IMPORTANT("重要"),
    NORMAL("一般"),
    UNIMPORTANT("不重要");

    companion object {
        fun getByCName(cname: String): TaskPriority {
            return values().first { it.cname == cname }
        }
        fun getById(id: Int): TaskPriority {
            return values()[id]
        }
    }
}