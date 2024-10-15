package deque;

/**
 * ClassName: Deque
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/15 9:42
 */
public interface Deque <T> {
    public void addFirst(T item);
    public void addLast(T item);
    default boolean isEmpty(){
        if (size() == 0){
            return true;
        }
        return false;
    }
    public int size();
    public void printDeque();
    public T removeFirst();
    public T removeLast();
    public T get(int index);

    void resize(int x);
}
