package pro.devsvc.unitask.connector.notion.utils

import notion.api.v1.model.databases.DatabaseProperty
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageProperty

fun titlePageProperty(str: String): PageProperty {
    return PageProperty(title = listOf(PageProperty.RichText(text = PageProperty.RichText.Text(content = str))))
}

