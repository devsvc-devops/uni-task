package pro.devsvc.unitask

import pro.devsvc.unitask.connector.notion.NotionConnector
import pro.devsvc.unitask.connector.zentao.ZentaoConnector
import pro.devsvc.unitask.core.connector.ConnectorManager
import kotlin.test.Test

internal class Test {
    @Test
    fun test() {
        ConnectorManager.registerConnector("notion", NotionConnector(database = "Tasks"))
        ConnectorManager.registerConnector("zentao", ZentaoConnector("http://pms.sinandata.com:8088/biz/", System.getProperty("ztUser"), System.getProperty("ztPwd")))
        ConnectorManager.start()
        ConnectorManager.sync()
    }
}