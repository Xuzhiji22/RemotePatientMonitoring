package rpm.data;

import java.util.ArrayList;
import java.util.List;

public class MinuteRingBuffer<T> {
    private final Object[] buf;
    private int head = 0;
    private int size = 0;

    public MinuteRingBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.buf = new Object[capacity];
    }

    public synchronized void add(T item) {
        buf[head] = item;
        head = (head + 1) % buf.length;
        if (size < buf.length) size++;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<T> snapshot() {
        List<T> out = new ArrayList<>(size);
        int start = (head - size + buf.length) % buf.length;
        for (int i = 0; i < size; i++) out.add((T) buf[(start + i) % buf.length]);
        return out;
    }

    public int capacity() { return buf.length; }
    public synchronized int size() { return size; }
}
