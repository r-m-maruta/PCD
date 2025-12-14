package kahoot.net.server;

import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue<E> {

    private final Queue<E> queue = new LinkedList<>();
    private final int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(E e) throws InterruptedException {
        while (queue.size() == capacity)
            wait();
        queue.add(e);
        notifyAll();
    }

    public synchronized E take() throws InterruptedException {
        while (queue.isEmpty())
            wait();
        E e = queue.poll();
        notifyAll();
        return e;
    }
}
