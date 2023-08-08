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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.*;

class SigIntTest
{
    @Test
    void throwsNullPointerExceptionIfRunnableIsNull()
    {
        assertThrowsExactly(NullPointerException.class, () -> SigInt.register(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "INT", "TERM" })
    void shouldReplaceExistingSignalHandler(final String name) throws InterruptedException
    {
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
            Thread.getDefaultUncaughtExceptionHandler();
        final Signal signal = new Signal(name);
        final SignalHandler originalHandler = Signal.handle(signal, sig -> {});

        try
        {
            final CountDownLatch executed = new CountDownLatch(1);
            final Thread.UncaughtExceptionHandler exceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
            doAnswer((Answer<Void>)invocation ->
            {
                executed.countDown();
                return null;
            }).when(exceptionHandler).uncaughtException(any(), any());
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

            final SignalHandler oldHandler = mock(SignalHandler.class);
            Signal.handle(signal, oldHandler);

            final Runnable newHandler = mock(Runnable.class);
            final IllegalStateException firstException = new IllegalStateException("something went wrong");
            doThrow(firstException).when(newHandler).run();

            SigInt.register(name, newHandler);

            Signal.raise(signal);

            executed.await();

            final InOrder inOrder = inOrder(newHandler, exceptionHandler);
            inOrder.verify(newHandler).run();
            inOrder.verify(exceptionHandler).uncaughtException(any(), eq(firstException));
            inOrder.verifyNoMoreInteractions();

            verifyNoInteractions(oldHandler);
        }
        finally
        {
            Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler);
            Signal.handle(signal, originalHandler);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "INT" })
    void shouldReplaceExistingSignalHandlerNoException(final String name) throws InterruptedException
    {
        final Signal signal = new Signal(name);
        final SignalHandler originalHandler = Signal.handle(signal, sig -> {});

        try
        {
            final SignalHandler oldHandler = mock(SignalHandler.class);
            Signal.handle(signal, oldHandler);

            final CountDownLatch executed = new CountDownLatch(1);
            SigInt.register(name, executed::countDown);

            Signal.raise(signal);

            executed.await();
            verifyNoInteractions(oldHandler);
        }
        finally
        {
            Signal.handle(signal, originalHandler);
        }
    }
}
