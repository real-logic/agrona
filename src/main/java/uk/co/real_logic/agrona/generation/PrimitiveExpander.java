/*
 * Copyright 2014 Real Logic Ltd.
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
package uk.co.real_logic.agrona.generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class PrimitiveExpander
{
    private static final String SOURCE_DIRECTORY = "src/main/java/";
    private static final String PACKAGE = "uk/co/real_logic/agrona/collections";
    private static final String SUFFIX = ".java";
    private static final String GENERATED_DIRECTORY = "build/generated-src";

    private static final List<Substitution> SUBSTITUTIONS = Arrays.asList(
        new Substitution("long", "Long")
    );

    public static void main(final String[] args) throws IOException
    {
        expandPrimitiveSpecialisedClass("IntIterator");
    }

    private static void expandPrimitiveSpecialisedClass(final String className) throws IOException
    {
        final Path path = Paths.get(SOURCE_DIRECTORY, PACKAGE, className + SUFFIX);
        String contents = new String(Files.readAllBytes(path), UTF_8);
        for (Substitution substitution : SUBSTITUTIONS)
        {
            contents = substitution.substitute(contents);
        }
        System.out.println(contents);
    }

    public static final class Substitution
    {
        private final String primitiveType;
        private final String boxedType;

        private Substitution(final String primitiveType, final String boxedType)
        {
            this.primitiveType = primitiveType;
            this.boxedType = boxedType;
        }

        public String substitute(final String contents)
        {
            return contents;
        }
    }
}
