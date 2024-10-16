package deque;

import java.util.Comparator;

/**
 * ClassName: MaxArrayDeque
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/14 22:49
 */
public class MaxArrayDeque<T> extends ArrayDeque<T> {

    private Comparator<T> comparator;

    public MaxArrayDeque(Comparator<T> c) {
        super(); //调用父类的构造方法
        this.comparator = c;

    }

    public T max() {
        return max(comparator);
    }


    public T max(Comparator<T> c) {
        if (this.isEmpty()) {
            return null;
        }
        T maxe = this.get(0);
        for (int i = 0; i < size(); i++) {
            if (c.compare(this.get(i), maxe) > 0) {
                maxe = this.get(i);
            }
        }
        return maxe;
    }

}
