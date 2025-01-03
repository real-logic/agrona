/*
 * Copyright 2014-2025 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.generation;

/**
 * Extended version of the {@link OutputManager} allowing the specification of packages for selected outputs.
 */
public interface DynamicPackageOutputManager extends OutputManager
{
    /**
     * Sets the package name to be used by the Writer obtained through the very next call to {@link
     * #createOutput(java.lang.String) }. A subsequent call to {@link #createOutput(java.lang.String)} should
     * use the initial package name, whatever that was.
     *
     * @param packageName the packageName to be applied to output. The first invocation will be captured as the
     *                    initial package name.
     */
    void setPackageName(String packageName);
}
