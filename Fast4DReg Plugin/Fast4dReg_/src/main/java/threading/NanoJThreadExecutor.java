package threading;

import tools.Log;

import java.util.ArrayList;

/**
 * Created by Ricardo Henriques on 28/02/2016.
 */
public class NanoJThreadExecutor {

    public final static int nCPUs = Runtime.getRuntime().availableProcessors();
    private final ArrayList<Thread> threadList = new ArrayList<Thread>();
    public int threadBufferSize;
    private final boolean memorizeThreads;
    private Log log = new Log();
    private int threadCounter = 0;
    public boolean showProgress = true;
    private long timer = System.currentTimeMillis();

    public NanoJThreadExecutor(boolean memorizeThreads) {
        this.memorizeThreads = memorizeThreads;
        this.threadBufferSize = nCPUs;
    }

    public NanoJThreadExecutor(int threadBufferSize, boolean memorizeThreads) {
        this.memorizeThreads = memorizeThreads;
        this.threadBufferSize = threadBufferSize;
    }

    public void execute(Thread thread) {
        threadList.add(thread);
        thread.start();
        threadCounter++;
        if (!showProgress) {
            while (howManyWorking() > threadBufferSize) {}
        }
        else {
            int nAlive;
            while ((nAlive = howManyWorking()) > threadBufferSize) {
                if (System.currentTimeMillis()-timer > 3000) {
                    log.status("Running thread: " + threadCounter + " Alive: " + nAlive);
                    timer = System.currentTimeMillis();
                }
            }
        }
    }

    public ArrayList<Thread> finish() {
        for (Thread t: threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (showProgress) log.status("");
        return threadList;
    }

    private int howManyWorking() {
        int aliveCounter = 0;
        int counter = 0;

        while (counter < threadList.size()) {
            if (threadList.get(counter).isAlive()) {
                aliveCounter++;
                counter++;
            }
            else if (!memorizeThreads) {
                threadList.remove(counter);
            }
            else {
                counter++;
            }
        }
        return aliveCounter;
    }
}
