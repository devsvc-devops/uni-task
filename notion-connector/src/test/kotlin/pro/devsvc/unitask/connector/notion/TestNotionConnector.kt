package pro.devsvc.unitask.connector.notion

import pro.devsvc.unitask.core.store.NitriteStore
import kotlin.test.Test

class TestNotionConnector {

    private val store = NitriteStore()

    @Test
    fun test() {
        val connector = NotionConnector(database = "Tasks").start()
    }
}