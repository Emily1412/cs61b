package deque;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * ClassName: ArrayDeque
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/11 20:18
 */
public class ArrayDeque<T> implements Iterable<T>, Deque<T> {

    private T[] items;
    private int size;
    private int nextFirst; //下一个头部元素的坐标
    private int nextLast; //下一个尾部元素的坐标

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        nextFirst = items.length - 1;
        nextLast = 0;

    }

    //重新设置大小
    private void resize(int newSize) {
        T[] a = (T[]) new Object[newSize];
        int currentFirst = (nextFirst + 1) % items.length;
        for (int i = 0; i < size; i++) {
            a[i] = items[(currentFirst + i) % items.length];
        }
        items = a;
        nextFirst = newSize - 1;
        nextLast = size;
    }

    //头增
    public void addFirst(T item) {
        if (size == items.length) {
            this.resize(size * 2);
        }
        items[nextFirst] = item;
        nextFirst = (nextFirst - 1 + items.length) % items.length;
        size++;
    }

    //尾增
    public void addLast(T item) {
        if (size == items.length) {
            this.resize(size * 2);
        }
        items[nextLast] = item;
        nextLast = (nextLast + 1) % items.length; // 环形队列
        size++;
    }

    //判空 接口已有默认实现


    public int size() {
        return size;
    }

    //打印
    public void printDeque() {
        int currentFirst = (nextFirst + 1) % items.length;
        for (int i = 0; i < size; i++) {
            System.out.print(items[(currentFirst + i) % items.length] + " ");
        }

        System.out.println();
    }

    //头删
    public T removeFirst() {

        if (size == 0) {
            return null;
        }
        nextFirst = (nextFirst + 1) % items.length;
        T x = items[nextFirst];
        items[nextFirst] = null; // 将这个位置置为空
        size--;
        if (size > 0 && size < items.length / 4) {
            this.resize(items.length / 4);
        }
        return x;
    }


    //尾删
    public T removeLast() {

        if (size == 0) {
            return null;
        }
        nextLast = (nextLast - 1 + items.length) % items.length;
        T x = items[nextLast];
        items[nextLast] = null;

        size--;
        if (size > 0 && size < items.length / 4) {
            this.resize(items.length / 4);
        }
        return x;

    }

    //下标索引
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        int actualIndex = (nextFirst + 1 + index) % items.length;
        return items[actualIndex];
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof deque.Deque)) { // 检查是否实现了 Deque 接口
            return false;
        }

        deque.Deque<T> other = (deque.Deque<T>) o;

        // 比较大小是否相同
        if (this.size() != other.size()) {
            return false;
        }

        // 逐个元素比较
        for (int i = 0; i < this.size(); i++) {
            T item1 = this.get(i);
            T item2 = other.get(i);
            if (!item1.equals(item2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int current = (nextFirst + 1) % items.length;
        private int i = 0; //已经迭代的元素数量


        @Override
        public boolean hasNext() {
            return i < size;
        }

        @Override
        public T next() {
            //无需显示的判断是否有next，调用者需要调用hasnext来检查的
            if (!hasNext()) {
                // 如果没有下一个元素，抛出异常
                throw new NoSuchElementException();
            }

            T item = items[current];
            current = (current + 1) % items.length;
            i++;
            return item;

        }
    }
}
