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

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * @author Doug Lea
 * @since 1.5
 */
public class AtomicLong extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 1927816293512124184L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    /**
     * value字段的偏移量
     */
    private static final long valueOffset;

    static final boolean VM_SUPPORTS_LONG_CAS = VMSupportsCS8();

    /**
     * JVM是否支持long类型的无锁cas操作.
     * 仅被调用这一次. 结果会存在 VM_SUPPORTS_LONG_CAS 字段里.
     */
    private static native boolean VMSupportsCS8();

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                    (AtomicLong.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    // 值 - value
    private volatile long value;

    /**
     * 根据给定的值进行初始化.
     */
    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    /**
     * 不进行任何操作的构造器. value 取默认值 0
     */
    public AtomicLong() {
    }

    /**
     * 获取当前值value
     */
    public final long get() {
        return value;
    }

    /**
     * 设置value. 非原子操作.
     */
    public final void set(long newValue) {
        value = newValue;
    }

    /**
     * 最终将value设置为newValue. 不保证其他线程立即可见.
     * putOrderedXXX方法是putXXXVolatile方法的延迟实现，不保证值的改变被其他线程立即看到
     *
     * @since 1.6
     */
    public final void lazySet(long newValue) {
        unsafe.putOrderedLong(this, valueOffset, newValue);
    }

    /**
     * cas地进行设置. 并返回设置前的值.
     */
    public final long getAndSet(long newValue) {
        return unsafe.getAndSetLong(this, valueOffset, newValue);
    }

    /**
     * 如果原子操作时当前value等于`预期值`expect, 那么就会cas成功.
     * 如果cas成功. 那么value就会更新为update`更新值`.
     *
     * @param expect `预期值`.
     * @param update `更新值`.
     * @return 当cas成功的时候返回{@code true} . cas操作时的value值不等于expect值的时候, 返回False
     */
    public final boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    /**
     * 跟上面没区别??
     */
    public final boolean weakCompareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    /**
     * 利用cas进行自增操作.
     * 并返回自增之前的值.
     */
    public final long getAndIncrement() {
        return unsafe.getAndAddLong(this, valueOffset, 1L);
    }

    /**
     * 利用cas进行自减操作.
     * 并返回自减之前的值.
     */
    public final long getAndDecrement() {
        return unsafe.getAndAddLong(this, valueOffset, -1L);
    }

    /**
     * 利用cas将value的值设置为 value + delta
     * 并返回原先的value值
     */
    public final long getAndAdd(long delta) {
        return unsafe.getAndAddLong(this, valueOffset, delta);
    }

    /**
     * 利用cas进行自增.
     * 并返回自增之后的值.
     */
    public final long incrementAndGet() {
        return unsafe.getAndAddLong(this, valueOffset, 1L) + 1L;
    }

    /**
     * 利用cas进行自减.
     * 并返回自减之后的值.
     */
    public final long decrementAndGet() {
        return unsafe.getAndAddLong(this, valueOffset, -1L) - 1L;
    }

    /**
     * 利用cas将value的值设置为 value + delta
     * 并返回更改之后的值.
     */
    public final long addAndGet(long delta) {
        return unsafe.getAndAddLong(this, valueOffset, delta) + delta;
    }

    /**
     * 函数式编程. 原子地更改(更新)value的值.
     *
     * @return 返回更新之前的值
     * @since 1.8
     */
    public final long getAndUpdate(LongUnaryOperator updateFunction) {
        long prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 函数式编程. 原子地更改(更新)value的值.
     *
     * @return 返回更新之后的值
     * @since 1.8
     */
    public final long updateAndGet(LongUnaryOperator updateFunction) {
        long prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 两参数的函数式方法.
     * 使用例子如下:
     * AtomicLong num = new AtomicLong(20);
     * long ret = num.getAndAccumulate(300, (prev, x) -> {
     * return prev + x + 4000;
     * });
     * System.out.println(ret);
     * System.out.println(num.get());
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
        long prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        long prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 字符串形式.
     */
    public String toString() {
        return Long.toString(get());
    }

    /**
     * number接口中声明的方法. 返回值的int型式.
     * 其实就是类型强转.
     */
    public int intValue() {
        return (int) get();
    }

    /**
     * number接口中声明的方法. 实际实现就是get()
     */
    public long longValue() {
        return get();
    }

    /**
     * number接口中声明的方法. 返回值的floatg型式.
     * 其实就是类型强转.
     */
    public float floatValue() {
        return (float) get();
    }

    /**
     * number接口中声明的方法. 返回值的double型式.
     * 其实就是类型强转.
     */
    public double doubleValue() {
        return (double) get();
    }

}
