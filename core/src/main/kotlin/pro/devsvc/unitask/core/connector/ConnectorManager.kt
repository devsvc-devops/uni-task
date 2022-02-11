package pro.devsvc.unitask.core.connector

import org.slf4j.LoggerFactory
import pro.devsvc.unitask.core.model.Task
import pro.devsvc.unitask.core.store.TaskStore
import pro.devsvc.unitask.core.store.TaskStoreManager
import java.time.ZonedDateTime

object ConnectorManager {
    private val log = LoggerFactory.getLogger(javaClass)

    private val connectors = mutableMapOf<String, Connector>()

    private val store = TaskStoreManager.store

    // for now, client use this method to register a connector
    // maybe we need a more flex mechanism to manage the reg of connectors
    fun registerConnector(id: String, connector: Connector) {
        connectors[id] = connector
    }

    fun start() {
        for ((id, connector) in connectors) {
            log.info("start sync with connector: {}...", id)
            connector.start()
        }
    }

    fun sync() {
        log.info("start sync...")
        log.debug("connectors: " + connectors.keys.joinToString { it })
        for ((id, connector) in connectors) {
            log.info("start sync with connector: {}...", id)
            syncConnector(connector)
        }
    }

    private fun syncConnector(connector: Connector) {
        for (t in store.list()) {
            syncToConnector(t, connector)
        }

        for (t in connector.listTasks()) {
            syncToStore(t, connector)
        }
    }

    private fun syncToStore(task: Task, connector: Connector) {
        val existing = store.find(task.title)
        if (existing != null) {
            val cid = existing.getIdInConnector(connector.id)
            if (cid != null) {
                val newCid = task.getIdInConnector(connector.id)
                if (cid != newCid) {
                    log.error("task title conflict...")
                    return
                }
            }
            val existingLst = existing.lastEditTime
            val existingLstFrom = existing.getLastSyncFromConnector(connector.id)
            // 在上次同步之后，又被修改过，应该是来自其他应用的修改，暂不同步以避免冲突
            if (existingLst!!.isAfter(existingLstFrom)) {
                return
            }
        }
        task.setLastSyncFromConnector(connector.id, ZonedDateTime.now())
        store.store(task)
    }

    private fun syncToConnector(task: Task, connector: Connector) {
        connector.update(task)
    }
}