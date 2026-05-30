package com.hyperosunfcker.feature.hyperos.commands

import com.hyperosunfcker.util.LogUtils
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Method

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean
        get() = exitCode == 0
}

object HyperOSCommandUtils {
    private const val TAG = "HyperOSCommandUtils"
    @Volatile
    var lastFailureMessage: String? = null
        private set

    /**
     * Executes a shell command using Shizuku.
     * Returns the output as String, or null if failed.
     */

    fun executeCommand(command: String): String? {
        return execute(command).takeIf { it.success }?.stdout
    }

    fun recordFailure(message: String) {
        lastFailureMessage = message
        LogUtils.w(TAG, message)
    }

    fun execute(command: String): CommandResult {
        LogUtils.i(TAG, "Executing command: $command")
        if (Shizuku.pingBinder()) {
            return executeShizukuCommand(command)
        }

        val result = CommandResult(
            command = command,
            exitCode = -1,
            stdout = "",
            stderr = "Shizuku not available"
        )
        lastFailureMessage = result.userVisibleFailureMessage()
        LogUtils.w(TAG, "Shizuku not available; command was not executed.")
        return result
    }

    private fun executeShizukuCommand(command: String): CommandResult {
        try {
            // newProcess is private in Shizuku API 13.x, use reflection
            val newProcessMethod: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null)
            
            // process is ShizukuRemoteProcess, we can use reflection to get its streams
            val getInputStreamMethod = process!!.javaClass.getMethod("getInputStream")
            val getErrorStreamMethod = process.javaClass.getMethod("getErrorStream")
            val waitForMethod = process.javaClass.getMethod("waitFor")

            val inputStream = getInputStreamMethod.invoke(process) as InputStream
            val errorStream = getErrorStreamMethod.invoke(process) as InputStream
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val errorReader = BufferedReader(InputStreamReader(errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }

            val exitCode = waitForMethod.invoke(process) as Int
            val result = CommandResult(
                command = command,
                exitCode = exitCode,
                stdout = output.toString().trim(),
                stderr = errorOutput.toString().trim()
            )
            if (exitCode != 0) {
                lastFailureMessage = result.userVisibleFailureMessage()
                LogUtils.e(TAG, "Shizuku Command failed with exit code $exitCode. Error: ${errorOutput.toString().trim()}")
            } else {
                lastFailureMessage = null
            }

            return result
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error executing Shizuku command: ${e.message}", e)
            val result = CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = e.message.orEmpty()
            )
            lastFailureMessage = result.userVisibleFailureMessage()
            return result
        }
    }

    private fun CommandResult.userVisibleFailureMessage(): String {
        val reason = stderr.ifBlank { stdout }.ifBlank { "Command exited with code $exitCode" }
        return "Command failed: $reason"
    }
}
