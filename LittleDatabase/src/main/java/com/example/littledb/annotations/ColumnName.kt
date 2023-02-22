package com.example.littledb.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class ColumnName(
    val columnName: String
)
