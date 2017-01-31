package org.agrona.agent;

import static net.bytebuddy.asm.Advice.to;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

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
 * An agent that verifies that all memory accesses in {@link DirectBuffer} implementations are aligned<br>
 * Unaligned accesses can be slower or even make the JVM crash on some architectures.<br>
 * Using this agent will avoid such crashes, but it has a performance overhead and should only be used for testing and debugging
 */

public class BufferAlignmentAgent
{

    private static ClassFileTransformer alignmentTransformer;
    private static Instrumentation instrumentation;

    public static void premain(final String agentArgs, final Instrumentation instrumentation)
    {
        agent(false, instrumentation);
    }

    public static void agentmain(final String agentArgs, final Instrumentation instrumentation)
    {
        agent(true, instrumentation);
    }

    private static synchronized void agent(final boolean redefine, final Instrumentation instrumentation)
    {

        BufferAlignmentAgent.instrumentation = instrumentation;
        // all Int methods, and all String method other than
        // XXXStringWithoutLengthXXX or getStringXXX(int, int)
        final Junction<MethodDescription> intVerifyerMatcher = nameContains("Int")
                .or(nameMatches(".*String[^W].*").and(not(ElementMatchers.takesArguments(int.class, int.class))));

        alignmentTransformer = new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.DISABLED)).with(LISTENER)
                .disableClassFormatChanges()
                .with(redefine ? AgentBuilder.RedefinitionStrategy.RETRANSFORMATION : AgentBuilder.RedefinitionStrategy.DISABLED)
                .type(isSubTypeOf(DirectBuffer.class).and(not(isInterface())))
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .visit(to(LongVerifier.class).on(nameContains("Long")))
                        .visit(to(DoubleVerifier.class).on(nameContains("Double")))
                        .visit(to(IntVerifier.class).on(intVerifyerMatcher))
                        .visit(to(FloatVerifier.class).on(nameContains("Float")))
                        .visit(to(ShortVerifier.class).on(nameContains("Short")))
                        .visit(to(CharVerifier.class).on(nameContains("Char"))))
                .installOn(instrumentation);
    }

    private static final AgentBuilder.Listener LISTENER = new AgentBuilder.Listener()
    {
        @Override
        public void onTransformation(final TypeDescription typeDescription, final ClassLoader classLoader,
            final JavaModule module, final boolean loaded, final DynamicType dynamicType)
        {
            System.out.format("TRANSFORM %s%n", typeDescription.getName());
        }

        @Override
        public void onIgnored(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module,
            final boolean loaded)
        {
        }

        @Override
        public void onError(final String typeName, final ClassLoader classLoader, final JavaModule module, final boolean loaded,
            final Throwable throwable)
        {
            System.out.format("ERROR %s%n", typeName);
            throwable.printStackTrace(System.out);
        }

        @Override
        public void onComplete(final String typeName, final ClassLoader classLoader, final JavaModule module,
            final boolean loaded)
        {
        }
    };

    public static synchronized void removeTransformer()
    {
        if (alignmentTransformer != null)
        {
            instrumentation.removeTransformer(alignmentTransformer);
            instrumentation.removeTransformer(
                    new AgentBuilder.Default()
                        .type(isSubTypeOf(DirectBuffer.class).and(not(isInterface())))
                        .transform(AgentBuilder.Transformer.NoOp.INSTANCE).installOn(instrumentation));
            alignmentTransformer = null;
            instrumentation = null;
        }
    }
}
