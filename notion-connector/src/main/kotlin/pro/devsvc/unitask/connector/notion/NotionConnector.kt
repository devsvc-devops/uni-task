package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.databases.Databases
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.connector.Connector
import pro.devsvc.unitask.core.model.Task
import pro.devsvc.unitask.store.nitrite.TaskStore
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField


class NotionConnector(private val token: String = System.getProperty("NOTION_TOKEN")) : Connector {

    private val log = LoggerFactory.getLogger(javaClass)
    private var formatter: DateTimeFormatter

    init {
        val dtfb = DateTimeFormatterBuilder()
        dtfb.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        formatter = dtfb.toFormatter().withZone(ZoneId.systemDefault())
    }

    private val client = NotionClient(
        token = token,
        httpClient = OkHttp4Client(),
        logger = Slf4jLogger(),
    )

    override fun start(store: TaskStore) {
        for (db in listDatabase().results) {
            val pages = client.queryDatabase(db.id).results
            for (page in pages) {
                val title = page.properties["Name"]?.title?.firstOrNull()?.plainText
                if (title != null) {
                    val task = Task(page.id, title)
                    task.customProperties["notion_id"] = page.id
                    task.createTime = parseDateTime(page.createdTime)
                    task.estStarted = parseDateTime(page.properties["Due Date"]?.date?.start)
                    task.deadline = parseDateTime(page.properties["Due Date"]?.date?.end)
                    task.lastEditTime = parseDateTime(page.lastEditedTime)
//                    task.assignedUserId = page.properties["assignedUserId"]?.select?.name.toString()
                    store.store(task)
                }
            }
        }
    }

    private fun parseDateTime(str: String?): ZonedDateTime? {
        if (str == null) {
            return null
        }
        return ZonedDateTime.parse(str, formatter)
    }

    private fun getPropertyAsString() {

    }

    fun listDatabase(): Databases {
        return client.listDatabases()
    }

    fun test() {
        val databases = listDatabase()
        for (db in databases.results) {
            val qdb = client.queryDatabase(db.id).results
        }
    }
}

