/*
 * Copyright 2014-2023 Real Logic Limited.
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
package org.agrona.collections;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    Int2ObjectHashMapConformanceTest.class,
    Int2IntHashMapConformanceTest.class,
    Int2NullableObjectHashMapConformanceTest.class,
    Long2ObjectHashMapConformanceTest.class,
    Long2LongHashMapConformanceTest.class,
    Long2NullableObjectHashMapConformanceTest.class,
    Object2IntHashMapConformanceTest.class,
    Object2LongHashMapConformanceTest.class,
    Object2ObjectHashMapConformanceTest.class,
    Object2NullableObjectHashMapConformanceTest.class})
public class ConformanceTestsSuite
{
    // This is required so that the suites are picked up by gradle
}
