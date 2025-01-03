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
package org.agrona.generation;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * A {@link SimpleJavaFileObject} that is used to store the bytes for a java class in memory.
 */
public class JavaClassObject extends SimpleJavaFileObject
{
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    /**
     * Create an instance for a given class name.
     *
     * @param className name of the class.
     * @param kind      kind of the class.
     */
    public JavaClassObject(final String className, final Kind kind)
    {
        super(URI.create("string:///" + className.replace('.', '/') + kind.extension), kind);
    }

    /**
     * Get the raw bytes for a class file.
     *
     * @return the raw bytes for a class file.
     */
    public byte[] getBytes()
    {
        return baos.toByteArray();
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream openOutputStream()
    {
        return baos;
    }

    /**
     * {@inheritDoc}
     */
    public Kind getKind()
    {
        return Kind.CLASS;
    }
}
