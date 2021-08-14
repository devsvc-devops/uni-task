package pro.devsvc.unitask.store.nitrite

import pro.devsvc.unitask.core.model.Task
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestTaskStore {

    @Test
    fun test() {
        val store = NitriteStore()
        val task = Task("aaa")
        task.desc = "111"
        val now = ZonedDateTime.now()
        task.estStarted = now
        store.store(task)
//        store.db.commit()
        val task2 = store.find("1")
        assertNotNull(task2)
        assertEquals("aaa", task2.title)
        assertEquals("111", task2.desc)
        assertEquals(now, task2.estStarted)
    }
}