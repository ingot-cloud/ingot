package com.ingot.plugin.assemble.utils

import org.gradle.api.GradleException

/**
 * <p>Description  : CommandUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:31 AM.</p>
 */
class CommandUtils {

    static final String DONE = "Done"

    static String executeAndWait(List<String> cmdLine){
        def process = cmdLine.execute()
        process.waitForProcessOutput(System.out, System.err)
        if (process.exitValue()) {
            throw new GradleException("Command execution failed\nCommand line [${cmdLine}]")
        }
        return DONE
    }
}
