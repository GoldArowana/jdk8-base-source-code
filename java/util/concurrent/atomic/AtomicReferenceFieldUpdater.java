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
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * @param <T> The type of the object holding the updatable field
 * @param <V> The type of the field
 * @author Doug Lea
 * @since 1.5
 */
public abstract class AtomicReferenceFieldUpdater<T, V> {

    /**
     * 实例化一个updater, 并返回
     *
     * @param tclass    the class of the objects holding the field
     * @param vclass    the class of the field
     * @param fieldName the name of the field to be updated
     * @param <U>       the type of instances of tclass
     * @param <W>       the type of instances of vclass
     * @return the updater
     */
    @CallerSensitive
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass,
                                                                      Class<W> vclass,
                                                                      String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl<U, W>(tclass, vclass, fieldName, Reflection.getCallerClass());
    }

    /**
     * 默认构造器里是空实现
     */
    protected AtomicReferenceFieldUpdater() {
    }

    /**
     * cas
     */
    public abstract boolean compareAndSet(T obj, V expect, V update);

    /**
     * cas
     */
    public abstract boolean weakCompareAndSet(T obj, V expect, V update);

    /**
     * 可以看做是volatile赋值
     */
    public abstract void set(T obj, V newValue);

    /**
     * 最终将value设置为newValue. 不保证其他线程立即可见.
     * putOrderedXXX方法是putXXXVolatile方法的延迟实现，不保证值的改变被其他线程立即看到
     *
     * @param obj      An object whose field to set
     * @param newValue the new value
     * @since 1.6
     */
    public abstract void lazySet(T obj, V newValue);

    /**
     * 获取当前的值
     *
     * @param obj An object whose field to get      obj是updater将要操作的对象
     * @return the current value                    返回obj的当前值
     */
    public abstract V get(T obj);

    /**
     * 原子地设置.
     *
     * @param obj      An object whose field to get and set
     * @param newValue the new value
     * @return the previous value       返回设置之前的值
     */
    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this updater
     * with the results of applying the given function, returning the previous
     * value. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     *
     * @param obj            An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final V getAndUpdate(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this updater
     * with the results of applying the given function, returning the updated
     * value. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     *
     * @param obj            An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final V updateAndGet(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically updates the field of the given object managed by this
     * updater with the results of applying the given function to the
     * current and given values, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.  The
     * function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param obj                 An object whose field to get and set
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final V getAndAccumulate(T obj, V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
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
    public final V accumulateAndGet(T obj, V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    private static final class AtomicReferenceFieldUpdaterImpl<T, V>
            extends AtomicReferenceFieldUpdater<T, V> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final long offset;
        private final Class<T> tclass;
        private final Class<V> vclass;
        private final Class<?> cclass;

        /*
         * Internal type checks within all update methods contain
         * internal inlined optimizations checking for the common
         * cases where the class is final (in which case a simple
         * getClass comparison suffices) or is of type Object (in
         * which case no check is needed because all objects are
         * instances of Object). The Object case is handled simply by
         * setting vclass to null in constructor.  The targetCheck and
         * updateCheck methods are invoked when these faster
         * screenings fail.
         */

        AtomicReferenceFieldUpdaterImpl(final Class<T> tclass,
                                        final Class<V> vclass,
                                        final String fieldName,
                                        final Class<?> caller) {
            final Field field;
            final Class<?> fieldClass;
            final int modifiers;
            try {
                field = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Field>() {
                            public Field run() throws NoSuchFieldException {
                                return tclass.getDeclaredField(fieldName);
                            }
                        });
                modifiers = field.getModifiers();
                sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                        caller, tclass, null, modifiers);
                ClassLoader cl = tclass.getClassLoader();
                ClassLoader ccl = caller.getClassLoader();
                if ((ccl != null) && (ccl != cl) &&
                        ((cl == null) || !isAncestor(cl, ccl))) {
                    sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
                }
                fieldClass = field.getType();
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            if (vclass != fieldClass)
                throw new ClassCastException();

            if (!Modifier.isVolatile(modifiers))
                throw new IllegalArgumentException("Must be volatile type");

            this.cclass = (Modifier.isProtected(modifiers) &&
                    caller != tclass) ? caller : null;
            this.tclass = tclass;
            if (vclass == Object.class)
                this.vclass = null;
            else
                this.vclass = vclass;
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

        void targetCheck(T obj) {
            if (!tclass.isInstance(obj))
                throw new ClassCastException();
            if (cclass != null)
                ensureProtectedAccess(obj);
        }

        void updateCheck(T obj, V update) {
            if (!tclass.isInstance(obj) ||
                    (update != null && vclass != null && !vclass.isInstance(update)))
                throw new ClassCastException();
            if (cclass != null)
                ensureProtectedAccess(obj);
        }

        public boolean compareAndSet(T obj, V expect, V update) {
            if (obj == null || obj.getClass() != tclass || cclass != null ||
                    (update != null && vclass != null &&
                            vclass != update.getClass()))
                updateCheck(obj, update);
            return unsafe.compareAndSwapObject(obj, offset, expect, update);
        }

        public boolean weakCompareAndSet(T obj, V expect, V update) {
            // same implementation as strong form for now
            if (obj == null || obj.getClass() != tclass || cclass != null ||
                    (update != null && vclass != null &&
                            vclass != update.getClass()))
                updateCheck(obj, update);
            return unsafe.compareAndSwapObject(obj, offset, expect, update);
        }

        public void set(T obj, V newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null ||
                    (newValue != null && vclass != null && vclass != newValue.getClass())) {
                updateCheck(obj, newValue);
            }
            unsafe.putObjectVolatile(obj, offset, newValue);
        }

        public void lazySet(T obj, V newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null ||
                    (newValue != null && vclass != null && vclass != newValue.getClass())) {
                updateCheck(obj, newValue);
            }
            unsafe.putOrderedObject(obj, offset, newValue);
        }

        @SuppressWarnings("unchecked")
        public V get(T obj) {
            if (obj == null || obj.getClass() != tclass || cclass != null) targetCheck(obj);

            return (V) unsafe.getObjectVolatile(obj, offset);
        }

        @SuppressWarnings("unchecked")
        public V getAndSet(T obj, V newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null ||
                    (newValue != null && vclass != null && vclass != newValue.getClass())) {
                updateCheck(obj, newValue);
            }
            return (V) unsafe.getAndSetObject(obj, offset, newValue);
        }

        private void ensureProtectedAccess(T obj) {
            if (cclass.isInstance(obj)) return;
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
