package deque;

/**
 * ClassName: Deque
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/15 9:42
 */
public interface Deque<T> {
    void addFirst(T item);
    void addLast(T item);
    default boolean isEmpty() {
        if (size() == 0) {
            return true;
        }
        return false;
    }
    int size();
    void printDeque();
    T removeFirst();
    T removeLast();
    T get(int index);

}
