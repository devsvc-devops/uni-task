package pro.devsvc.unitask.store.nitrite

import pro.devsvc.unitask.core.model.Task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestTaskStore {

    @Test
    fun test() {
        val store = NitriteStore()
        val task = Task("1", "aaa")
        task.desc = "111"
        store.store(task)
//        store.db.commit()
        val task2 = store.load("1")
        assertNotNull(task2)
        assertEquals("aaa", task2.title)
        assertEquals("111", task2.desc)
    }
}