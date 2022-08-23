/*
 * Copyright 2014-2022 Real Logic Limited.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * Specialise classes written for primitive type int for other primitive types by substitution.
 */
public final class SpecialisationGenerator
{
    private static final String COLLECTIONS_PACKAGE = "org/agrona/collections";
    private static final String SRC_DIR = "src/main/java/";
    private static final String DST_DIR = "build/generated-src";
    private static final String SUFFIX = ".java";

    private static final List<Substitution> SUBSTITUTIONS = Collections.singletonList(
        new Substitution("long", "Long", "Long"));

    /**
     * Main method.
     *
     * @param args command line args.
     * @throws IOException in case of I/O error.
     */
    public static void main(final String[] args) throws IOException
    {
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntIntConsumer", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntIntFunction", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntObjConsumer", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntObjectToObjectFunction", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "ObjectIntToIntFunction", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntArrayList", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntArrayQueue", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2IntHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2IntCounterMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntHashSet", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntLruCache", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2ObjectCache", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2ObjectHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2NullableObjectHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Object2IntHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Object2IntCounterMap", SRC_DIR, DST_DIR);
    }

    /**
     * Specialise a class replacing int types based on {@link Substitution}s.
     *
     * @param substitutions to be applied.
     * @param packageName   for the source and destination classes.
     * @param srcClassName  to be specialised.
     * @param srcDirName    containing the source file.
     * @param dstDirName    for where the generated file should be stored.
     * @throws IOException if an error occurs.
     */
    public static void specialise(
        final List<Substitution> substitutions,
        final String packageName,
        final String srcClassName,
        final String srcDirName,
        final String dstDirName)
        throws IOException
    {
        final Path inputPath = Paths.get(srcDirName, packageName, srcClassName + SUFFIX);
        final Path outputDirectory = Paths.get(dstDirName, packageName);
        Files.createDirectories(outputDirectory);

        final List<String> contents = Files.readAllLines(inputPath, UTF_8);
        for (final Substitution substitution : substitutions)
        {
            final String substitutedFileName = substitution.substitute(srcClassName);
            final List<String> substitutedContents = contents
                .stream()
                .map(substitution::conditionalSubstitute)
                .collect(toList());

            final Path outputPath = Paths.get(dstDirName, packageName, substitutedFileName + SUFFIX);

            Files.write(outputPath, substitutedContents, UTF_8);
        }
    }

    /**
     * Substitution to be performed on each code line. Lines with {@link DoNotSub} are ignored.
     */
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

        /**
         * Perform code substitutions.
         *
         * @param contents original source code.
         * @return modified source code.
         */
        public String substitute(final String contents)
        {
            return contents
                .replace("int", primitiveType)
                .replace("Integer", boxedType)
                .replace("Int", className);
        }

        /**
         * Perform conditional code substitutions, i.e. only if not disabled.
         *
         * @param contents original source code.
         * @return modified source code.
         */
        public String conditionalSubstitute(final String contents)
        {
            return
                (contents.contains("@DoNotSub") || contents.contains("interface") || contents.contains("Interface")) ?
                    contents : substitute(contents);
        }
    }
}
