/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Basic lock implementation using a permit object to allow reentrancy. The lock will only be released when all
 * participants which previously acquired the lock have called {@linkplain #unlock}.
 * The lock supports two mutually exclusive modes, shared and exclusive. If any shared locks are acquired and held
 * then the exclusive lock may not be acquired, and if the exclusive lock is held, the shared locks may not be acquired.
 * For an existing "permit holder" (operationId), the lock may be reentrantly re-acquired.
 *
 * @author Emanuel Muckenhuber
 * @author Ken Wills
 */
class ModelControllerLock {
    private final Sync sync = new Sync();

    void lock(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquire(permit);
    }

    void lockShared(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireShared(permit);
    }

    boolean lock(final Integer permit, final long timeout, final TimeUnit unit) {
        boolean result = false;
        try {
            result = lockInterruptibly(permit, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    boolean lockShared(final Integer permit, final long timeout, final TimeUnit unit) {
        boolean result = false;
        try {
            result = lockSharedInterruptibly(permit, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    void lockInterruptibly(final Integer permit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireInterruptibly(permit);
    }

    void lockSharedInterruptibly(final Integer permit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireSharedInterruptibly(permit);
    }

    boolean lockInterruptibly(final Integer permit, final long timeout, final TimeUnit unit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireNanos(permit, unit.toNanos(timeout));
    }

    boolean lockSharedInterruptibly(final Integer permit, final long timeout, final TimeUnit unit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireSharedNanos(permit, unit.toNanos(timeout));
    }

    void unlock(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.release(permit);
    }

    void unlockShared(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.releaseShared(permit);
    }

    boolean detectDeadlockAndGetLock(final int permit) {
        return sync.tryAcquire(permit);
    }

    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        private final AtomicReference<Object> permitHolder = new AtomicReference<>(null);

        @Override
        protected synchronized boolean tryAcquire(int permit) {
            if (compareAndSetState(0, 1)) {
                permitHolder.set(permit);
                return true;
            } else if (permitHolder.get().equals(permit)) {
                for (;;) {
                    int current = getState();
                    int next = current + 1; // increase by one
                    if (next < 0) // overflow
                        throw new Error("Maximum lock count exceeded");
                    if (compareAndSetState(current, next)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected synchronized int tryAcquireShared(final int permit) {
            if (permitHolder.get() != null) {
                return -1;
            }
            if (compareAndSetState(0, 1)) {
                return 1;
            } else {
                for (;;) {
                    int current = getState();
                    int next = current + 1; // increase by one
                    if (next < 0) // overflow
                        throw new Error("Maximum lock count exceeded");
                    if (compareAndSetState(current, next)) {
                        return 1;
                    }
                }
            }
        }

        @Override
        protected synchronized boolean tryRelease(final int permit) {
            final Object value = permitHolder.get();
            if (value == null) {
                throw new IllegalStateException();
            }
            if (value.equals(permit)) {
                for (;;) {
                    int current = getState();
                    int next = current - 1; // count down one
                    if(next < 0)
                        throw new IllegalStateException();
                    if (compareAndSetState(current, next)) {
                        if (next == 0) {
                            permitHolder.compareAndSet(value, null);
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        protected synchronized boolean tryReleaseShared(final int permit) {
            if (permitHolder.get() != null) {
                return false;
            } else {
                for (;;) {
                    int current = getState();
                    int next = current - 1; // count down one
                    if(next < 0)
                        throw new IllegalStateException();
                    if (compareAndSetState(current, next)) {
                        return (next == 0);
                    }
                }
            }
        }

    }
}
