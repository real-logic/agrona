/*
 * Copyright 2014-2024 Real Logic Limited.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * One time barrier for blocking one or more threads until a SIGINT or SIGTERM signal is received from the operating
 * system or by programmatically calling {@link #signal()}. Useful for shutting down a service.
 */
public class ShutdownSignalBarrier
{
    /**
     * Signals the barrier will be registered for.
     */
    private static final String[] SIGNAL_NAMES = { "INT", "TERM" };
    private static final ArrayList<CountDownLatch> LATCHES = new ArrayList<>();

    static
    {
        try
        {
            final Class<?> signalClass = Class.forName("jdk.internal.misc.Signal");
            final Class<?> signalHandlerClass = Class.forName("jdk.internal.misc.Signal$Handler");
            final Constructor<?> signalConstructor = signalClass.getConstructor(String.class);
            final Method handle = signalClass.getMethod("handle", signalClass, signalHandlerClass);

            final Object handler = Proxy.newProxyInstance(
                signalHandlerClass.getClassLoader(),
                new Class<?>[]{ signalHandlerClass },
                (proxy, method, args) ->
                {
                    if (signalHandlerClass == method.getDeclaringClass())
                    {
                        ShutdownSignalBarrier.signalAndClearAll();
                    }
                    else if (Object.class == method.getDeclaringClass())
                    {
                        if (method.getName().equals("toString"))
                        {
                            return args[0].toString();
                        }
                    }
                    return null;
                });


            for (final String name : SIGNAL_NAMES)
            {
                final Object signal = signalConstructor.newInstance(name);
                handle.invoke(null, signal, handler);
            }
        }
        catch (final ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Construct and register the barrier ready for use.
     */
    public ShutdownSignalBarrier()
    {
        synchronized (LATCHES)
        {
            LATCHES.add(latch);
        }
    }

    /**
     * Programmatically signal awaiting threads on the latch associated with this barrier.
     */
    public void signal()
    {
        synchronized (LATCHES)
        {
            LATCHES.remove(latch);
            latch.countDown();
        }
    }

    /**
     * Programmatically signal all awaiting threads.
     */
    public void signalAll()
    {
        signalAndClearAll();
    }

    /**
     * Remove the barrier from the shutdown signals.
     */
    public void remove()
    {
        synchronized (LATCHES)
        {
            LATCHES.remove(latch);
        }
    }

    /**
     * Await the reception of the shutdown signal.
     */
    public void await()
    {
        try
        {
            latch.await();
        }
        catch (final InterruptedException ignore)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static void signalAndClearAll()
    {
        synchronized (LATCHES)
        {
            LATCHES.forEach(CountDownLatch::countDown);
            LATCHES.clear();
        }
    }
}
