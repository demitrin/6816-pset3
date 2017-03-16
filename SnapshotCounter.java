import java.util.concurrent.atomic.*; // for AtomicXXX classes
public class Counter implements Reader {
    /**
     * You may add your fields here.
     */

    protected final AtomicReferenceArray<Register> high;
    protected final AtomicReferenceArray<Register> low;
    protected final AtomicInteger curSeq;
    /**
     * Initializes a Counter.
     *
     * @param numServers
     *          The number of servers which it must read from.
     */
    public Counter(int numServers) {
        curSeq = new AtomicInteger(0);
        high = new AtomicReferenceArray<Register>(numServers);
        low = new AtomicReferenceArray<Register>(numServers);
        for (int i = 0; i < numServers; i++) {
            high.set(i, new Register());
            low.set(i, new Register());
        }
    }
    
    /**
     * Returns the value of the Counter.
     * 
     * This method returns the sum of all increments linearized before
     * this method's linearization point.
     * Your implementation only needs to support one thread that calls read().
     * 
     * @return
     *          The value of the Counter.
     */
    public int read() {
        int length = high.length();
        int sum = 0;
        Register highReg;
        curSeq.set(curSeq.get() + 1);
        for (int i = 0; i < length; i++) {
            highReg = high.get(i);
            if (highReg.seq < curSeq.get()) {
                sum += highReg.val;
            } else {
                sum += low.get(i).val;
            }
        }

        return sum;
    }
    
    /**
     * You may add other methods here.
     */
    public void inc(int processNum) {
        int seq = curSeq.get();
        Register highReg = high.get(processNum);
        if (seq != highReg.seq) {
            low.set(processNum, highReg);
        }
        high.set(processNum, new Register(highReg.val + 1, seq));
    }
}

class CountingServer implements Server {
    /**
     * You may add your fields here.
     */
    
    /**
     * Initializes a CountingServer.
     *
     * @param counter
     *          The Counter which receives data from this server.
     *
     * @param processNum
     *          A unique integer that represents the ID of the incrementing
     *          process.
     */
    protected Counter counter;
    protected int processNum;
    public CountingServer(Counter counter, int processNum) {
        this.counter = counter;
        this.processNum = processNum;
    }
    
    /**
     * Increments the value of the Counter.
     * 
     * This method must add one to the value of all future calls to read()
     * (on the Counter from the constructor) linearized after this call.
     * Each CountingServer object will only be operated on by one thread.
     */
    public void inc() {
        counter.inc(processNum);
    }
}

