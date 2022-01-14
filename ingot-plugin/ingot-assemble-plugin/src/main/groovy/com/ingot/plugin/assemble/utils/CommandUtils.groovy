package com.ingot.plugin.assemble.utils

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecSpec

/**
 * <p>Description  : CommandUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:31 AM.</p>
 */
class CommandUtils {

    static final String DONE = "Done"

    static String executeAndWait(List<String> cmdLine) {
        def process = cmdLine.execute()
        process.waitForProcessOutput(System.out, System.err)
        if (process.exitValue()) {
            throw new GradleException("Command execution failed\nCommand line [${cmdLine}]")
        }
        return DONE
    }

    static void execWithErrorMessage(Project project, Action<ExecSpec> execSpecAction) {

    }
}
