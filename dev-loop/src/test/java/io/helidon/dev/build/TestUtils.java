/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
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

package io.helidon.dev.build;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.helidon.build.util.Log;
import io.helidon.dev.build.maven.DefaultHelidonProjectSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Class TestUtils.
 */
class TestUtils {

    private TestUtils() {
    }

    static BuildLoop newLoop(Path projectRoot,
                             boolean initialClean,
                             boolean watchBinariesOnly,
                             int stopCycleNumber) {
        return newLoop(projectRoot, initialClean, watchBinariesOnly, new TestMonitor(stopCycleNumber));
    }

    static BuildLoop newLoop(Path projectRoot,
                             boolean initialClean,
                             boolean watchBinariesOnly,
                             BuildMonitor monitor) {
        return BuildLoop.builder()
                .projectDirectory(projectRoot)
                .clean(initialClean)
                .watchBinariesOnly(watchBinariesOnly)
                .projectSupplier(new DefaultHelidonProjectSupplier(60))
                .stdOut(monitor.stdOutConsumer())
                .stdErr(monitor.stdErrConsumer())
                .buildMonitor(monitor)
                .build();
    }

    static <T extends BuildMonitor> T run(BuildLoop loop) throws InterruptedException {
        return run(loop, 30);
    }

    @SuppressWarnings("unchecked")
    static <T extends BuildMonitor> T run(BuildLoop loop, int maxWaitSeconds) throws InterruptedException {
        loop.start();
        Log.info("Waiting up to %d seconds for build loop completion", maxWaitSeconds);
        if (!loop.waitForStopped(maxWaitSeconds, TimeUnit.SECONDS)) {
            loop.stop(0L);
            fail("Timeout");
        }
        return (T) loop.monitor();
    }
}