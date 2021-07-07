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
        val store2 = store.load("1")
        assertNotNull(store2)
        assertEquals("aaa", store2.title)
    }
}