package pro.devsvc.sync

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import pro.devsvc.unitask.connector.notion.NotionConnector
import pro.devsvc.unitask.connector.zentao.ZentaoConnector
import pro.devsvc.unitask.store.nitrite.NitriteStore

class SyncRouting {
}

fun Route.start() {
    get("/sync") {
        val store = NitriteStore()
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