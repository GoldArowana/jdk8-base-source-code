/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

/**
 * The abstract class {@code Number} is the superclass of platform
 * classes representing numeric values that are convertible to the
 * primitive types {@code byte}, {@code double}, {@code float}, {@code
 * int}, {@code long}, and {@code short}.
 * <p>
 * The specific semantics of the conversion from the numeric value of
 * a particular {@code Number} implementation to a given primitive
 * type is defined by the {@code Number} implementation in question.
 * <p>
 * For platform classes, the conversion is often analogous to a
 * narrowing primitive conversion or a widening primitive conversion
 * as defining in <cite>The Java&trade; Language Specification</cite>
 * for converting between primitive types.  Therefore, conversions may
 * lose information about the overall magnitude of a numeric value, may
 * lose precision, and may even return a result of a different sign
 * than the input.
 * <p>
 * See the documentation of a given {@code Number} implementation for
 * conversion details.
 *
 * @author Lee Boynton
 * @author Arthur van Hoff
 * @jls 5.1.2 Widening Primitive Conversions
 * @jls 5.1.3 Narrowing Primitive Conversions
 * @since JDK1.0
 */
public abstract class Number implements java.io.Serializable {
    /**
     * 返回值的int形式
     */
    public abstract int intValue();

    /**
     * 返回值的long形式
     */
    public abstract long longValue();

    /**
     * 返回值的float形式
     */
    public abstract float floatValue();

    /**
     * 返回值的double形式
     */
    public abstract double doubleValue();

    /**
     * 返回值的byte形式.
     * 实现方式是 把intValue() 强制转化为byte值.
     *
     * @since JDK1.1
     */
    public byte byteValue() {
        return (byte) intValue();
    }

    /**
     * 返回值的short形式.
     * 现方式是 把intValue() 强制转化为short值.
     *
     * @since JDK1.1
     */
    public short shortValue() {
        return (short) intValue();
    }

    private static final long serialVersionUID = -8742448824652078965L;
}
