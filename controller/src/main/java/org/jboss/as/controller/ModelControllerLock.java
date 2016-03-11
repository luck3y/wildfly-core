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
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

/**
 * Basic lock implementation using a permit value to allow reentrancy. The lock will only be released when all
 * participants which previously acquired the lock have called {@linkplain #unlock}.
 *
 * The lock supports two mutually exclusive modes, shared and exclusive. If shared locks are acquired and held
 * then the exclusive lock may not be acquired, and if the exclusive lock is held, the shared locks may not be acquired.
 * For an existing "permit holder" (operationId), the lock may be reentrantly re-acquired.
 *
 * @author Emanuel Muckenhuber
 * @author Ken Wills
 */
class ModelControllerLock {
    private final Sync sync = new Sync();

    /**
     * Attempts to acquire in exclusive mode. This will allow any other consumers using the same {@code permit} to
     * also acquire. This is typically used for a write lock.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @throws IllegalStateException - if the permit is null.
     */
    void lock(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquire(permit);
    }

    /**
     * Attempts to acquire the lock in shared mode. In this mode the lock may be shared over a number
     * of different permit holders, and blocking the exclusive look from being acquired. Typically used for read locks to
     * allow multiple readers concurrently.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     */
    void lockShared(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireShared(permit);
    }

    /** Attempts exclusive acquisition with a max wait time.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @param timeout - the time value to wait for acquiring the lock
     * @param unit - See {@code TimeUnit} for valid values
     * @return {@code boolean} true on success.
     */
    boolean lock(final Integer permit, final long timeout, final TimeUnit unit) {
        boolean result = false;
        try {
            result = lockInterruptibly(permit, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    /** Attempts shared acquisition with a max wait time.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @param timeout - the time value to wait for acquiring the lock
     * @param unit - See {@code TimeUnit} for valid values
     * @return {@code boolean} true on success.
     */
    boolean lockShared(final Integer permit, final long timeout, final TimeUnit unit) {
        boolean result = false;
        try {
            result = lockSharedInterruptibly(permit, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    /**
     * Acquire the exclusive lock allowing the acquisition to be interrupted.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @throws InterruptedException - if the acquiring thread is interrupted.
     * @throws IllegalArgumentException if {@code permit} is null.
     */
    void lockInterruptibly(final Integer permit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireInterruptibly(permit);
    }

    /**
     * Acquire the shared lock allowing the acquisition to be interrupted.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @throws InterruptedException - if the acquiring thread is interrupted.
     * @throws IllegalArgumentException if {@code permit} is null.
     */
    void lockSharedInterruptibly(final Integer permit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireSharedInterruptibly(permit);
    }

    /**
     * Acquire the exclusive lock, with a max wait timeout to acquire.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @param timeout - the timeout scalar quantity.
     * @param unit - see {@code TimeUnit} for quantities.
     * @return {@code boolean} true on successful acquire.
     * @throws InterruptedException - if the acquiring thread was interrupted.
     * @throws IllegalArgumentException if {@code permit} is null.
     */
    boolean lockInterruptibly(final Integer permit, final long timeout, final TimeUnit unit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireNanos(permit, unit.toNanos(timeout));
    }

    /**
     * Acquire the shared lock, with a max wait timeout to acquire.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @param timeout - the timeout scalar quantity.
     * @param unit - see {@code TimeUnit} for quantities.
     * @return {@code boolean} true on successful acquire.
     * @throws InterruptedException - if the acquiring thread was interrupted.
     * @throws IllegalArgumentException if {@code permit} is null.
     */
    boolean lockSharedInterruptibly(final Integer permit, final long timeout, final TimeUnit unit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireSharedNanos(permit, unit.toNanos(timeout));
    }

    /**
     * Unlock a previously held exclusive lock. In the case of multiple lock holders, the underlying lock is only
     * released when all of the holders have called #unlock.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @throws IllegalArgumentException if {@code permit} is null.
     */
    void unlock(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.release(permit);
    }

    /**
     * Unlock a previously held shared lock. In the case of multiple lock holders, the underlying lock is only
     * released when all of the holders have called #unlock. In the case of shared mode lock holders, they may
     * be a variety of different permit holders, as the shared mode lock is not tagged with a single owner as
     * the exclusive lock is.
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @throws IllegalArgumentException if {@code permit} is null.
     */
    void unlockShared(final Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.releaseShared(permit);
    }

    /**
     * Attempt to query and acquire the exclusive lock
     * @param permit - the permit Integer for this operation. May not be {@code null}.
     * @return {@code boolean} true if the lock was acquired, false if not available
     * for locking in exclusive mode, or already locked shared.
     */
    boolean detectDeadlockAndGetLock(final int permit) {
        return sync.tryAcquire(permit);
    }

    /**
     * Implementation {@link AbstractQueuedLongSynchronizer} that maintains
     * lock state in a single {@code long}, managed by #getState() and #compareAndSet().
     */
    private class Sync extends AbstractQueuedLongSynchronizer {
        private static final long serialVersionUID = 1L;

        // reserves the top 32 bytes for exclusive / count shorts.
        private static final int EXCLUSIVE_SHIFT = 48;
        private static final int COUNT_SHIFT = 32;

        // 16 bits for shorts holding the lock mode and count.
        private static final long SHORT_MASK = 0xffffL;
        // 32 bits for int storing the permit holder.
        private static final long PERMIT_MASK = 0xffffffffL;
        // values for indicating shared / exclusive modes.
        private static final short EXCLUSIVE = 1;
        private static final short SHARED = 2;

        @Override
        protected boolean tryAcquire(final long permitID) {
            return internalAcquire(permitID, true) == 1;
        }

        @Override
        protected long tryAcquireShared(final long permitID) {
            return internalAcquire(permitID, false);
        }

        @Override
        protected boolean tryRelease(final long permitID) {
            return internalRelease(permitID, true);
        }

        @Override
        protected boolean tryReleaseShared(final long permitID) {
            return internalRelease(permitID, false);
        }

        @Override
        protected boolean isHeldExclusively() {
            long state = getState();
            return state != 0L && getLockMode(state) == EXCLUSIVE;
        }

        private int getPermitHolder(final long value) {
            return (int) (value & PERMIT_MASK);
        }

        private short getCount(final long value) {
            return (short) ((value >> COUNT_SHIFT) & SHORT_MASK);
        }

        private short getLockMode(final long value) {
            return (short) ((value >> EXCLUSIVE_SHIFT) & SHORT_MASK);
        }

        // store creates the long state value, storing it as (short)lockMode(short)lockCount(int)permitHolder
        // int the case of shared mode, permitHolder will be OL.
        private long makeState(final int permit, final int count, final short lockMode) {
            assert lockMode == EXCLUSIVE || lockMode == SHARED;
            assert count > 0;
            if (lockMode == SHARED && permit != 0) {
                throw new IllegalStateException("Permit for shared mode lock must be 0.");
            }
            if (count < 0 || count > Short.MAX_VALUE)
                throw new IllegalStateException("Maximum lock count exceeded");

            short newCount = (short) count;
            long state = ((((long) lockMode) << EXCLUSIVE_SHIFT)) | ((((long) newCount) << COUNT_SHIFT)) | (((long) permit) & PERMIT_MASK);

            // tmp asserts
            assert permit == getPermitHolder(state);
            assert count == getCount(state);
            assert lockMode == getLockMode(state);

            return state;
        }

        /**
         * Attempt to acquire the lock in the specified lock mode.
         * @param permitID - the lock permit object, for exclusive locks, multiple acquires for the same permitID are allowed.
         *                 This value is a long, but must be <= {@code Integer.MAX_VALUE} and >= {@code Integer.MIN_VALUE}.
         * @param exclusive - Whether to attempt to acquire the exclusive (true) or shared lock (false).
         * @return {@code long} < 0 for failure, > 0 for success.
         */
        private long internalAcquire(final long permitID, final boolean exclusive) {
            // constrain permitID to int
            if (permitID > Integer.MAX_VALUE || permitID < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("permitID must be between Integer.MIN_VALUE and Integer.MAX_VALUE");
            }

            if (isHeldExclusively() && !exclusive) {
                return -1;
            }

            int permit = (int) permitID;
            long state = getState();
            int count = getCount(state);
            short mode = getLockMode(state);

            // can't exclusively lock, already in shared mode
            if (mode == SHARED && exclusive) {
                return -1;
            }

            // create the new state, incrementing the lock count by 1.
            long newState = makeState(exclusive ? permit : 0, (short) (count + 1), exclusive ? EXCLUSIVE : SHARED);
            // currently unlocked state, if this succeeds, then the lock has been acquired in the specified mode.
            if (compareAndSetState(0L, newState)) {
                return 1;
            } else {
                // loop until the CAS is successful and the state has been updated.
                for (; ; ) {
                    // read the current state
                    state = getState();
                    count = getCount(state);
                    int permitHolder = getPermitHolder(state);
                    int next = count + 1; // increase lock count

                    if (exclusive) {
                        // in exclusive mode, only the permit holder is allowed access
                        if (permitHolder != permit) {
                            return -1;
                        }
                        mode = EXCLUSIVE;
                    } else {
                        // in shared mode, the permit holder must be 0
                        if (permitHolder != 0) {
                            return -1;
                        }
                        mode = SHARED;
                    }
                    newState = makeState(mode == SHARED ? 0 : permitHolder, (short) next, mode);
                    if (compareAndSetState(state, newState)) {
                        return 1;
                    }
                }
            }
        }

        /**
         * Attempt to release the lock in the specified lock mode.
         * @param permitID - the lock permit object. This value is a long, but must be <= {@code Integer.MAX_VALUE} and >= {@code Integer.MIN_VALUE}.
         * @param exclusive - Whether to attempt to release the lock in the the exclusive (true) or shared lock (false) modes.
         * @return {@code boolean} true for success.
         */
        private boolean internalRelease(long permitID, final boolean exclusive) {
            // constrain permitID to int
            if (permitID > Integer.MAX_VALUE || permitID < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("permitID must be between Integer.MIN_VALUE and Integer.MAX_VALUE");
            }

            int permit = (int) permitID;
            for (; ; ) {
                long state = getState();
                short mode = getLockMode(state);
                if (mode == 0) {
                    mode = exclusive ? EXCLUSIVE : SHARED;
                }

                if (mode == EXCLUSIVE && !exclusive)
                    throw new IllegalStateException("Read lock not held.");
                if (mode == SHARED && exclusive)
                    throw new IllegalStateException("Write lock not held.");

                int permitHolder = getPermitHolder(state);
                int next = getCount(state) - 1; // decrease lock count
                if (next < 0) {
                    throw new IllegalStateException();
                }
                if (mode == EXCLUSIVE) {
                    if (permitHolder != permit)
                        return false;
                } else { // shared mode
                    if (permitHolder != 0)
                        return false;
                }
                long newState = next == 0L ? 0 : makeState(mode == EXCLUSIVE ? permit : 0, (short) next, mode);
                if (compareAndSetState(state, newState)) {
                    return next == 0;
                }
            }
        }

    }
}


