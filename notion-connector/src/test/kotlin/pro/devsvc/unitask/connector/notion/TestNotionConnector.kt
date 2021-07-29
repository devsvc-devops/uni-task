package pro.devsvc.unitask.connector.notion

import pro.devsvc.unitask.store.nitrite.NitriteStore
import kotlin.test.Test

class TestNotionConnector {

    private val store = NitriteStore()

    @Test
    fun test() {
        val connector = NotionConnector(database = "Tasks").start(store)
    }
}