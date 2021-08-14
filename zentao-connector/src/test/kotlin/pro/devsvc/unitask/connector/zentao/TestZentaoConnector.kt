package pro.devsvc.unitask.connector.zentao

import org.junit.Test
import pro.devsvc.unitask.store.nitrite.NitriteStore

class TestZentaoConnector {

    private val store = NitriteStore()

    @Test
    fun test() {
        val connector = ZentaoConnector(
            "http://pms.sinandata.com:8088/biz/",
            System.getProperty("ztUser"),
            System.getProperty("ztPwd")
        )

        connector.start(store)
    }
}