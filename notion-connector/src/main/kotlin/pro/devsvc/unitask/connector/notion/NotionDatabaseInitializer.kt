package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.databases.Database
import notion.api.v1.model.databases.DatabaseProperty

object NotionDatabaseInitializer {

    private val properties = mapOf(
        "Date Created" to DatabaseProperty(
            PropertyType.CreatedTime,
            "created_time",
            createdTime = DatabaseProperty.CreatedTime()
        ),
        "Status" to DatabaseProperty(
            PropertyType.Select,
            "status",
            select = DatabaseProperty.Select(
                listOf(
                    DatabaseProperty.Select.Option()
                )
            )
        ),
        "Due Date" to DatabaseProperty(
            PropertyType.Date,
            "due_date",
            date = DatabaseProperty.Date()
        ),
    )

    fun init(client: NotionClient, database: Database) {
        // client.updatePageProperties()
    }
}