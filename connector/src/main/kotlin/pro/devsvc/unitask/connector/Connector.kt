package pro.devsvc.unitask.connector

import pro.devsvc.unitask.store.nitrite.TaskStore

interface Connector {
    fun sync(store: TaskStore)
}