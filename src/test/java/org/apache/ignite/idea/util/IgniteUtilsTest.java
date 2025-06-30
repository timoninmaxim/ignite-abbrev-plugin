/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.idea.util;

import java.util.Comparator;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** */
public class IgniteUtilsTest {
    /** */
    @Test
    public void testCamelCaseParts() {
        assertEquals(List.of("foo", "Bar", "Baz"), IgniteUtils.camelCaseParts("fooBarBaz"));
        assertEquals(List.of("Foo", "Bar", "Baz"), IgniteUtils.camelCaseParts("FooBarBaz"));

        assertEquals(List.of("FOO", "Bar", "Baz"), IgniteUtils.camelCaseParts("FOOBarBaz"));
        assertEquals(List.of("foo", "BAR", "Baz"), IgniteUtils.camelCaseParts("fooBARBaz"));
        assertEquals(List.of("foo", "Bar", "BAZ"), IgniteUtils.camelCaseParts("fooBarBAZ"));

        assertEquals(List.of("Foo", "Bar", "Baz", "42"), IgniteUtils.camelCaseParts("FooBarBaz42"));
        assertEquals(List.of("Foo", "Bar", "BAZ", "42"), IgniteUtils.camelCaseParts("FooBarBAZ42"));
        assertEquals(List.of("A", "42"), IgniteUtils.camelCaseParts("A42"));

        assertEquals(List.of("foo", "Bar", "#", "#", "Baz"), IgniteUtils.camelCaseParts("fooBar##Baz"));
        assertEquals(List.of("#", "foo", "Bar", "Baz"), IgniteUtils.camelCaseParts("#fooBarBaz"));
        assertEquals(List.of("42", "!"), IgniteUtils.camelCaseParts("42!"));
        assertEquals(List.of("A", "_"), IgniteUtils.camelCaseParts("A_"));
    }

    /** */
    @Test
    public void testUnCapitalizeFirst() {
        assertEquals("fooBar", IgniteUtils.unCapitalizeFirst("FooBar"));
        assertEquals("fOO", IgniteUtils.unCapitalizeFirst("FOO"));
        assertEquals("a", IgniteUtils.unCapitalizeFirst("A"));
    }

    /** */
    @Test
    public void testCapitalizeFirst() {
        assertEquals("FooBar", IgniteUtils.capitalizeFirst("fooBar"));
        assertEquals("Foo", IgniteUtils.capitalizeFirst("foo"));
        assertEquals("A", IgniteUtils.capitalizeFirst("a"));
    }

    /** */
    @Test
    public void testTransformCamelCase() {
        assertEquals("foo0Bar1Baz2", IgniteUtils.transformCamelCase("fooBarBaz",
            (in1, in2) -> in1 + in2));

        assertEquals("FooBarBaz", IgniteUtils.transformCamelCase("fooBarBaz",
            (in1, in2) -> in2 == 0 ? IgniteUtils.capitalizeFirst(in1) : in1));
    }

    /** */
    @Test
    public void testMin() {
        assertNull(IgniteUtils.min(Integer::compare));

        assertEquals(Integer.valueOf(1), IgniteUtils.min(Integer::compare,1, 3, 5, 4, 2));
        assertEquals("A", IgniteUtils.min(Comparator.naturalOrder(),"B", "A", "C", null));
    }
}
