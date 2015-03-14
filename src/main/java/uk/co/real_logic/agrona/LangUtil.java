/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona;

/**
 * Grouping of language level utilities to make programming in Java more convenient.
 */
public class LangUtil
{
    /**
     * Rethrow an {@link java.lang.Exception} preserving the stack trace but making it unchecked.
     *
     * @param ex to be rethrown and unchecked.
     */
    public static void rethrowUnchecked(final Exception ex)
    {
        LangUtil.<RuntimeException>rethrow(ex);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Exception> void rethrow(final Exception ex)
        throws T
    {
        throw (T)ex;
    }
}
