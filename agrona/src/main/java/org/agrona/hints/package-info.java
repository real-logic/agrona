/*
 * Copyright 2016 Gil Tene
 * Copyright 2014-2020 Real Logic Limited.
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

/**
 * <p>
 * This package captures hints that may be used by some
 * runtimes to improve code performance. It is intended to provide a portable
 * means for using performance hinting APIs across Java and JDK versions,
 * such that calling code can avoid maintaining version-specific sources
 * for various JDK or Java version capabilities.
 * </p>
 * <p>
 * All the features supported by this package are (by definition) hints.
 * A no-op implementation of a hint is always considered valid.
 * </p>
 * <p>
 * When executing on Java versions for which corresponding capabilities exist
 * and are specified in the Java version spec, the hint implementations in this
 * package will attempt to use the appropriate JDK calls.
 * </p>
 * <p>
 * Some JDKs may choose to "intrinsify" some APIs in this package to e.g.
 * provide runtime support for certain hints on java versions that do not
 * yet have specified support for those capabilities.
 * </p>
 * <p>
 * A good example of the purpose of this package and an example of how it may
 * be used can be found with
 * {@link org.agrona.hints.ThreadHints#onSpinWait ThreadHints.onSpinWait()}:
 * Per JEP285, it is anticipated that Java SE 9 will include a <code>Thread.onSpinWait()</code>
 * method with specified behavior identical to
 * {@link org.agrona.hints.ThreadHints#onSpinWait ThreadHints.onSpinWait()}.
 * However, earlier Java SE versions do not include this behavior, forcing code
 * that wants to make use of this hinting capability to be written and
 * maintained separately when targeting Java versions before and after Java SE 9.
 * </p>
 * <p>
 * The implementation
 * of {@link org.agrona.hints.ThreadHints#onSpinWait ThreadHints.onSpinWait()}
 * resolves this problem by calling <code>Thread.onSpinWait()</code> if it exists,
 * and doing nothing if it does not. This allows code that wants to include spin wait
 * hinting to remain portable across Java versions, without needing separate
 * implementations for versions prior to and following Java SE 9. The mechanism
 * used to conditionally make this call was specifically designed and tested for
 * efficient inlining and by common JVMs, such that there is no extra no overhead
 * associated with making the hint call: in JIT'ted code it becomes either an nop
 * or an efficient inlined intrinsic. The implementation can be disabled by setting
 * the {@link org.agrona.hints.ThreadHints#DISABLE_ON_SPIN_WAIT_PROP_NAME} system
 * property to <code>true</code>.
 * </p>
 * <p>
 * In addition, JDKs that wish to introduce support for newer hinting capabilities
 * in their implementations of older Java SE versions can do so by "inrinsifying"
 * associated org.agrona.hints classes and methods. Code that makes use of
 * org.agrona.hints hinting methods will then benefit from potential
 * performance improvements even on prior java SE versions. E.g. the
 * example onSpinWait capability discussed above can be added in such a way to
 * Java SE 6, 7, and 8 by JDKs who wish to do so.
 */
package org.agrona.hints;