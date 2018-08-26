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
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 这是一个抽象类. 具体实现是内部类 AtomicIntegerFieldUpdaterImpl
 * 想要原子地更新某一个字段里的 volatile + int 类型的变量时, 可以使用updater来达到这个目的.
 *
 * @author Doug Lea
 * @since 1.5
 */
public abstract class AtomicIntegerFieldUpdater<T> {
    /**
     * 指定一个类tclass, 和类里的字段fieldName.
     * 这个字段必须是Integer类型的, 而且还是volatile修饰的.
     *
     * @param tclass    the class of the objects holding the field
     * @param fieldName the name of the field to be updated
     * @param <U>       the type of instances of tclass
     * @return the updater
     */
    @CallerSensitive
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return new AtomicIntegerFieldUpdaterImpl<U>(tclass, fieldName, Reflection.getCallerClass());
    }

    /**
     * 构造器里是空实现.
     */
    protected AtomicIntegerFieldUpdater() {
    }

    /**
     * cas操作. 交给子类实现.
     */
    public abstract boolean compareAndSet(T obj, int expect, int update);

    public abstract boolean weakCompareAndSet(T obj, int expect, int update);

    /**
     * 底层使用putIntVolatile来进行原子地设置值
     */
    public abstract void set(T obj, int newValue);

    /**
     * 最终将value设置为newValue. 不保证其他线程立即可见.
     * putOrderedXXX方法是putXXXVolatile方法的延迟实现，不保证值的改变被其他线程立即看到
     *
     * @since 1.6
     */
    public abstract void lazySet(T obj, int newValue);

    /**
     * 获取值.
     */
    public abstract int get(T obj);

    /**
     * 原子地进行设置值.
     * 并返回修改之前的值
     */
    public int getAndSet(T obj, int newValue) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    /**
     * 自增.
     *
     * @return the previous value   返回自增前的值.
     */
    public int getAndIncrement(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 自减
     *
     * @return the previous value   返回自减之前的值.
     */
    public int getAndDecrement(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 原子地进行修改值. 将值改为 value(旧值) + delta
     *
     * @param obj   An object whose field to get and set
     * @param delta the value to add
     * @return the previous value   返回修改之前的值
     */
    public int getAndAdd(T obj, int delta) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * cas自增.
     *
     * @param obj An object whose field to get and set
     * @return the updated value        返回自增之后的值.
     */
    public int incrementAndGet(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * cas自减.
     *
     * @param obj An object whose field to get and set
     * @return the updated value        返回自减之后的值.
     */
    public int decrementAndGet(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * 原子地进行修改值. 将值改为 value(旧值) + delta
     *
     * @param obj   An object whose field to get and set
     * @param delta the value to add
     * @return the previous value   返回修改之后的值
     */
    public int addAndGet(T obj, int delta) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * 函数式编程. 原子地更改(更新)value的值.
     *
     * @param obj            An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the previous value   返回修改之前的值
     * @since 1.8
     */
    public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 函数式编程. 原子地更改(更新)value的值.
     *
     * @param obj            An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the updated value    返回修改之后的值.
     * @since 1.8
     */
    public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * 两参数的函数式方法.原子操作.
     *
     * @param obj                 An object whose field to get and set
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final int getAndAccumulate(T obj, int x, IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this
     * updater with the results of applying the given function to the
     * current and given values, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.  The
     * function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param obj                 An object whose field to get and set
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final int accumulateAndGet(T obj, int x, IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * updater的具体实现.
     */
    private static class AtomicIntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final long offset;
        private final Class<T> tclass;
        private final Class<?> cclass;

        AtomicIntegerFieldUpdaterImpl(final Class<T> tclass, final String fieldName, final Class<?> caller) {
            final Field field;
            final int modifiers;
            try {
                field = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Field>() {
                            public Field run() throws NoSuchFieldException {
                                return tclass.getDeclaredField(fieldName);
                            }
                        });
                modifiers = field.getModifiers();
                sun.reflect.misc.ReflectUtil.ensureMemberAccess(caller, tclass, null, modifiers);
                ClassLoader cl = tclass.getClassLoader();
                ClassLoader ccl = caller.getClassLoader();
                if ((ccl != null) && (ccl != cl) && ((cl == null) || !isAncestor(cl, ccl))) {
                    sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
                }
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            Class<?> fieldt = field.getType();

            // 必须是int类型
            if (fieldt != int.class) throw new IllegalArgumentException("Must be integer type");

            // 必须是volatile修饰的
            if (!Modifier.isVolatile(modifiers)) throw new IllegalArgumentException("Must be volatile type");

            this.cclass = (Modifier.isProtected(modifiers) && caller != tclass) ? caller : null;
            this.tclass = tclass;
            offset = unsafe.objectFieldOffset(field);
        }

        /**
         * Returns true if the second classloader can be found in the first
         * classloader's delegation chain.
         * Equivalent to the inaccessible: first.isAncestor(second).
         */
        private static boolean isAncestor(ClassLoader first, ClassLoader second) {
            ClassLoader acl = first;
            do {
                acl = acl.getParent();
                if (second == acl) {
                    return true;
                }
            } while (acl != null);
            return false;
        }

        private void fullCheck(T obj) {
            if (!tclass.isInstance(obj)) throw new ClassCastException();
            if (cclass != null) ensureProtectedAccess(obj);
        }

        public boolean compareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.compareAndSwapInt(obj, offset, expect, update);
        }

        public boolean weakCompareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.compareAndSwapInt(obj, offset, expect, update);
        }

        public void set(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            unsafe.putIntVolatile(obj, offset, newValue);
        }

        public void lazySet(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            unsafe.putOrderedInt(obj, offset, newValue);
        }

        public final int get(T obj) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.getIntVolatile(obj, offset);
        }

        public int getAndSet(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.getAndSetInt(obj, offset, newValue);
        }

        public int getAndIncrement(T obj) {
            return getAndAdd(obj, 1);
        }

        public int getAndDecrement(T obj) {
            return getAndAdd(obj, -1);
        }

        public int getAndAdd(T obj, int delta) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.getAndAddInt(obj, offset, delta);
        }

        public int incrementAndGet(T obj) {
            return getAndAdd(obj, 1) + 1;
        }

        public int decrementAndGet(T obj) {
            return getAndAdd(obj, -1) - 1;
        }

        public int addAndGet(T obj, int delta) {
            return getAndAdd(obj, delta) + delta;
        }

        private void ensureProtectedAccess(T obj) {
            if (cclass.isInstance(obj)) {
                return;
            }
            throw new RuntimeException(
                    new IllegalAccessException("Class " +
                            cclass.getName() +
                            " can not access a protected member of class " +
                            tclass.getName() +
                            " using an instance of " +
                            obj.getClass().getName()
                    )
            );
        }
    }
}
