package com.example.littledb.processor.ext

import javax.annotation.processing.ProcessingEnvironment
import javax.tools.Diagnostic

internal fun ProcessingEnvironment.noteMessage(message: () -> String) {
    this.messager.printMessage(Diagnostic.Kind.NOTE, message())
}

internal fun ProcessingEnvironment.errorMessage(message: () -> String) {
    this.messager.printMessage(Diagnostic.Kind.ERROR, message())
}