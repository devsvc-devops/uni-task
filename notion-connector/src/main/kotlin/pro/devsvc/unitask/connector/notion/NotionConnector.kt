package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.databases.Databases
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.connector.Connector
import pro.devsvc.unitask.core.model.Task
import pro.devsvc.unitask.store.nitrite.TaskStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotionConnector(private val token: String = System.getProperty("NOTION_TOKEN")) : Connector {

    private val log = LoggerFactory.getLogger(javaClass)
    private val formatter = DateTimeFormatter.ISO_INSTANT

    private val client = NotionClient(
        token = token,
        httpClient = OkHttp4Client(),
        logger = Slf4jLogger(),
    )

    override fun start(store: TaskStore) {
        for (db in listDatabase().results) {
            val pages = client.queryDatabase(db.id).results
            for (page in pages) {
                val title = page.properties["Name"]?.title?.get(0)?.plainText
                if (title != null) {
                    val task = Task(page.id, title)
                    task.customProperties["notion_id"] = page.id
                    task.createTime = Instant.parse(page.createdTime).atZone(ZoneId.systemDefault())
                    store.store(task)
                }
            }
        }
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

