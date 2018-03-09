/*
 * Copyright 2014-2018 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent.status;

public abstract class StatusIndicatorReader
{
    /**
     * Identifier for this status indicator.
     *
     * @return the identifier for this status indicator.
     */
    public abstract int id();

    /**
     * Get the current status indication of a component with volatile semantics.
     *
     * @return the current status indication of a component with volatile semantics.
     */
    public abstract long getVolatile();
}
