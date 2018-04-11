package org.agrona.collections;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    Int2ObjectHashMapConformanceTest.class, Int2IntHashMapConformanceTest.class,
    Long2ObjectHashMapConformanceTest.class, Long2LongHashMapConformanceTest.class})
public class ConformanceTestsSuite
{
    // This is required so that the suites are picked up by gradle
}
