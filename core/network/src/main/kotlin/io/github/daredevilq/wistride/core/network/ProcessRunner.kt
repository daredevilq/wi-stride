package io.github.daredevilq.wistride.core.network

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** A started child process whose merged stdout/stderr is read line by line. */
interface RunningProcess {
    /** Blocks for the next output line; null at end of output (process ended or was destroyed). */
    fun readLine(): String?

    /** Asks the process to stop (SIGTERM); makes a blocked [readLine] return null. */
    fun destroy()

    /** Kills the process (SIGKILL). */
    fun destroyForcibly()

    /** Waits up to [timeoutMs] for the process to exit; true if it has exited. */
    fun waitForExit(timeoutMs: Long): Boolean
}

/** Starts child processes; an abstraction so `ping` handling is testable without a real binary. */
fun interface ProcessRunner {
    /** Throws [IOException] when the executable cannot be started. */
    fun start(command: List<String>): RunningProcess
}

/** [ProcessRunner] backed by `ProcessBuilder`; stderr is merged so error messages reach the parser. */
class SystemProcessRunner
    @Inject
    constructor() : ProcessRunner {
        override fun start(command: List<String>): RunningProcess {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
            return object : RunningProcess {
                override fun readLine(): String? =
                    try {
                        reader.readLine()
                    } catch (_: IOException) {
                        // Stream closed by destroy() while blocked.
                        null
                    }

                override fun destroy() = process.destroy()

                override fun destroyForcibly() {
                    process.destroyForcibly()
                }

                override fun waitForExit(timeoutMs: Long): Boolean = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            }
        }
    }
