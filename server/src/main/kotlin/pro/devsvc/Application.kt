package pro.devsvc

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.kodein.di.bind
import org.kodein.di.ktor.di
import org.kodein.di.singleton
import pro.devsvc.sync.syncRoutes
import pro.devsvc.unitask.store.nitrite.NitriteStore
import pro.devsvc.unitask.store.nitrite.TaskStore

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    di {
        bind<TaskStore> { singleton { NitriteStore() } }
    }
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        syncRoutes()
    }
}