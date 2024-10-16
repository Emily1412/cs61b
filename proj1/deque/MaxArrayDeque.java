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
        if (this.isEmpty()) {
            return null;
        }
        //主要是实现max函数，遍历这个数据结构，调用抽象的compare函数，找到最大的值
        int idx = 0;
        T maxe = this.get(idx);
        for (int i = 0; i < size(); i++) {
            idx = (idx + i);
            if (comparator.compare(maxe, this.get(idx)) > 0) {
                maxe = this.get(idx);
            }
        }
        return maxe;
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
    public  boolean equals(Object o) {
        // 可选实现 不做评分
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof MaxArrayDeque)){
            return false;
        }
        if (((MaxArrayDeque<?>) o).max() != max()){
            return false;
        }
        return super.equals(o);
    }
}
