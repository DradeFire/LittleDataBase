package com.example.littledb.processor

import com.example.littledb.annotations.*
import com.example.littledb.processor.ext.errorMessage
import com.example.littledb.processor.ext.noteMessage
import com.example.littledb.processor.helpers.DaoATP
import com.example.littledb.processor.helpers.DatabaseATP
import com.example.littledb.processor.helpers.EntityATP
import com.example.littledb.processor.model.EntityData
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class LittleDatabaseProcessor : AbstractProcessor() {

    private var entityData: EntityData? = null

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            LittleEntity::class.java.name,
            PrimaryKey::class.java.name,
            ColumnName::class.java.name,
        )
    }

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment
    ): Boolean {
        if (annotations == null || annotations.isEmpty()) {
            processingEnv.noteMessage { "TypeElements is null or empty, skip the process." }
            return true
        }

        if (!processEntity(roundEnv)) return false
        if (!processDatabase()) return false
        if (!processDao()) return false

        return true
    }

    private fun processDatabase(): Boolean = DatabaseATP.createDataBase(processingEnv, entityData)

    private fun processEntity(roundEnv: RoundEnvironment): Boolean {
        val entity = roundEnv.getElementsAnnotatedWith(LittleEntity::class.java).also {
            if (it.size > 1) {
                processingEnv.errorMessage { "Can`t exist 2+ @LittleEntity annotated classes" }
                return false
            }
        }.firstOrNull()
        if (entity == null) {
            processingEnv.noteMessage { "Not exist class annotated by @LittleEntity, skip the process." }
            return false
        }

        entityData = EntityATP.createEntity(entity, processingEnv)

        return entityData?.isSuccess != false
    }

    private fun processDao(): Boolean = DaoATP.createDao(processingEnv, entityData)


    companion object {
        const val KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated"
    }

}
