package kahoot.game;

public class ModifiedCountdownLatch {

    private final int bonusFactor;    // ex: 2
    private final int bonusCount;     // ex: 2 (primeiros 2 ganham bonus)
    private final int waitPeriodMs;   // ex: 30000
    private int count;                // nº de jogadores esperados

    private int answered = 0;
    private long startTime;

    public ModifiedCountdownLatch(int bonusFactor, int bonusCount, int waitPeriodMs, int count) {
        this.bonusFactor = bonusFactor;
        this.bonusCount = bonusCount;
        this.waitPeriodMs = waitPeriodMs;
        this.count = count;
        this.startTime = System.currentTimeMillis();
    }


    public synchronized int countdown() {
        answered++;

        int multiplier = (answered <= bonusCount) ? bonusFactor : 1;

        count--;
        notifyAll();

        return multiplier;
    }


    public synchronized void await() throws InterruptedException {
        long end = startTime + waitPeriodMs;

        while (count > 0) {
            long remaining = end - System.currentTimeMillis();
            if (remaining <= 0) break;

            wait(remaining);
        }
    }
}
