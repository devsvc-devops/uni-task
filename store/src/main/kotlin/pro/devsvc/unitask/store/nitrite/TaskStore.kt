package pro.devsvc.unitask.store.nitrite

import pro.devsvc.unitask.core.model.Task

interface TaskStore {

    fun store(task: Task)
    fun store(tasks: List<Task>)
    fun load(): Sequence<Task>
    fun load(id: String): Task?
    
}