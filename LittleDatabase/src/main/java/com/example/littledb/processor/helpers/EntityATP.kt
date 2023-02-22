package com.example.littledb.processor.helpers

import com.example.littledb.annotations.ColumnName
import com.example.littledb.annotations.LittleEntity
import com.example.littledb.annotations.PrimaryKey
import com.example.littledb.processor.ext.errorMessage
import com.example.littledb.processor.model.EntityData
import com.example.littledb.processor.model.QueryAndTableName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

object EntityATP {

    private fun getCreateQuery(
        entity: Element,
        processingEnv: ProcessingEnvironment
    ): QueryAndTableName? {
        val allMember = processingEnv.elementUtils.getAllMembers(entity as TypeElement)
        val fields = ElementFilter.fieldsIn(allMember)

        val tableName = entity.getAnnotation(LittleEntity::class.java).table

        val columnNameSet = hashSetOf<String>()
        val columnToField: HashMap<String, Element> = hashMapOf()
        var namePrimaryField = ""
        var query = "CREATE TABLE $tableName ("
        fields.mapIndexed { index, field ->
            val primaryKeyAnnotation = field.getAnnotationsByType(PrimaryKey::class.java).firstOrNull()
            val columnNameAnnotation = field.getAnnotationsByType(ColumnName::class.java).firstOrNull()

            if (columnNameAnnotation == null) {
                processingEnv.errorMessage { "Exist field without @ColumnName" }
                return null
            }

            if (columnNameSet.contains(columnNameAnnotation.columnName)) {
                processingEnv.errorMessage { "Not unique @ColumnName.columnName" }
                return null
            }

            columnNameSet.add(columnNameAnnotation.columnName)
            columnToField[columnNameAnnotation.columnName] = field

            if (primaryKeyAnnotation != null) {
                if (namePrimaryField != "") {
                    processingEnv.errorMessage { "Exist 2+ PrimaryKeys" }
                    return null
                }
                namePrimaryField = columnNameAnnotation.columnName
            }

            query += "${columnNameAnnotation.columnName} "

            query += when (field.simpleName.toString()) {
                Int::class.java.canonicalName -> "INTEGER "
                Float::class.java.canonicalName, Double::class.java.canonicalName -> "FLOAT "
                else -> "TEXT "
            }

            query += if (namePrimaryField == columnNameAnnotation.columnName)
                    "PRIMARY KEY"
                else
                    ""

            if (index != fields.lastIndex) {
                query += ",\n"
            }
        }
        query += ")"
        return QueryAndTableName(query, tableName, columnNameSet, columnToField)
    }

    private fun getDeleteQuery(tableName: String): String = "DROP TABLE IF EXISTS $tableName"


    fun createEntity(
        entity: Element,
        processingEnv: ProcessingEnvironment
    ): EntityData {
        val data = EntityData(true, entity)

        val createQueryAndTableName = getCreateQuery(entity, processingEnv)
            ?: return data.apply { isSuccess = false }

        val deleteQuery = getDeleteQuery(createQueryAndTableName.tableName)

        return data.apply {
            createSQL = createQueryAndTableName.query
            columnNameSet = createQueryAndTableName.columnNameSet
            tableName = createQueryAndTableName.tableName
            deleteSQL = deleteQuery
            columnToField = createQueryAndTableName.columnToField
        }
    }

}

