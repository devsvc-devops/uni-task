package pro.devsvc.unitask.connector

import pro.devsvc.unitask.core.model.Task
import pro.devsvc.unitask.store.nitrite.TaskStore

interface Connector {
    fun start(store: TaskStore)
}