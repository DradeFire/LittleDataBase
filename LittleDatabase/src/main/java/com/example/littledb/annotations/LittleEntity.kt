package com.example.littledb.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class LittleEntity(
    val table: String
)
