package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.databases.Databases

class NotionConnector(private val token: String = System.getProperty("NOTION_TOKEN")) {

    private val client = NotionClient(
        token = token,
        httpClient = OkHttp4Client(),
        logger = Slf4jLogger(),
    )

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