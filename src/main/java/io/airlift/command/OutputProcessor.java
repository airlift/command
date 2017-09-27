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

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static io.airlift.command.Command.submit;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

class OutputProcessor
{
    private final InputStream inputStream;
    private final Executor executor;
    private Future<String> outputFuture;

    public OutputProcessor(Process process, Executor executor)
    {
        this.inputStream = requireNonNull(process, "process is null").getInputStream();
        this.executor = requireNonNull(executor, "executor is null");
    }

    public void start()
    {
        outputFuture = submit(executor, () -> CharStreams.toString(new InputStreamReader(inputStream, UTF_8)));
    }

    public String getOutput()
    {
        if ((outputFuture != null) && !outputFuture.isCancelled()) {
            try {
                return outputFuture.get();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (ExecutionException ignored) {
            }
        }
        return null;
    }

    public void destroy()
    {
        // close input stream which will normally interrupt the reader
        try {
            inputStream.close();
        }
        catch (IOException ignored) {
        }

        if (outputFuture != null) {
            outputFuture.cancel(true);
        }
    }
}
