package pro.devsvc.unitask.core.model

import kotlinx.serialization.Serializable

@Serializable
class Project(
    /** id from notion is uuid, so have to be string */
    var id: String,
    var title: String,
)