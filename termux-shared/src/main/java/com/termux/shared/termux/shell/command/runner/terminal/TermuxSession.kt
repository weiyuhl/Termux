package com.termux.shared.termux.shell.command.runner.terminal

import android.content.Context
import android.system.OsConstants
import com.google.common.base.Joiner
import com.termux.shared.R
import com.termux.shared.errors.Errno
import com.termux.shared.logger.Logger
import com.termux.shared.shell.ShellUtils
import com.termux.shared.shell.command.ExecutionCommand
import com.termux.shared.shell.command.environment.IShellEnvironment
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils
import com.termux.shared.shell.command.environment.UnixShellEnvironment
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

/**
 * A class that maintains info for foreground Termux sessions.
 * It also provides a way to link each [TerminalSession] with the [ExecutionCommand]
 * that started it.
 */
class TermuxSession private constructor(
    private val mTerminalSession: TerminalSession,
    private val mExecutionCommand: ExecutionCommand,
    private val mTermuxSessionClient: TermuxSessionClient?,
    private val mSetStdoutOnExit: Boolean
) {

    val terminalSession: TerminalSession
        get() = mTerminalSession

    val executionCommand: ExecutionCommand
        get() = mExecutionCommand

    /**
     * Signal that this [TermuxSession] has finished.
     */
    fun finish() {
        // If process is still running, then ignore the call
        if (mTerminalSession.isRunning) return

        val exitCode = mTerminalSession.exitStatus

        if (exitCode == 0)
            Logger.logDebug(LOG_TAG, "The \"${mExecutionCommand.getCommandIdAndLabelLogString()}\" TermuxSession exited normally")
        else
            Logger.logDebug(LOG_TAG, "The \"${mExecutionCommand.getCommandIdAndLabelLogString()}\" TermuxSession exited with code: $exitCode")

        // If the execution command has already failed, like SIGKILL was sent, then don't continue
        if (mExecutionCommand.isStateFailed()) {
            Logger.logDebug(LOG_TAG, "Ignoring setting \"${mExecutionCommand.getCommandIdAndLabelLogString()}\" TermuxSession state to ExecutionState.EXECUTED and processing results since it has already failed")
            return
        }

        mExecutionCommand.resultData.exitCode = exitCode

        if (mSetStdoutOnExit)
            mExecutionCommand.resultData.stdout.append(ShellUtils.getTerminalSessionTranscriptText(mTerminalSession, true, false))

        if (!mExecutionCommand.setState(ExecutionCommand.ExecutionState.EXECUTED))
            return

        processTermuxSessionResult(this, null)
    }

    /**
     * Kill this [TermuxSession] by sending a [OsConstants.SIGILL] to its [mTerminalSession]
     * if its still executing.
     */
    fun killIfExecuting(context: Context, processResult: Boolean) {
        // If execution command has already finished executing, then no need to process results or send SIGKILL
        if (mExecutionCommand.hasExecuted()) {
            Logger.logDebug(LOG_TAG, "Ignoring sending SIGKILL to \"${mExecutionCommand.getCommandIdAndLabelLogString()}\" TermuxSession since it has already finished executing")
            return
        }

        Logger.logDebug(LOG_TAG, "Send SIGKILL to \"${mExecutionCommand.getCommandIdAndLabelLogString()}\" TermuxSession")
        if (mExecutionCommand.setStateFailed(Errno.ERRNO_FAILED.code, context.getString(R.string.error_sending_sigkill_to_process))) {
            if (processResult) {
                mExecutionCommand.resultData.exitCode = 137 // SIGKILL

                // Get whatever output has been set till now in case its needed
                if (mSetStdoutOnExit)
                    mExecutionCommand.resultData.stdout.append(ShellUtils.getTerminalSessionTranscriptText(mTerminalSession, true, false))

                processTermuxSessionResult(this, null)
            }
        }

        // Send SIGKILL to process
        mTerminalSession.finishIfRunning()
    }

    interface TermuxSessionClient {
        /**
         * Callback function for when [TermuxSession] exits.
         */
        fun onTermuxSessionExited(termuxSession: TermuxSession)
    }

    companion object {
        private const val LOG_TAG = "TermuxSession"

        /**
         * Start execution of an [ExecutionCommand] with [Runtime.exec].
         */
        @JvmStatic
        fun execute(
            currentPackageContext: Context,
            executionCommand: ExecutionCommand,
            terminalSessionClient: TerminalSessionClient,
            termuxSessionClient: TermuxSessionClient?,
            shellEnvironmentClient: IShellEnvironment,
            additionalEnvironment: HashMap<String, String>?,
            setStdoutOnExit: Boolean
        ): TermuxSession? {
            if (executionCommand.executable != null && executionCommand.executable!!.isEmpty())
                executionCommand.executable = null
            if (executionCommand.workingDirectory.isNullOrEmpty())
                executionCommand.workingDirectory = shellEnvironmentClient.defaultWorkingDirectoryPath
            if (executionCommand.workingDirectory!!.isEmpty())
                executionCommand.workingDirectory = "/"

            var defaultBinPath = shellEnvironmentClient.defaultBinPath
            if (defaultBinPath.isEmpty())
                defaultBinPath = "/system/bin"

            var isLoginShell = false
            if (executionCommand.executable == null) {
                if (!executionCommand.isFailsafe) {
                    for (shellBinary in UnixShellEnvironment.LOGIN_SHELL_BINARIES) {
                        val shellFile = File(defaultBinPath, shellBinary)
                        if (shellFile.canExecute()) {
                            executionCommand.executable = shellFile.absolutePath
                            break
                        }
                    }
                }

                if (executionCommand.executable == null) {
                    // Fall back to system shell as last resort
                    executionCommand.executable = "/system/bin/sh"
                } else {
                    isLoginShell = true
                }
            }

            // Setup command args
            val executableForSetup = executionCommand.executable!!
            val commandArgs = shellEnvironmentClient.setupShellCommandArguments(executableForSetup, executionCommand.arguments)

            executionCommand.executable = commandArgs[0]
            val executablePath = executionCommand.executable!!
            val processName = (if (isLoginShell) "-" else "") + ShellUtils.getExecutableBasename(executablePath)

            val arguments = Array(commandArgs.size) { i ->
                if (i == 0) processName else commandArgs[i]
            }

            executionCommand.arguments = arguments

            if (executionCommand.commandLabel == null)
                executionCommand.commandLabel = processName

            // Setup command environment
            val environment = shellEnvironmentClient.setupShellCommandEnvironment(currentPackageContext, executionCommand)
            if (additionalEnvironment != null)
                environment.putAll(additionalEnvironment)
            val environmentList = ShellEnvironmentUtils.convertEnvironmentToEnviron(environment)
            val environmentArray = environmentList.sorted().toTypedArray()

            if (!executionCommand.setState(ExecutionCommand.ExecutionState.EXECUTING)) {
                executionCommand.setStateFailed(Errno.ERRNO_FAILED.code, currentPackageContext.getString(R.string.error_failed_to_execute_termux_session_command, executionCommand.getCommandIdAndLabelLogString()))
                processTermuxSessionResult(null, executionCommand)
                return null
            }

            Logger.logDebugExtended(LOG_TAG, executionCommand.toString())
            Logger.logVerboseExtended(LOG_TAG, "\"${executionCommand.getCommandIdAndLabelLogString()}\" TermuxSession Environment:\n${Joiner.on("\n").join(environmentArray)}")

            Logger.logDebug(LOG_TAG, "Running \"${executionCommand.getCommandIdAndLabelLogString()}\" TermuxSession")
            val terminalSession = TerminalSession(
                executionCommand.executable,
                executionCommand.workingDirectory,
                executionCommand.arguments,
                environmentArray,
                executionCommand.terminalTranscriptRows,
                terminalSessionClient
            )

            if (executionCommand.shellName != null) {
                terminalSession.mSessionName = executionCommand.shellName
            }

            return TermuxSession(terminalSession, executionCommand, termuxSessionClient, setStdoutOnExit)
        }

        /**
         * Process the results of [TermuxSession] or [ExecutionCommand].
         */
        @JvmStatic
        private fun processTermuxSessionResult(termuxSession: TermuxSession?, executionCommand: ExecutionCommand?) {
            var execCommand = executionCommand
            if (termuxSession != null)
                execCommand = termuxSession.mExecutionCommand

            if (execCommand == null) return

            if (execCommand.shouldNotProcessResults()) {
                Logger.logDebug(LOG_TAG, "Ignoring duplicate call to process \"${execCommand.getCommandIdAndLabelLogString()}\" TermuxSession result")
                return
            }

            Logger.logDebug(LOG_TAG, "Processing \"${execCommand.getCommandIdAndLabelLogString()}\" TermuxSession result")

            if (termuxSession != null && termuxSession.mTermuxSessionClient != null) {
                termuxSession.mTermuxSessionClient.onTermuxSessionExited(termuxSession)
            } else {
                // If a callback is not set and execution command didn't fail, then we set success state now
                if (!execCommand.isStateFailed())
                    execCommand.setState(ExecutionCommand.ExecutionState.SUCCESS)
            }
        }
    }
}
