package pro.devsvc.sync

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import org.kodein.di.ktor.di
import pro.devsvc.unitask.connector.notion.NotionConnector
import pro.devsvc.unitask.connector.zentao.ZentaoConnector
import pro.devsvc.unitask.store.nitrite.NitriteStore
import pro.devsvc.unitask.store.nitrite.TaskStore

class SyncRouting {
}

fun Route.start() {
    get("/sync") {
        val store by call.closestDI().instance<TaskStore>()
         ZentaoConnector(
             "http://pms.sinandata.com:8088/biz/",
             System.getProperty("ztUser"),
             System.getProperty("ztPwd")).start(store)
        NotionConnector(database = "Tasks").start(store)
        call.respondText("ok")
    }
}

fun Application.syncRoutes() {
    routing {
        start()
    }
}