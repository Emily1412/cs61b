package deque;

import java.lang.reflect.Array;
import java.util.logging.Level;

/**
 * ClassName: ArrayDeque
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/11 20:18
 */
public class ArrayDeque<T> {

    private T[] items;
    private int size;
    private int nextFirst; //下一个头部元素的坐标
    private int nextLast; //下一个尾部元素的坐标

    public ArrayDeque()
    {
        items = (T[]) new Object[8];
        size = 0;
        nextFirst = 0;
        nextLast = 0;

    }

    //重新设置大小
    private void resize(int newSize){
        T[] a = (T[]) new Object[newSize];
        int currentFirst = (nextFirst + 1) % items.length;
        for (int i = 0; i < size; i++){
            a[i] = items[(currentFirst + i) % items.length];
        }
        items = a;
        nextFirst = newSize - 1;
        nextLast = size;
    }

    //头增
    public void addFirst(T item){
        if (size == items.length){
            this.resize(size * 2);
        }
        items[nextFirst] = item;
        nextFirst = (nextFirst - 1 + items.length) % items.length;
        size++;
    }

    //尾增
    public void addLast(T item) {
        if (size == items.length){
            this.resize(size * 2);
        }
        items[nextLast] = item;
        nextLast = (nextLast + 1) % items.length; // 环形队列
        size++;
    }

    //判空
    public boolean isEmpty() {
        return size == 0;
    }

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

        if (size == 0){
            return null;
        }
        nextFirst = (nextFirst + 1) % items.length;
        T x = items[nextFirst];
        items[nextFirst] = null; // 将这个位置置为空
            size--;
            if (size > 0 && size < items.length / 4){
                this.resize(items.length / 4);
            }
            return x;
        }


    //尾删
    public T removeLast() {

        if (size == 0){
            return null;
        }
        nextLast = (nextLast - 1 + items.length) % items.length;
        T x = items[nextLast];
        items[nextLast] = null;

            size--;
        if (size > 0 && size < items.length / 4){
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
}
