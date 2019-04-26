/*
 * Copyright 2019 Real Logic Ltd.
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
package org.agrona;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class PrintBufferUtilTest
{
    @Test
    public void shouldPrettyPrintHex()
    {
        final String contents = "Hello World!\nThis is a test String\nto print out.";
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

        buffer.putStringAscii(0, contents);

        final StringBuilder builder = new StringBuilder();
        PrintBufferUtil.appendPrettyHexDump(builder, buffer);
        assertThat(builder.toString(), containsString("0...Hello World!"));
    }
}