/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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
package org.agrona.generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public final class PrimitiveExpander
{
    private static final String SOURCE_DIRECTORY = "src/main/java/";
    private static final String COLLECTIONS = "org/agrona/collections";
    private static final String SUFFIX = ".java";
    private static final String GENERATED_DIRECTORY = "build/generated-src";

    private static final List<Substitution> SUBSTITUTIONS = Collections.singletonList(
        new Substitution("long", "Long", "Long"));

    public static void main(final String[] args) throws IOException
    {
        expandPrimitiveSpecialisedClass(COLLECTIONS, "IntIterator");
        expandPrimitiveSpecialisedClass(COLLECTIONS, "Int2IntHashMap");
        expandPrimitiveSpecialisedClass(COLLECTIONS, "IntHashSet");
        expandPrimitiveSpecialisedClass(COLLECTIONS, "IntLruCache");
        expandPrimitiveSpecialisedClass(COLLECTIONS, "IntIntConsumer");
        expandPrimitiveSpecialisedClass(COLLECTIONS, "Int2ObjectCache");
        expandPrimitiveSpecialisedClass(COLLECTIONS, "Int2ObjectHashMap");
    }

    private static void expandPrimitiveSpecialisedClass(final String packageName, final String className)
        throws IOException
    {
        final Path inputPath = Paths.get(SOURCE_DIRECTORY, packageName, className + SUFFIX);
        final Path outputDirectory = Paths.get(GENERATED_DIRECTORY, packageName);
        Files.createDirectories(outputDirectory);

        final List<String> contents = Files.readAllLines(inputPath, UTF_8);
        for (final Substitution substitution : SUBSTITUTIONS)
        {
            final String substitutedFileName = substitution.substitute(className);
            final List<String> substitutedContents = contents
                .stream()
                .map(substitution::checkedSubstitute)
                .collect(toList());

            final Path outputPath = Paths.get(GENERATED_DIRECTORY, packageName, substitutedFileName + SUFFIX);
            Files.write(outputPath, substitutedContents, UTF_8);
            System.out.println("Generated " + substitutedFileName);
        }
    }

    public static final class Substitution
    {
        private final String primitiveType;
        private final String boxedType;
        private final String className;

        private Substitution(final String primitiveType, final String boxedType, final String className)
        {
            this.primitiveType = primitiveType;
            this.boxedType = boxedType;
            this.className = className;
        }

        public String substitute(final String contents)
        {
            return contents
                .replace("int", primitiveType)
                .replace("Integer", boxedType)
                .replace("Int", className);
        }

        public String checkedSubstitute(final String contents)
        {
            return contents.contains("@DoNotSub") || contents.contains("interface")  || contents.contains("Interface")
                 ? contents
                 : substitute(contents);
        }
    }
}
