package ticketingsystem;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class BackOffAtomicLong {

    public static long bk;

    private final AtomicLong value = new AtomicLong(0L);

    public long get() {
        return value.get();
    }

    public long getAndIncrement() {
        for (; ; ) {
            long current = get();
            long next = current + 1;
            if (compareAndSet(current, next))
                return next;
        }
    }

    public boolean compareAndSet(final long current, final long next) {
        if (value.compareAndSet(current, next)) {
            return true;
        } else {
            LockSupport.parkNanos(2L);
            return false;
        }
    }

    public void set(final long l) {
        value.set(l);
    }

}
