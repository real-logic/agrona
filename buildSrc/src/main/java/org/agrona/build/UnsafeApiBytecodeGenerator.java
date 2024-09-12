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
package org.agrona.build;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.jar.asm.MethodVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This plugin generates bytecode for {@code org.agrona.UnsafeApi} class.
 */
public final class UnsafeApiBytecodeGenerator implements Plugin
{
    private static final Class<?> UNSAFE_CLASS;

    static
    {
        final String unsafeTypeName = "jdk.internal.misc.Unsafe";
        try
        {
            UNSAFE_CLASS = Class.forName(unsafeTypeName);
        }
        catch (final ClassNotFoundException e)
        {
            throw new Error("Failed to resolve: " + unsafeTypeName, e);
        }
    }

    enum GetUnsafeMethodByteCode implements ByteCodeAppender
    {
        INSTANCE;

        @Override
        public @NotNull Size apply(
            final @NotNull MethodVisitor methodVisitor,
            final @NotNull Implementation.Context implementationContext,
            final @NotNull MethodDescription instrumentedMethod)
        {
            final TypeDescription.ForLoadedType classType = new TypeDescription.ForLoadedType(Class.class);
            final TypeDescription.ForLoadedType fieldType = new TypeDescription.ForLoadedType(Field.class);

            final MethodDescription.InDefinedShape classForName = classType.getDeclaredMethods()
                .filter(hasSignature(new MethodDescription.SignatureToken(
                "forName",
                    new TypeDescription.ForLoadedType(Class.class),
                    new TypeDescription.ForLoadedType(String.class))))
                .getOnly();
            final MethodDescription.InDefinedShape classGetDeclaredField = classType.getDeclaredMethods()
                .filter(named("getDeclaredField"))
                .getOnly();
            final MethodDescription.InDefinedShape fieldSetAccessible = fieldType.getDeclaredMethods()
                .filter(named("setAccessible"))
                .getOnly();
            final MethodDescription.InDefinedShape fieldGet =
                fieldType.getDeclaredMethods().filter(named("get")).getOnly();
            final StackManipulation.Size operandStackSize = new StackManipulation.Compound(
                new TextConstant(UNSAFE_CLASS.getName()),
                MethodInvocation.invoke(classForName),
                new TextConstant("theUnsafe"),
                MethodInvocation.invoke(classGetDeclaredField),
                MethodVariableAccess.REFERENCE.storeAt(0),
                MethodVariableAccess.REFERENCE.loadFrom(0),
                IntegerConstant.forValue(1),
                MethodInvocation.invoke(fieldSetAccessible),
                MethodVariableAccess.REFERENCE.loadFrom(0),
                NullConstant.INSTANCE,
                MethodInvocation.invoke(fieldGet),
                TypeCasting.to(new TypeDescription.ForLoadedType(UNSAFE_CLASS)),
                MethodReturn.REFERENCE
            ).apply(methodVisitor, implementationContext);

            return new Size(operandStackSize.getMaximalSize(),
                instrumentedMethod.getStackSize() + 1 /* local variable */);
        }
    }

    enum GetUnsafeImplementation implements Implementation
    {

        INSTANCE;

        @Override
        public @NotNull InstrumentedType prepare(final @NotNull InstrumentedType instrumentedType)
        {
            return instrumentedType;
        }

        @Override
        public @NotNull ByteCodeAppender appender(final @NotNull Target implementationTarget)
        {
            return GetUnsafeMethodByteCode.INSTANCE;
        }
    }

    /**
     * {@inheritDoc}
     */
    public @NotNull DynamicType.Builder<?> apply(
        final DynamicType.Builder<?> builder,
        final @NotNull TypeDescription typeDescription,
        final @NotNull ClassFileLocator classFileLocator)
    {
        final String unsafeAccessor = "getUnsafe";
        final String unsafeFieldName = "UNSAFE";
        DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> newBuilder = builder
            .modifiers(Visibility.PUBLIC, TypeManifestation.FINAL)
            .defineField(unsafeFieldName, UNSAFE_CLASS, Ownership.STATIC, Visibility.PRIVATE, FieldManifestation.FINAL)
            .defineMethod(unsafeAccessor, UNSAFE_CLASS, Ownership.STATIC, Visibility.PRIVATE)
            .intercept(GetUnsafeImplementation.INSTANCE);

        newBuilder = newBuilder
            .invokable(isTypeInitializer())
            .intercept(MethodCall.invoke(named(unsafeAccessor))
                .setsField(new FieldDescription.Latent(
                    newBuilder.toTypeDescription(),
                    unsafeFieldName,
                    ModifierContributor.Resolver.of(Ownership.STATIC, Visibility.PRIVATE, FieldManifestation.FINAL)
                        .resolve(),
                    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(UNSAFE_CLASS),
                    List.of()))
                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));

        final TypeDescription.ForLoadedType unsafeType = new TypeDescription.ForLoadedType(UNSAFE_CLASS);
        final MethodList<MethodDescription.InDefinedShape> staticMethods = unsafeType.getDeclaredMethods()
            .filter(isPublic().and(not(isDeclaredBy(Object.class))));
        for (final MethodDescription.InDefinedShape method : staticMethods)
        {
            // Redefine existing method
            newBuilder = newBuilder
                .method(named(method.getName()))
                .intercept(method.isStatic() ? MethodDelegation.to(unsafeType) :
                    MethodDelegation.withDefaultConfiguration().filter(named(method.getName()))
                        .toField(unsafeFieldName));
        }

        return newBuilder;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException
    {
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(final TypeDescription target)
    {
        return "org.agrona.UnsafeApi".equals(target.getName());
    }
}
