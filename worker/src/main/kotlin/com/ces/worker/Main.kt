package com.ces.worker

import com.ces.worker.module.workerModule
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

fun main(): Unit = runBlocking {

    startKoin {
        modules(workerModule)
    }

    val executor: CodeExecutor = GlobalContext.get().get(CodeExecutor::class)

    executor.run()
}