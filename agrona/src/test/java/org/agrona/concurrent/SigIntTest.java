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
package org.agrona.concurrent;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SigIntTest
{
    @Test
    void throwsNullPointerExceptionIfRunnableIsNull()
    {
        assertThrowsExactly(NullPointerException.class, () -> SigInt.register(null));
    }

    @Test
    void shouldInstallHandlerThatWillDelegateToTheExistingHandler() throws InterruptedException
    {
        final Thread.UncaughtExceptionHandler exceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
            Thread.getDefaultUncaughtExceptionHandler();
        try
        {
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

            final Signal signal = new Signal("INT");
            final SignalHandler oldHandler = mock(SignalHandler.class);
            final NumberFormatException oldHandlerException = new NumberFormatException("NaN");
            doThrow(oldHandlerException).when(oldHandler).handle(signal);
            Signal.handle(signal, oldHandler);

            final Runnable newHandler = mock(Runnable.class);
            final CountDownLatch handlerExecuted = new CountDownLatch(1);
            final IllegalStateException newHandlerException = new IllegalStateException("something went wrong");
            doAnswer((Answer<Void>)invocation ->
            {
                handlerExecuted.countDown();
                throw newHandlerException;
            }).when(newHandler).run();

            SigInt.register(newHandler);

            Signal.raise(signal);

            handlerExecuted.await();

            final InOrder inOrder = inOrder(newHandler, oldHandler, exceptionHandler);
            inOrder.verify(newHandler).run();
            inOrder.verify(oldHandler).handle(signal);
            inOrder.verify(exceptionHandler).uncaughtException(any(), eq(newHandlerException));
            inOrder.verifyNoMoreInteractions();

            final Throwable[] suppressed = newHandlerException.getSuppressed();
            assertEquals(1, suppressed.length);
            assertSame(oldHandlerException, suppressed[0]);
        }
        finally
        {
            Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler);
        }
    }
}
