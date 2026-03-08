package com.termux.shared.shell

import com.termux.shared.file.FileUtils
import com.termux.terminal.TerminalSession

object ShellUtils {

    /** Get process id of [Process]. */
    @JvmStatic
    fun getPid(p: Process): Int {
        return try {
            val f = p.javaClass.getDeclaredField("pid")
            f.isAccessible = true
            try {
                f.getInt(p)
            } finally {
                f.isAccessible = false
            }
        } catch (e: Throwable) {
            -1
        }
    }

    /** Setup shell command arguments for the execute. */
    @JvmStatic
    fun setupShellCommandArguments(executable: String, arguments: Array<String>?): Array<String> {
        val result = ArrayList<String>()
        result.add(executable)
        if (arguments != null) {
            result.addAll(arguments)
        }
        return result.toTypedArray()
    }

    /** Get basename for executable. */
    @JvmStatic
    fun getExecutableBasename(executable: String?): String? {
        return FileUtils.getFileBasename(executable)
    }

    /** Get transcript for [TerminalSession]. */
    @JvmStatic
    fun getTerminalSessionTranscriptText(
        terminalSession: TerminalSession?,
        linesJoined: Boolean,
        trim: Boolean
    ): String? {
        if (terminalSession == null) return null

        val terminalEmulator = terminalSession.emulator ?: return null
        val terminalBuffer = terminalEmulator.screen ?: return null

        var transcriptText = if (linesJoined) {
            terminalBuffer.transcriptTextWithFullLinesJoined
        } else {
            terminalBuffer.transcriptTextWithoutJoinedLines
        }

        if (transcriptText == null) return null

        if (trim) {
            transcriptText = transcriptText.trim()
        }

        return transcriptText
    }
}
