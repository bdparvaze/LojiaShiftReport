package com.lojia.shiftreport.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SerialUpdater {
    private val mutex = Mutex()
    suspend fun <T> update(block: suspend () -> T): T = mutex.withLock { block() }
}
