package pro.devsvc.unitask.connector.notion

import notion.api.v1.model.databases.query.filter.QueryTopLevelFilter

open class NCompoundFilter(
    var or: List<NPropertyFilter>? = null,
    var and: List<NPropertyFilter>? = null,
) : QueryTopLevelFilter