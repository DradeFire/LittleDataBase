package com.example.littledb.processor.helpers

import com.example.littledb.annotations.ColumnName
import com.example.littledb.annotations.PrimaryKey
import com.example.littledb.processor.LittleDatabaseProcessor.Companion.KAPT_KOTLIN_GENERATED
import com.example.littledb.processor.ext.errorMessage
import com.example.littledb.processor.model.EntityData
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import kotlin.text.StringBuilder

object DaoATP {

    fun createDao(
        processingEnv: ProcessingEnvironment,
        entityData: EntityData?
    ): Boolean {
        if (entityData == null) return false
        val generatedSource = processingEnv.options[KAPT_KOTLIN_GENERATED] ?: run {
            processingEnv.errorMessage { "Can`t find target source" }
            return false
        }

        val mapFields = hashMapOf<String, String>()

        val allMember = processingEnv.elementUtils.getAllMembers(entityData.entity as TypeElement)
        val fields = ElementFilter.fieldsIn(allMember)

        var primaryKeyColumnName = ""
        fields.mapIndexed { _, field ->
            val primaryKeyAnnotation =
                field.getAnnotationsByType(PrimaryKey::class.java).firstOrNull()
            val columnNameAnnotation =
                field.getAnnotationsByType(ColumnName::class.java).firstOrNull()

            if (primaryKeyAnnotation != null) {
                primaryKeyColumnName = columnNameAnnotation?.columnName.toString()
            }

            mapFields[columnNameAnnotation?.columnName.toString()] = field.toString()
        }

        val addLines = StringBuilder().also {
            mapFields.keys.map { key ->
                it.append(
                    "put(\"${key}\", entity.${mapFields[key]}) \n"
                )
            }
        }
        val readLines = StringBuilder().also {
            mapFields.keys.map { key ->
                it.append(
                    "${mapFields[key]} = ${
                        getStringOfField(
                            key,
                            entityData.columnToField[key]!!
                        )
                    },\n"
                )
            }
        }

        val fileName = "LittleDaoApi"

        val littleDaoApiClass = TypeSpec.classBuilder(fileName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(
                        name = "littleDatabase",
                        type = ClassName(
                            "com.example",
                            "LittleDatabase"
                        )
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "readableDatabase",
                    ClassName("android.database.sqlite", "SQLiteDatabase"),
                    modifiers = arrayOf(KModifier.FINAL, KModifier.PRIVATE)
                ).mutable(false).initializer("littleDatabase.readableDatabase").build()
            )
            .addProperty(
                PropertySpec.builder(
                    "writableDatabase",
                    ClassName("android.database.sqlite", "SQLiteDatabase"),
                    modifiers = arrayOf(KModifier.FINAL, KModifier.PRIVATE)
                ).mutable(false).initializer("littleDatabase.writableDatabase").build()
            )
            .addProperty(
                PropertySpec.builder(
                    "tableName",
                    String::class,
                    modifiers = arrayOf(KModifier.FINAL, KModifier.PRIVATE)
                ).mutable(false).initializer("littleDatabase.tableName").build()
            )
            .addFunction(
                FunSpec.builder("insert")
                    .addParameter(
                        "entity",
                        ClassName(
                            entityData.entity.toString()
                                .substring(0, entityData.entity.toString().lastIndexOf('.')),
                            entityData.entity.simpleName.toString()
                        )
                    )
                    .addCode(
                        """
                        val values = ContentValues().apply {
                            $addLines
                        }
                        
                        writableDatabase.insert("${entityData.tableName}", null, values)
                    """.trimIndent()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("delete")
                    .addParameter(
                        "entity",
                        ClassName(
                            entityData.entity.toString()
                                .substring(0, entityData.entity.toString().lastIndexOf('.')),
                            entityData.entity.simpleName.toString()
                        )
                    )
                    .addCode(
                        """
                        val selection = "$primaryKeyColumnName LIKE ?"
                        
                        val selectionArgs = arrayOf(entity.${mapFields[primaryKeyColumnName]}.toString())
                        
                        writableDatabase.delete(tableName, selection, selectionArgs)
                    """.trimIndent()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("update")
                    .addParameter(
                        "entity",
                        ClassName(
                            entityData.entity.toString()
                                .substring(0, entityData.entity.toString().lastIndexOf('.')),
                            entityData.entity.simpleName.toString()
                        )
                    )
                    .addCode(
                        """
                        val values = ContentValues().apply {
                             $addLines
                        }
                        
                        val selection = "$primaryKeyColumnName LIKE ?"
                        val selectionArgs = arrayOf(entity.${mapFields[primaryKeyColumnName]}.toString())
                        
                        writableDatabase.update(
                            tableName,
                            values,
                            selection,
                            selectionArgs
                        )
                    """.trimIndent()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("read")
                    .returns(
                        ClassName("kotlin.collections", "List")
                            .parameterizedBy(
                                ClassName(
                                    entityData.entity.toString().substring(
                                        0,
                                        entityData.entity.toString().lastIndexOf('.')
                                    ),
                                    entityData.entity.simpleName.toString()
                                )
                            )
                    )
                    .addCode(
                        """
                        val projection = arrayOf("$primaryKeyColumnName")
                        
                        val cursor = readableDatabase.query(
                            tableName,
                            projection,
                            null,
                            null,
                            null,
                            null,
                            null
                        )
                        
                        val itemList = mutableListOf<${entityData.entity.simpleName}>()
                        with(cursor) {
                            while (moveToNext()) {
                                itemList.add(
                                    ${entityData.entity.simpleName}(
                                        $readLines
                                    )
                                )
                            }
                        }
                        cursor.close()
                        return itemList
                    """.trimIndent()
                    )
                    .build()
            )
            .build()

        val file = FileSpec
            .builder("com.example", fileName)
            .addImport("android.content", "ContentValues")
            .addType(littleDaoApiClass)
            .build()

        file.writeTo(File(generatedSource))

        return true
    }

    private fun getStringOfField(key: String, element: Element): String {
        return when (element.simpleName.toString()) {
            Int::class.java.canonicalName -> getIntString(key)
            Float::class.java.canonicalName -> getFloatString(key)
            Double::class.java.canonicalName -> getDoubleString(key)
            else -> getStringString(key)
        }
    }

    private fun getFloatString(key: String): String = "getFloat(getColumnIndexOrThrow(\"$key\"))"
    private fun getDoubleString(key: String): String = "getDouble(getColumnIndexOrThrow(\"$key\"))"
    private fun getStringString(key: String): String = "getString(getColumnIndexOrThrow(\"$key\"))"
    private fun getIntString(key: String): String = "getInt(getColumnIndexOrThrow(\"$key\"))"

}