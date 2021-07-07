package pro.devsvc.unitask.core.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import pro.devsvc.unitask.core.serial.KZonedDateTimeSerializer
import java.time.Period
import java.time.ZonedDateTime
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.serializers.DateTimePeriodIso8601Serializer

@Serializable
data class Task(
    /** id from notion is uuid, so have to be string */
    var id: String,
    var title: String,
    ) {

    var desc: String = ""

    /** the user id */
    var assignedTo: String = ""
    @Serializable(with = KZonedDateTimeSerializer::class)
    var estStarted: ZonedDateTime? = null
    @Serializable(with = DateTimePeriodIso8601Serializer::class)
    var estimate: DateTimePeriod? = null
    @Serializable(with = KZonedDateTimeSerializer::class)
    var createTime: ZonedDateTime? = null
    @Serializable(with = KZonedDateTimeSerializer::class)
    var lastEditTime: ZonedDateTime? = null
    var lastEditBy: String = ""
    var project: String? = null
    var planId: String? = null
    var productId: String? = null
}