package pro.devsvc.unitask.connector

import org.slf4j.LoggerFactory

object ConnectorManager {
    private val log = LoggerFactory.getLogger(javaClass)

    private val connectors = mutableMapOf<String, Connector>()

    fun registerConnector(id: String, connector: Connector) {
        connectors[id] = connector
    }

    fun sync() {
        for ((id, connector) in connectors) {
            log.info("start sync with connector: {}...", id)
        }
    }
}