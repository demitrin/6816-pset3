import java.util.concurrent.atomic.*; // for AtomicXXX classes

class SingleScanSnapshot implements Snapshot {
    /**
     * The current sequence number (aka timestamp).
     * <p>
     * curSeq is updated by scan and used to compare to the sequence numbers
     * of values in Register objects, in order to know when a value is either:
     * 1. old, thus can be overwritten by update,
     * 2. snapped, thus should be preserved so the scan can return it, or
     * 3. fresh, thus the scan should NOT return it.
     */
    protected final AtomicInteger curSeq;
    /**
     * The current values of the snapshot object.
     * <p>
     * high and low are arrays of Register objects, which are just immutable
     * pairs of (int value, sequence number).
     * This is where you can store the values that update will change and that
     * scan will read and return. While all operations on this array are atomic
     * and changes will be seen across all threads, it does not have a built-in
     * atomic snapshot method, so you will need to
     */
    protected final AtomicReferenceArray<Register> high;
    protected final AtomicReferenceArray<Register> low;

    /**
     * SingleScanSnapshot constructor.
     * <p>
     * Constructs a new SingleScanSnapshot object which supports the specified
     * number of threads as "writers" that update concurrently. The initial
     * value of each slot is set to 0.
     *
     * @param numWriters The number of allowed concurrent updaters.
     */
    public SingleScanSnapshot(int numWriters) {
        curSeq = new AtomicInteger(0);
        high = new AtomicReferenceArray<Register>(numWriters);
        low = new AtomicReferenceArray<Register>(numWriters);
        for (int i = 0; i < numWriters; i++) {
            high.set(i, new Register());
            low.set(i, new Register());
        }
    }

    /**
     * Gets an atomic snapshot of the values in SingleScanSnapshot.
     * <p>
     * This method is called by one designated "scanner" thread. This method
     * returns a view of the values in each slot of the SingleScanSnapshot
     * object such that for each index i, the last call of update(i, view[i]) is
     * the last update(i, val) that is linearized before this method call. If no
     * update(i, *) calls are linearized before this method call, then
     * view[i] == 0.
     *
     * @return The value of the Snapshot.
     */
    public int[] scan() {
        int length = high.length();
        int[] view = new int[length];
        Register highReg;
        curSeq.set(curSeq.get() + 1);
        for (int i = 0; i < length; i++) {
            highReg = high.get(i);
            if (highReg.seq < curSeq.get()) {
                view[i] = highReg.val;
            } else {
                view[i] = low.get(i).val;
            }
        }

        return view;
    }

    /**
     * Updates the value of the SingleScanSnapshot in the slot for a given
     * thread.
     * <p>
     * This method is called by many "writer" threads. This method sets the slot
     * in the SingleScanSnapshot at location processNum to have value val such
     * that all calls to scan linearized after this method call will read
     * view[processNum] == val until after the next update(processNum, val2) for
     * some val != val2 is linearized.
     * No two concurrent calls to inc will ever have the same processNum.
     *
     * @param processNum A unique integer that represents the ID of the calling process.
     * @param val        The value to be written.
     */
    public void update(int processNum, int val) {
        int seq = curSeq.get();
        Register highReg = high.get(processNum);
        if (seq != highReg.seq) {
            low.set(processNum, highReg);
        }
        high.set(processNum, new Register(val, seq));
    }
}
