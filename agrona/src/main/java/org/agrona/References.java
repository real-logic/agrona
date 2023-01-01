/*
 * Copyright 2014-2023 Real Logic Limited.
 * Copyright 2018 Gil Tene
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
package org.agrona;

import java.lang.ref.Reference;

/**
 * References provides two key helper methods for commonly used idioms on ref types:
 * {@link #isCleared(Reference)}, and {@link #isReferringTo(Reference, Object)}.
 * <p>
 * Both of these idioms are obviously trivially implementable using {@link Reference#get()}.
 * However, such explicit implementations produce a temporary strong reference to
 * the referent, forcing a "strengthening" of the referent when performed during
 * concurrent marking phases on most concurrent collectors (such as G1, C4, ZGC,
 * Shenandoah, etc.). By capturing the non-reference-escaping semantic definitions
 * of these idioms, JDKs may validly intrisify their implementations to perform
 * the required logic without strengthening the referent.
 * <p>
 * Various uses of {@link Reference} subclasses can use these method idioms when
 * performing the common operations needed to maintain various data structures such
 * and weakly keyed maps, lists, etc., without those operations explicitly
 * force-strengthening referents. When run on JDKs that intrisify these implementations,
 * or on future JDKs that would provide similar functionality (and to which this class
 * will adapt in a portable way), such strengthening will be avoided.
 * <p>
 * For example, the JDK's own internal {@code WeakIdentityHashMap} implementation could,
 * when using these two methods, have its get() and set() methods work without requiring
 * the force-strengthening of unrelated keys and without forcing unrelated entries and
 * values to stay alive.
 * <p>
 * Since JDKs may choose to "intrinsify" the implementation of the methods in
 * this class, no assumptions, beyond those provided in the documentation below,
 * should be made about their actual implementation.
 */
public final class References
{
    private References()
    {
    }

    /**
     * Indicate whether a {@link Reference} has been cleared.
     *
     * @param ref The {@link Reference} to be tested.
     * @return true if {@link Reference} is cleared, otherwise false.
     */
    public static boolean isCleared(final Reference<?> ref)
    {
        return ref.get() == null;
    }

    /**
     * Indicate whether a Reference refers to a given object.
     *
     * @param ref The {@link Reference} to be tested.
     * @param obj The object to which the {@link Reference} may, or may not, be referring.
     * @param <T> the class of the referent.
     * @return true if the {@link Reference}'s referent is obj, otherwise false.
     */
    public static <T> boolean isReferringTo(final Reference<T> ref, final T obj)
    {
        return ref.get() == obj;
    }
}
