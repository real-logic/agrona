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
package org.agrona.agent;

import static net.bytebuddy.asm.Advice.to;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import org.agrona.DirectBuffer;
import org.agrona.agent.BufferAlignmentInterceptor.CharVerifier;
import org.agrona.agent.BufferAlignmentInterceptor.DoubleVerifier;
import org.agrona.agent.BufferAlignmentInterceptor.FloatVerifier;
import org.agrona.agent.BufferAlignmentInterceptor.IntVerifier;
import org.agrona.agent.BufferAlignmentInterceptor.LongVerifier;
import org.agrona.agent.BufferAlignmentInterceptor.ShortVerifier;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * A Java agent that verifies that all memory accesses in {@link DirectBuffer} implementations are aligned.
 * <p>
 * Unaligned accesses can be slower or even make the JVM crash on some architectures.
 * <p>
 * Using this agent will avoid such crashes, but it has a performance overhead and should only be used for testing
 * and debugging.
 */
public final class BufferAlignmentAgent
{
    private static ResettableClassFileTransformer alignmentTransformer;
    private static Instrumentation instrumentation;

    private BufferAlignmentAgent()
    {
    }

    /**
     * Invoked when the agent is launched with the JVM and before the main application.
     *
     * @param agentArgs       ignored for buffer alignment agent.
     * @param instrumentation for adding bytecode to classes.
     */
    public static void premain(final String agentArgs, final Instrumentation instrumentation)
    {
        agent(false, instrumentation);
    }

    /**
     * Invoked when the agent is attached to an already running application.
     *
     * @param agentArgs       ignored for buffer alignment agent.
     * @param instrumentation for adding bytecode to classes.
     */
    public static void agentmain(final String agentArgs, final Instrumentation instrumentation)
    {
        agent(true, instrumentation);
    }

    private static synchronized void agent(final boolean shouldRedefine, final Instrumentation instrumentation)
    {
        BufferAlignmentAgent.instrumentation = instrumentation;

        // all Int methods, and all String method other than
        // XXXStringWithoutLengthXXX or getStringXXX(int, int)
        final Junction<MethodDescription> intVerifierMatcher = nameContains("Int")
            .or(nameMatches(".*String[^W].*").and(not(ElementMatchers.takesArguments(int.class, int.class))))
            .and(not(ElementMatchers.isPrivate()));

        final AgentBuilder.Transformer transformer =
            (builder, typeDescription, classLoader, javaModule, protectionDomain) ->
            {
                return builder
                    .visit(to(LongVerifier.class).on(nameContains("Long").and(not(ElementMatchers.isPrivate()))))
                    .visit(to(DoubleVerifier.class).on(nameContains("Double").and(not(ElementMatchers.isPrivate()))))
                    .visit(to(IntVerifier.class).on(intVerifierMatcher))
                    .visit(to(FloatVerifier.class).on(nameContains("Float")))
                    .visit(to(ShortVerifier.class).on(nameContains("Short")))
                    .visit(to(CharVerifier.class).on(nameContains("Char")));
            };

        final AgentBuilder.RedefinitionStrategy redefinitionStrategy = shouldRedefine ?
            AgentBuilder.RedefinitionStrategy.RETRANSFORMATION : AgentBuilder.RedefinitionStrategy.DISABLED;

        alignmentTransformer = new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.DISABLED))
            .with(new AgentBuilderListener())
            .disableClassFormatChanges()
            .with(redefinitionStrategy)
            .type(isSubTypeOf(DirectBuffer.class).and(not(isInterface())))
            .transform(transformer)
            .installOn(instrumentation);
    }

    /**
     * Remove the bytecode transformer and associated bytecode weaving so the alignment checks are not made.
     */
    public static synchronized void removeTransformer()
    {
        if (alignmentTransformer != null)
        {
            alignmentTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
            alignmentTransformer = null;
            BufferAlignmentAgent.instrumentation = null;
        }
    }

    static class AgentBuilderListener implements AgentBuilder.Listener
    {
        public void onDiscovery(
            final String typeName,
            final ClassLoader classLoader,
            final JavaModule javaModule,
            final boolean loaded)
        {
        }

        public void onTransformation(
            final TypeDescription typeDescription,
            final ClassLoader classLoader,
            final JavaModule javaModule,
            final boolean loaded,
            final DynamicType dynamicType)
        {
        }

        public void onIgnored(
            final TypeDescription typeDescription,
            final ClassLoader classLoader,
            final JavaModule javaModule,
            final boolean loaded)
        {
        }

        public void onError(
            final String typeName,
            final ClassLoader classLoader,
            final JavaModule javaModule,
            final boolean loaded,
            final Throwable throwable)
        {
            System.err.println("ERROR " + typeName);
            throwable.printStackTrace(System.err);
        }

        public void onComplete(
            final String typeName,
            final ClassLoader classLoader,
            final JavaModule javaModule,
            final boolean loaded)
        {
        }
    }
}
