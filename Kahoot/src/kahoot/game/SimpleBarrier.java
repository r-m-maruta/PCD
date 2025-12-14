package kahoot.game;

import java.util.HashMap;
import java.util.Map;


public class SimpleBarrier {

    private final int playersPerTeam;
    private final long timeoutMs;

    private final Map<String, Integer> countPerTeam = new HashMap<>();
    private long startTime;

    public SimpleBarrier(int playersPerTeam, long timeoutMs) {
        this.playersPerTeam = playersPerTeam;
        this.timeoutMs = timeoutMs;
        this.startTime = System.currentTimeMillis();
    }


    public synchronized void arrive(String team) {
        countPerTeam.put(team, countPerTeam.getOrDefault(team, 0) + 1);
        notifyAll();
    }


    public synchronized void await(String team) throws InterruptedException {
        long end = startTime + timeoutMs;

        while (countPerTeam.getOrDefault(team, 0) < playersPerTeam) {
            long remaining = end - System.currentTimeMillis();
            if (remaining <= 0) break;

            wait(remaining);
        }
    }
}
