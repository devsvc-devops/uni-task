package pro.devsvc.unitask.core.store

import pro.devsvc.unitask.core.model.Task
import java.time.ZonedDateTime
import kotlin.test.Test

class TestStore {


    @Test
    fun test() {
        val store = NitriteStore()
        val task1 = Task("task1")
        task1.estStarted = ZonedDateTime.now()
        println(task1.estStarted)
        store.store(task1)

        val task11 = store.findTask("task1")
        println(task11?.estStarted)
    }
}