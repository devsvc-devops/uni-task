package pro.devsvc.unitask.core.model

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class Model {
    var lastEditTime: ZonedDateTime = ZonedDateTime.now()

    var customProperties = mutableMapOf<String, String?>()

    fun getIdInConnector(connectorId: String): String? {
        return customProperties["id-$connectorId"]
    }

    fun setIdInConnector(connectorId: String, id: String) {
        customProperties["id-$connectorId"] = id
    }

    fun setLastEditOfConnector(connectorId: String, time: ZonedDateTime) {
        customProperties["lst-from-$connectorId"] = time.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    fun getLastEditOfConnector(connectorId: String): ZonedDateTime? {
        val timeStr = customProperties["lst-from-$connectorId"]
        return if (timeStr == null) null else ZonedDateTime.parse(timeStr)
    }
}