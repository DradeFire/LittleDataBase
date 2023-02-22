package com.example.littledb.processor.model

import javax.lang.model.element.Element

data class QueryAndTableName(
    var query: String,
    var tableName: String,
    val columnNameSet: HashSet<String>,
    val columnToField: HashMap<String, Element>
)

data class EntityData(
    var isSuccess: Boolean,
    var entity: Element,
    var createSQL: String = "",
    var deleteSQL: String = "",
    var tableName: String = "",
    var columnNameSet: HashSet<String> = hashSetOf(),
    var columnToField: HashMap<String, Element> = hashMapOf()
)