package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.blocks.BlockType
import notion.api.v1.model.blocks.ChildDatabaseBlock
import notion.api.v1.model.databases.*
import notion.api.v1.model.databases.query.filter.condition.SelectFilter
import notion.api.v1.model.databases.query.filter.condition.TextFilter
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import notion.api.v1.model.pages.PageProperty
import notion.api.v1.model.pages.PageProperty.RichText
import notion.api.v1.model.pages.PageProperty.RichText.Text
import notion.api.v1.request.databases.CreateDatabaseRequest
import notion.api.v1.request.pages.CreatePageRequest
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.connector.notion.utils.titlePageProperty
import pro.devsvc.unitask.core.connector.Connector
import pro.devsvc.unitask.core.model.Task
import pro.devsvc.unitask.core.model.TaskStatus
import pro.devsvc.unitask.core.model.TaskType
import pro.devsvc.unitask.core.store.TaskStoreManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

const val NOTION_CONNECTOR_ID = "notion"
const val DEFAULT_PAGE_TITLE = "UniTask"
const val TASK_DATABASE_TITLE = "Tasks"
const val PROJECT_DATABSE_TITLE = "Projects"
const val PRODUCT_DATABSE_TITLE = "Products"

class NotionConnector(
    token: String = System.getProperty("NOTION_TOKEN"),
    private val database: String = "UniTask") : Connector {

    private val log = LoggerFactory.getLogger(javaClass)
    private var formatter: DateTimeFormatter
    private val NOTION_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

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
        httpClient = OkHttp4Client(connectTimeoutMillis = 10_000),
        logger = Slf4jLogger(),
    )

    lateinit var pageId: String
    private var databaseId = ""
    private var taskDatabaseId = ""

    private val schema = mutableMapOf<String, DatabaseProperty>()
    private val store = TaskStoreManager.store
    override val id: String
        get() = NOTION_CONNECTOR_ID


    override fun start() {
        val pages = client.search("UniTask").results
        if (pages.isNotEmpty()) {
            pageId = pages[0].id
        } else return

        createDatabase()
    }

    private fun createDatabase() {
        val children = client.retrieveBlockChildren(pageId)
        for (child in children.results) {
            if (child.type == BlockType.ChildDatabase && child.asChildDatabase().childDatabase.title == "UniTask") {
                val databaseBlock = child as ChildDatabaseBlock
                databaseBlock.childDatabase.title ==
            }
        }

        client.createDatabase(
            CreateDatabaseRequest(
                parent = DatabaseParent.page(pageId),
                title = listOf(DatabaseProperty.RichText(text = DatabaseProperty.RichText.Text(content = "UniTask"))),
                properties = mapOf(
                    "Title" to TitlePropertySchema(),
                    // "AssignedTo" to SelectPropertySchema(),
                    "Date" to DatePropertySchema(),
                    //"Status" to SelectPropertySchema(),
                    //"Type" to SelectPropertySchema(),
                )
            )
        )

    }

    private fun createTaskDatabase() {

    }

    private fun createProjectDatabase() {

    }

    private fun createPersonDatabase() {

    }

    private fun createProductDatabase() {

    }

    override fun listTasks(): List<Task> {
        val result = mutableListOf<Task>()
        for (db in listDatabase().results) {
            if (db.title[0].plainText != database) {
                continue
            }
            databaseId = db.id
            schema.putAll(db.properties)

            val pages = client.queryDatabase(db.id).results
            for (page in pages) {
                val title = page.properties["Name"]?.title?.firstOrNull()?.plainText
                if (title != null) {
                    val task = Task(title)
                    pageToUniTask(page, task)
                    result.add(task)
                }
            }
        }
        return result
    }

    override fun update(task: Task) {
        log.info("sync task $task to notion...")
        val notionId = task.getIdInConnector(NOTION_CONNECTOR_ID)
        val properties = taskToNotionPageProperties(task)
        if (properties.isEmpty()) {
            return
        }

        if (notionId != null) {
            if (properties.isNotEmpty()) {
                // val page = client.retrievePage(notionId)
                // if (page.archived == true) {
                //     log.warn("page $page is deleted by user...")
                // }
                // val notionLastEditTime = parseDateTime(page.lastEditedTime)
                val lastEditTime = task.lastEditTime
                // val lastSyncTime = parseDateTime(task.customProperties["last_sync_to_notion"])
                // if no edit since last sync, don't sync again
                // if (lastEditTime?.isBefore(lastSyncTime) == true) {
                //     continue
                // }
                if (true) {
                    try {
                        val result = client.updatePageProperties(notionId, properties)
                        log.debug("update result $result")
                        task.customProperties["notion_last_sync_time"] = ZonedDateTime.now().format(formatter)
                        store.store(task)
                    } catch (e: NotionAPIError) {
                        if (e.message.startsWith("Could not find page with ID")) {
                            log.warn("deleting task $task")
                            store.delete(task)
                        }
                    } catch (e: Throwable) {
                        log.error("error update task: $task", e)
                    }
                } else {
                    log.debug("task ${task.title} last edit time is before or equals to notion's last edit time, skip...")
                }
            }
        } else {
            val page = client.createPage(CreatePageRequest(
                PageParent.database(databaseId),
                properties
            ))
            task.customProperties["notion_last_sync_time"] = ZonedDateTime.now().format(formatter)
            task.customProperties["notion_id"] = page.id
            store.store(task)
        }
    }

    private fun pageToUniTask(page: Page, task: Task) {
        val typeName = page.properties["Type"]?.select?.name
        if (typeName != null && typeName.isNotBlank()) {
            task.type = TaskType.valueOf(typeName.toUpperCase())
        }

        task.customProperties["notion_id"] = page.id
        val pageStatus = page.properties["Status"]?.select?.name
        if (pageStatus != null) {
            task.status = TaskStatus.getByName(pageStatus)
        }
        task.createTime = parseDateTime(page.createdTime)
        task.estStarted = parseDateTime(page.properties["Due Date"]?.date?.start)
        task.deadline = parseDateTime(page.properties["Due Date"]?.date?.end)
        task.assignedUserName = page.properties["AssignedTo"]?.select?.name
        task.lastEditTime = parseDateTime(page.lastEditedTime)
        // task.customProperties["last_sync_to_notion"] = ZonedDateTime.now().format(formatter)
        task.projectName = page.properties["ProjectName"]?.select?.name
        task.productName = page.properties["ProductName"]?.select?.name
        task.from = "Notion"
    }

    private fun taskToNotionPageProperties(task: Task): MutableMap<String, PageProperty> {
        val properties = mutableMapOf<String, PageProperty>()
        if (task.title.isNotBlank()) {
            properties["Name"] = PageProperty(title = listOf(
                RichText(text = Text(task.title))
            ))
        }

        if (task.estStarted != null || task.deadline != null) {
            if (task.estStarted == null) {
                task.estStarted = task.deadline
            }
            properties["Date"] = PageProperty(date = PageProperty.Date(
                start = task.estStarted?.format(NOTION_FMT),
                end = task.deadline?.format(NOTION_FMT),
            ))
        }
        safeCreatePageProperty(properties,"Status", task.status.name)
        safeCreatePageProperty(properties,"AssignedTo", task.assignedUserName)
        safeCreatePageProperty(properties,"ProjectName", task.projectName)
        safeCreatePageProperty(properties,"ProductName", task.productName)
        safeCreatePageProperty(properties,"Type", task.type.name)
        safeCreatePageProperty(properties, "Priority", task.priority.cname)

        if (task.type == TaskType.TASK && task.projectName != null) {
            // if project is already in notion, construct the relation
            // if not, have to do this in next sync. TODO: better solution?
            val projectInNotion = client.queryDatabase(databaseId, NCompoundFilter(
                and = listOf(
                    NPropertyFilter("Name", title = TextFilter(task.projectName)),
                    NPropertyFilter("Type", select = SelectFilter("Project"))
                )
            ))
            if (projectInNotion.results.isNotEmpty()) {
                val projectInNotionPage = projectInNotion.results[0]
                properties["Project"] = PageProperty(relation = listOf(
                    PageProperty.PageReference(projectInNotionPage.id)
                ))
            }
        }
        return properties
    }

    private fun safeCreatePageProperty(properties: MutableMap<String, PageProperty>, name: String, value: String?) {
        if (!value.isNullOrBlank()) {
            val propertyDef = schema[name]
            var pageProperty: PageProperty? = null
            if (propertyDef != null) {
                if (propertyDef.select != null) {
                    val option = findOptionInSchema(name, value)
                    if (option != null) {
                        pageProperty = PageProperty(select = option)
                    } else {
                        pageProperty = PageProperty(select = DatabaseProperty.Select.Option(name = value))
                    }
                } else if (propertyDef.richText != null) {
                    pageProperty = PageProperty(richText = listOf(RichText(text = Text(value))))
                }
            }
            if (pageProperty != null) {
                properties[name] = pageProperty
            }
        }
    }

    private fun findOptionInSchema(propertyName: String, optionValue: String): DatabaseProperty.Select.Option? {
        val property = schema[propertyName]
        if (property != null) {
            return property.select?.options?.find { it.name.equals(optionValue, ignoreCase = true) }
        }
        return null
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
}

