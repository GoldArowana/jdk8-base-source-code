/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import sun.misc.Unsafe;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * @author Doug Lea
 * @since 1.5
 */
public class AtomicIntegerArray implements java.io.Serializable {
    private static final long serialVersionUID = 2862133569453604235L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /**
     * 返回当前数组第一个元素地址相对于数组起始地址的偏移值
     * MarkWord 4字节 + Class Pointer 4字节 + 数组长度4字节 + 对齐4字节 = 16 字节
     */
    private static final int base = unsafe.arrayBaseOffset(int[].class);

    private static final int shift;
    private final int[] array;

    static {
        // arrayIndexScale 返回当前数组一个元素占用的字节数
        int scale = unsafe.arrayIndexScale(int[].class);
        if ((scale & (scale - 1)) != 0) throw new Error("data type scale not a power of two");
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    }

    /**
     * @param i 索引(下角标)
     * @return 返回相应的`偏移量`
     */
    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length) throw new IndexOutOfBoundsException("index " + i);

        return byteOffset(i);
    }

    /**
     * @param i 索引(下角标)
     * @return 返回相应的`偏移量`
     */
    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    /**
     * 初始化一个length大小的数组
     */
    public AtomicIntegerArray(int length) {
        array = new int[length];
    }

    /**
     * 根据给定的array进行拷贝.
     * clone之后, 不仅数组大小相等, 而且含有相同的元素.
     */
    public AtomicIntegerArray(int[] array) {
        this.array = array.clone();
    }

    /**
     * 获取数组的长度
     */
    public final int length() {
        return array.length;
    }

    /**
     * 获取数组中特定索引的值.
     */
    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    /**
     * 根据偏移量来获取数中具体索引(下角标)处的值.
     */
    private int getRaw(long offset) {
        return unsafe.getIntVolatile(array, offset);
    }

    /**
     * 原子地更新.
     *
     * @param i        the index        索引(下角标)
     * @param newValue the new value
     */
    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
    }

    /**
     * 最终将数组索引i处的value设置为newValue. 不保证其他线程立即可见.
     * putOrderedXXX方法是putXXXVolatile方法的延迟实现，不保证值的改变被其他线程立即看到
     *
     * @param i        the index        索引(下角标)
     * @param newValue the new value    新值
     * @since 1.6
     */
    public final void lazySet(int i, int newValue) {
        unsafe.putOrderedInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * 原子地设置数组`i`处的值为`newValue`
     *
     * @param i        the index            索引(下角标)
     * @param newValue the new value        新值
     * @return the previous value           返回之前的值
     */
    public final int getAndSet(int i, int newValue) {
        return unsafe.getAndSetInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * cas更改数组中的某一索引处的值
     *
     * @param i      the index              索引(下角标)
     * @param expect the expected value     cas的`预期值`
     * @param update the new value          cas的`更新值`
     * @return cas成功就返回true. 当预期值不等于value的当前值的时候, 就会cas失败, 返回false.
     */
    public final boolean compareAndSet(int i, int expect, int update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    /**
     * @param offset 偏移量
     * @param expect 预期值
     * @param update 更新值
     * @return
     */
    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(array, offset, expect, update);
    }

    /**
     * 其实就是compareAndSet方法
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     */
    public final boolean weakCompareAndSet(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    /**
     * 原子地自增
     *
     * @param i the index           索引(下角标)
     * @return the previous value   返回自增之前的值
     */
    public final int getAndIncrement(int i) {
        return getAndAdd(i, 1);
    }

    /**
     * 原子地自减
     *
     * @param i the index
     * @return the previous value   返回自减之前的值
     */
    public final int getAndDecrement(int i) {
        return getAndAdd(i, -1);
    }

    /**
     * 将索引i处的值设置为 value(旧值) + delta
     *
     * @param i     the index
     * @param delta the value to add
     * @return the previous value
     */
    public final int getAndAdd(int i, int delta) {
        return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
    }

    /**
     * 原子地自增
     *
     * @param i the index           索引(下角标)
     * @return the updated value    返回自增之前的值
     */
    public final int incrementAndGet(int i) {
        return getAndAdd(i, 1) + 1;
    }

    /**
     * 原子地自减.
     *
     * @param i the index           索引(下角标)
     * @return the updated value    返回自增之后的值
     */
    public final int decrementAndGet(int i) {
        return getAndAdd(i, -1) - 1;
    }

    /**
     * 原子地将索引i处的值设置为 value(旧值) + delta
     *
     * @param i     the index
     * @param delta the value to add
     * @return the updated value
     */
    public final int addAndGet(int i, int delta) {
        return getAndAdd(i, delta) + delta;
    }


    /**
     * 函数式编程, 原子地更新
     *
     * @param i              the index      索引(下角标)
     * @param updateFunction a side-effect-free function
     * @return the updated value            返回更新前的值
     * @since 1.8
     */
    public final int getAndUpdate(int i, IntUnaryOperator updateFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSetRaw(offset, prev, next));
        return prev;
    }

    /**
     * 函数式编程, 原子地更新
     *
     * @param i              the index      索引(下角标)
     * @param updateFunction a side-effect-free function
     * @return the updated value            返回更新后的值
     * @since 1.8
     */
    public final int updateAndGet(int i, IntUnaryOperator updateFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSetRaw(offset, prev, next));
        return next;
    }

    /**
     * 函数式编程
     *
     * @param i                   the index         索引(数组下角标)
     * @param x                   the update value  将索引位置的值更新为x
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value    返回更新之前的值
     * @since 1.8
     */
    public final int getAndAccumulate(int i, int x, IntBinaryOperator accumulatorFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSetRaw(offset, prev, next));
        return prev;
    }

    /**
     * Atomically updates the element at index {@code i} with the
     * results of applying the given function to the current and
     * given values, returning the updated value. The function should
     * be side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value at index {@code i} as its first
     * argument, and the given update as the second argument.
     *
     * @param i                   the index         索引(数组下角标)
     * @param x                   the update value  将索引位置的值更新为x
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value    返回更新之后的值
     * @since 1.8
     */
    public final int accumulateAndGet(int i, int x, IntBinaryOperator accumulatorFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSetRaw(offset, prev, next));
        return next;
    }

    /**
     * String形式
     */
    public String toString() {
        int iMax = array.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(getRaw(byteOffset(i)));
            if (i == iMax)
                return b.append(']').toString();
            b.append(',').append(' ');
        }
    }

}
