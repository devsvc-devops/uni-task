package pro.devsvc

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import pro.devsvc.sync.syncRoutes

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        syncRoutes()
    }
}