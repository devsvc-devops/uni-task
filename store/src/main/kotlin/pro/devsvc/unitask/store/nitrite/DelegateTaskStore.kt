package pro.devsvc.unitask.store.nitrite

import pro.devsvc.unitask.core.model.Task

class DelegateTaskStore(private val delegate: TaskStore, val connectorId: String): TaskStore {
    override fun store(task: Task, oldTask: Task?) {
        val lastSyncTimeKey = "${connectorId}-last-sync-time"
        task.customProperties[lastSyncTimeKey] = System.currentTimeMillis().toString()
        delegate.store(task, oldTask)
    }

    override fun store(tasks: List<Task>) {
        delegate.store(tasks)
    }

    override fun list(): Sequence<Task> {
        return delegate.list()
    }

    override fun find(title: String): Task? {
        return delegate.find(title)
    }

    override fun find(map: Map<String, Any?>): Task? {
        return delegate.find(map)
    }

    override fun delete(task: Task) {
        return delegate.delete(task)
    }
}