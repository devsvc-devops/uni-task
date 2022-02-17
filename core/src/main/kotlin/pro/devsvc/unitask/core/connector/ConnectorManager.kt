package pro.devsvc.unitask.core.connector

import org.slf4j.LoggerFactory
import pro.devsvc.unitask.core.model.Model
import pro.devsvc.unitask.core.model.Product
import pro.devsvc.unitask.core.model.Project
import pro.devsvc.unitask.core.model.Task
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
        for (t in store.listModels()) {
            syncToConnector(t, connector)
        }
        for (t in connector.listProducts()) {
            syncToStore(t, connector)
        }
        for (t in connector.listProjects()) {
            syncToStore(t, connector)
        }

        for (t in store.list()) {
            syncToConnector(t, connector)
        }

        for (t in connector.listTasks()) {
            syncToStore(t, connector)
        }
    }

    private fun syncToStore(task: Task, connector: Connector) {
        val existing = store.findTask(task.title)
        if (shouldStore(task, existing, connector)) {
            task.setLastEditOfConnector(connector.id, task.lastEditTime)
            store.store(task)
        }
    }

    private fun syncToStore(product: Product, connector: Connector) {
        val existing = store.findProduct(product.name)
        if (shouldStore(product, existing, connector)) {
            product.setLastEditOfConnector(connector.id, product.lastEditTime)
            store.store(product)
        }
    }

    private fun syncToStore(project: Project, connector: Connector) {
        val existing = store.findProduct(project.title)
        if (shouldStore(project, existing, connector)) {
            project.setLastEditOfConnector(connector.id, project.lastEditTime)
            store.store(project)
        }
    }

    private fun storeModel(model: Model, existing: Model?, connector: Connector) {
        if (shouldStore(model, existing, connector)) {
            model.setLastEditOfConnector(connector.id, model.lastEditTime)
            store.store(model)
        }
    }

    private fun shouldStore(model: Model, existing: Model?, connector: Connector): Boolean {
        if (existing == null) {
            return true
        } else {
            val cid = existing.getIdInConnector(connector.id)
            if (cid != null) {
                val newCid = model.getIdInConnector(connector.id)
                if (cid != newCid) {
                    log.error("model title/name conflict...")
                    return false
                }
            }
            val existingLst = existing.lastEditTime
            val cLastEditTime = model.lastEditTime
            // let > cLet, consider as there is modification from other connectors, don't, store,
            // wait syncToConnector to execute first
            if (existingLst!!.isAfter(cLastEditTime)) {
                return false
            }
            // check existing connector last edit time, if cLet > existing cLet, do update  else do nothing.
            val existingCLet = existing.getLastEditOfConnector(connector.id)
            if (existingCLet != null && existingCLet.isBefore(cLastEditTime)) {
                return true
            }
            return false
        }
    }

    private fun syncToConnector(model: Model, connector: Connector) {
        connector.update(model)
    }
}