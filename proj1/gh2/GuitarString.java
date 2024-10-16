package gh2;


import deque.ArrayDeque;
import deque.Deque;




//Note: This file will not compile until you complete the Deque implementations
public class GuitarString {
    /** Constants. Do not change. In case you're curious, the keyword final
     * means the values cannot be changed at runtime. We'll discuss this and
     * other topics in lecture on Friday. */
    private static final int SR = 44100;      // Sampling Rate
    private static final double DECAY = .996; // energy decay factor

    /* Buffer for storing sound data. */

    private Deque<Double> buffer;

    /* Create a guitar string of the given frequency.  */
    public GuitarString(double frequency) {

        int capacity = (int) Math.round(SR / frequency);  // 计算容量
        buffer = new ArrayDeque<>();  // 初始化buffer

        // 填充buffer为0.0
        for (int i = 0; i < capacity; i++) {
            buffer.addLast(0.0);  // 在末尾添加0.0
        }
    }


    /* Pluck the guitar string by replacing the buffer with white noise. */
    public void pluck() {
        //既然已经封装好了方法，就不要使用更底层的方法
        for (int i = 0; i < buffer.size(); i++) {
            buffer.removeFirst(); // Dequeue the front
            double r = Math.random() - 0.5;  // Generate random noise
            buffer.addLast(r);  // Add new random value
        }
    }

    /* Advance the simulation one time step by performing one iteration of
     * the Karplus-Strong algorithm.
     */
    public void tic() {
        // 首先确保 buffer 中至少有两个元素，才能进行操作
        if (buffer.size() < 2) {
            return;  // 如果元素不够，直接返回，不做操作
        }

        // 移除第一个元素，并检查是否为 null
        Double first = buffer.removeFirst();
        if (first == null) {
            return;  // 如果移除的元素为 null，直接返回
        }

        // 获取第二个元素
        Double second = buffer.get(0);
        if (second == null) {
            return;  // 如果第二个元素为 null，直接返回
        }

        // 计算新值并添加到 buffer 的末尾
        double newDouble = (first + second) / 2 * DECAY;
        buffer.addLast(newDouble);
    }


    /* Return the double at the front of the buffer. */
    public double sample() {
        if (!buffer.isEmpty()) {
            return buffer.get(0);
        }
        return 0;
    }
}


