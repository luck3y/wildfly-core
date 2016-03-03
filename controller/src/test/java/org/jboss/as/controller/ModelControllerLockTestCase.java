package org.jboss.as.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Unit tests of {@link ModelControllerLock}.
 *
 * @author Ken Wills <kwills@redhat.com> (c) 2016 Red Hat
 */
public class ModelControllerLockTestCase {

    private static final int OP1 = 11111;
    private static final int OP2 = 22222;
    private static final int OP3 = 33333;
    private static final long DEFAULT_TIMEOUT = 1;
    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;

    @Test
    public void testAcquireBasic() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lock(OP1);
        assertTrue(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lock(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testReacquireBasic() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lock(OP1);
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lock(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP1);
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP1);
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP1);
        lock.lock(OP2);
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lock(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP2);
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP2);
        assertTrue(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnlockNotLockedExclusive() throws IllegalStateException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.unlock(OP1);
    }

    @Test(expected = IllegalStateException.class)
    public void testTooManyExclusiveUnlocks() throws IllegalStateException {
        ModelControllerLock lock = new ModelControllerLock();
        for (int i = 0; i < DEFAULT_TIMEOUT; i++) {
            lock.lock(OP1);
        }

        for (int i = 0; i < 6; i++) {
            lock.unlock(OP1);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUnlockNotLockedShared() throws IllegalStateException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.unlockShared(OP1);
    }

    @Test(expected = IllegalStateException.class)
    public void testTooManyUnlocksShared() throws IllegalStateException {
        ModelControllerLock lock = new ModelControllerLock();
        for (int i = 0; i < DEFAULT_TIMEOUT; i++) {
            lock.lockShared(OP1);
        }

        for (int i = 0; i < 6; i++) {
            lock.unlockShared(OP1);
        }
    }

    @Test
    public void testAcquire() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lock(OP1);
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lock(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP1);
        lock.lock(OP2);
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lock(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testExclusiveBlocksShared() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lock(OP1);
        assertFalse(lock.lockSharedInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lockSharedInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlock(OP1);
        lock.lockShared(OP2);
        assertTrue(lock.lockSharedInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockSharedInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testAcquireSharedBasic() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lockShared(OP1);
        assertTrue(lock.lockSharedInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockShared(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlockShared(OP1);
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlockShared(OP1);
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlockShared(OP1);
        assertTrue(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testAcquireShared() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lockShared(OP1);
        lock.lockShared(OP2);
        assertTrue(lock.lockSharedInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockSharedInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lockInterruptibly(OP3, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockShared(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockShared(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testSharedBlocksExclusive() throws InterruptedException {
        ModelControllerLock lock = new ModelControllerLock();
        lock.lockShared(OP1);
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        lock.unlockShared(OP1);
        assertTrue(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        assertTrue(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testExclusiveWithThreads() throws InterruptedException {

        final ModelControllerLock lock = new ModelControllerLock();

        Runnable a = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock(OP1);
                    assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable b = new Runnable() {
            @Override
            public void run() {
                try {
                    assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                    lock.unlock(OP1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread t1 = new Thread(a);
        Thread t2 = new Thread(b);
        t1.start();
        t1.join();
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        t2.start();
        t2.join();
        assertTrue(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }

    @Test
    public void testSharedWithThreads() throws InterruptedException {

        final ModelControllerLock lock = new ModelControllerLock();

        Runnable a = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockShared(OP1);
                    assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                    assertTrue(lock.lockSharedInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                    assertFalse(lock.lockInterruptibly(OP1, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable b = new Runnable() {
            @Override
            public void run() {
                try {
                    assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                    lock.unlockShared(OP2);
                    lock.unlockShared(OP1);
                    assertTrue(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
                    lock.unlock(OP2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread t1 = new Thread(a);
        Thread t2 = new Thread(b);
        t1.start();
        t1.join();
        assertFalse(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
        t2.start();
        t2.join();
        assertTrue(lock.lockInterruptibly(OP2, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT));
    }
}
