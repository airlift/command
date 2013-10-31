/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.command;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

class ProcessCallable
        implements Callable<Integer>
{
    private final Command command;
    private final Executor executor;

    public ProcessCallable(Command command, Executor executor)
    {
        this.command = checkNotNull(command, "command is null");
        this.executor = checkNotNull(executor, "executor is null");
    }

    @Override
    public Integer call()
            throws CommandFailedException, InterruptedException
    {
        ProcessBuilder processBuilder = new ProcessBuilder(command.getCommand());
        processBuilder.directory(command.getDirectory());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().putAll(command.getEnvironment());

        // start the process
        Process process;
        try {
            process = processBuilder.start();
        }
        catch (IOException e) {
            throw new CommandFailedException(command, "failed to start", e);
        }

        OutputProcessor outputProcessor = null;
        try {
            // start the output processor
            outputProcessor = new OutputProcessor(process, executor);
            outputProcessor.start();

            // wait for command to exit
            int exitCode = process.waitFor();

            // validate exit code
            if (!command.getSuccessfulExitCodes().contains(exitCode)) {
                String out = outputProcessor.getOutput();
                throw new CommandFailedException(command, exitCode, out);
            }
            return exitCode;
        }
        finally {
            try {
                process.destroy();
            }
            finally {
                if (outputProcessor != null) {
                    outputProcessor.destroy();
                }
            }
        }
    }
}
