package pro.devsvc.unitask.connector.zentao

import org.junit.Test
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.store.nitrite.NitriteStore
import java.time.format.DateTimeFormatter

class TestZentaoConnector {

    private val store = NitriteStore()

    @Test
    fun test() {
        val connector = ZentaoConnector(
            "http://pms.sinandata.com:8088/biz/",
            System.getProperty("ztUser"),
            System.getProperty("ztPwd"))

        connector.start(store)
    }
}