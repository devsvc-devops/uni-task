package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.http.HttpUrlConnNotionHttpClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.databases.DatabaseProperty
import notion.api.v1.model.databases.Databases
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageProperty
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.connector.Connector
import pro.devsvc.unitask.core.model.Task
import pro.devsvc.unitask.core.model.TaskType
import pro.devsvc.unitask.store.nitrite.TaskStore
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField


class NotionConnector(token: String = System.getProperty("NOTION_TOKEN"),
    private val database: String) : Connector {

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
        httpClient = HttpUrlConnNotionHttpClient(connectTimeoutMillis = 10_000),
        logger = Slf4jLogger(),
    )

    private val statusMap = mutableMapOf<String, DatabaseProperty.Select.Option>()

    override fun start(store: TaskStore) {
        for (db in listDatabase().results) {
            if (db.title[0].plainText != database) {
                continue
            }
            db.properties["Status"]?.select?.options?.forEach { statusMap[it.name!!.toLowerCase()] = it }
            val pages = client.queryDatabase(db.id).results
            for (page in pages) {
                syncToStore(page, store)
            }
        }
        syncToNotion(store)
    }

    private fun syncToStore(page: Page, store: TaskStore) {
        val title = page.properties["Name"]?.title?.firstOrNull()?.plainText
        if (title != null) {
            val task = Task(page.id, title)
            task.customProperties["notion_id"] = page.id
            task.customProperties["status"] = page.properties["Status"]?.select?.id
            task.createTime = parseDateTime(page.createdTime)
            task.estStarted = parseDateTime(page.properties["Due Date"]?.date?.start)
            task.deadline = parseDateTime(page.properties["Due Date"]?.date?.end)
            task.lastEditTime = parseDateTime(page.lastEditedTime)
            store.store(task)
        }
    }

    private fun syncToNotion(store: TaskStore) {
        for (task in store.load()) {
            if (task.title != null) {
                val notionId = task.customProperties["notion_id"]

                if (notionId != null) {
                    client.updatePageProperties(notionId, mapOf(
                        "Status" to PageProperty(select = statusMap[task.status.toLowerCase()]),
                        "Date" to PageProperty(date = PageProperty.Date(
                            start = task.estStarted?.format(DateTimeFormatter.ISO_DATE_TIME),
                            end = task.deadline?.format(DateTimeFormatter.ISO_DATE_TIME),
                        )),
                        "AssignedTo" to PageProperty(richText = listOf(PageProperty.RichText(plainText = task.assignedUserName)))
                    ))
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

