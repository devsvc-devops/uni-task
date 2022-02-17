package pro.devsvc.unitask.connector.notion.ext

import notion.api.v1.model.databases.DatabasePropertySchema
import notion.api.v1.model.databases.SelectOptionSchema


open class RelationPropertySchema
@JvmOverloads
constructor(val relation: Relation) : DatabasePropertySchema {
    open class Relation(val database_id: String)
}

open class SelectPropertySchema : DatabasePropertySchema {
    open class Select(val options: List<SelectOptionSchema>? = null)

    private var select: Select? = null

    @JvmOverloads
    constructor(options: List<SelectOptionSchema>? = null) {
        this.select = Select(options)
    }
}
