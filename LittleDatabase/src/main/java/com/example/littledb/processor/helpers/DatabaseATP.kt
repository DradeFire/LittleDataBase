package com.example.littledb.processor.helpers

import com.example.littledb.processor.LittleDatabaseProcessor
import com.example.littledb.processor.ext.errorMessage
import com.example.littledb.processor.model.EntityData
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.ProcessingEnvironment

object DatabaseATP {

    fun createDataBase(
        processingEnv: ProcessingEnvironment,
        entityData: EntityData?
    ): Boolean {
        if (entityData == null) return false
        val generatedSource =
            processingEnv.options[LittleDatabaseProcessor.KAPT_KOTLIN_GENERATED] ?: run {
                processingEnv.errorMessage { "Can`t find target source" }
                return false
            }

        val fileName = "LittleDatabase"
        val packageName = "com.example"

        val constructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameter(
                name = "context",
                type = ClassName(
                    "android.content",
                    "Context"
                )
            )
            .addParameter(
                name = "databaseName",
                type = ClassName(
                    "kotlin",
                    "String"
                )
            )
            .addParameter(
                name = "version",
                type = ClassName(
                    "kotlin",
                    "Int"
                )
            )
            .build()

        val tableName = PropertySpec.builder("tableName", ClassName("kotlin", "String"))
            .addModifiers(KModifier.FINAL)
            .initializer("\"${entityData.tableName}\"")
            .build()

        val daoProperty = PropertySpec.builder("dao", ClassName("com.example", "LittleDaoApi"))
            .addModifiers(KModifier.FINAL, KModifier.PRIVATE)
            .initializer("LittleDaoApi(this)")
            .build()


        val onCreate = FunSpec.builder("onCreate")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("db", ClassName("android.database.sqlite", "SQLiteDatabase"))
            .addCode(
                """
                db.execSQL("${entityData.createSQL}")
            """.trimIndent()
            )
            .build()

        val onUpgrade = FunSpec.builder("onUpgrade")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("db", ClassName("android.database.sqlite", "SQLiteDatabase"))
            .addParameter("oldVersion", ClassName("kotlin", "Int"))
            .addParameter("newVersion", ClassName("kotlin", "Int"))
            .addCode(
                """
                db.execSQL("${entityData.deleteSQL}")
                onCreate(db)
            """.trimIndent()
            )
            .build()

        val onDowngrade = FunSpec.builder("onDowngrade")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("db", ClassName("android.database.sqlite", "SQLiteDatabase"))
            .addParameter("oldVersion", ClassName("kotlin", "Int"))
            .addParameter("newVersion", ClassName("kotlin", "Int"))
            .addCode(
                """
                onUpgrade(db, oldVersion, newVersion)
            """.trimIndent()
            )
            .build()


        val companionObject = TypeSpec.companionObjectBuilder()
            .addFunction(
                FunSpec.builder("factory")
                    .addParameter(
                        name = "context",
                        type = ClassName(
                            "android.content",
                            "Context"
                        )
                    )
                    .addParameter(
                        name = "databaseName",
                        type = ClassName(
                            "kotlin",
                            "String"
                        )
                    )
                    .addParameter(
                        name = "version",
                        type = ClassName(
                            "kotlin",
                            "Int"
                        )
                    )
                    .addStatement("return LittleDatabase(context, databaseName, version).dao")
                    .build()
            )
            .build()

        val dbClass = TypeSpec.classBuilder(fileName)
            .primaryConstructor(constructor)
            .superclass(ClassName("android.database.sqlite", "SQLiteOpenHelper"))
            .addSuperclassConstructorParameter("%N", "context")
            .addSuperclassConstructorParameter("%N", "databaseName")
            .addSuperclassConstructorParameter("null")
            .addSuperclassConstructorParameter("%N", "version")
            .addProperty(tableName)
            .addProperty(daoProperty)
            .addFunction(onCreate)
            .addFunction(onUpgrade)
            .addFunction(onDowngrade)
            .addType(companionObject)
            .build()

        val file = FileSpec
            .builder(packageName, fileName)
            .addType(dbClass)
            .build()

        file.writeTo(File(generatedSource))

        return true
    }

}
