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

import java.util.List;
import java.util.function.Predicate;

/**
 * A project build step.
 */
public interface BuildStep extends Predicate<BuildComponent> {

    /**
     * Returns the name of this build step.
     *
     * @return the name.
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the input type to which this step will apply.
     *
     * @return The type.
     */
    BuildType inputType();

    /**
     * Returns the output type that this step will produce.
     *
     * @return The type.
     */
    BuildType outputType();

    @Override
    default boolean test(BuildComponent component) {
        return component.sourceRoot().buildType() == inputType()
               && component.outputRoot().buildType() == outputType();
    }

    /**
     * Remove any output artifacts for the given component.
     *
     * @param component The component.
     */
    default void clean(BuildComponent component) {
        if (test(component)) {
            component.outputRoot().clean();
        }
    }

    /**
     * Execute the build step for the given components, skipping any up-to-date result. Any component
     * that does not match this predicate is ignored.
     *
     * @param components The components.
     * @return A list of build errors, empty on success.
     */
    List<String> build(List<BuildComponent> components);

    /**
     * Execute the build step for the given changed files only, skipping any up-to-date result. Any component
     * that does not match this predicate is ignored.
     *
     * @param changes The changes.
     * @return A list of build errors, empty on success.
     */
    List<String> incrementalBuild(List<BuildRoot.Changes> changes);
}