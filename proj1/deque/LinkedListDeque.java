package deque;

//import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ClassName: LinkedListDeque
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/11 15:53
 */
public class LinkedListDeque<T> implements Iterable<T> {


    //节点类
    private class Node {
        public T item; //存储的值
        //前后结点
        public Node next;
        public Node prev;

        public Node(T i, Node next, Node prev) {
            //根据值和前后节点构造节点
            this.item = i;
            this.next = next;
            this.prev = prev;
        }
    }

    private Node sentinel; //哨兵节点
    private int size; //容量

    //空构造器
    public LinkedListDeque() {
        sentinel = new Node(null, null, null); //初始化的时候哨兵节点应指向自己
        size = 0;
    }

    // 递归辅助函数
    private T getRecursiveHelper(Node current, int index) {
        if (index == 0) {
            return current.item;  // 基础情况，找到节点，返回其值
        }
        return getRecursiveHelper(current.next, index - 1);  // 递归，前往下一个节点
    }

    // 用递归来找到节点值
    public T getRecursive(int index) {
        if (index < 0 || index >= size) {
            return null;  // 边界检查，防止越界
        }
        return getRecursiveHelper(sentinel.next, index);  // 调用递归函数，从第一个有效节点开始查找
    }

    //头部增加
    public void addFirst(T item) {

        if (size == 0) {
            Node newNode = new Node(item, sentinel.next, sentinel);
            sentinel.next = newNode;
            sentinel.prev = newNode;  // 空链表时，prev 也指向新节点
        } else {
            Node n = sentinel.next;
            Node n2 = new Node(item, n, sentinel);
            sentinel.next.prev = n2;
            sentinel.next = n2;
        }
        size++;
    }

    //尾部增加
    public void addLast(T item) {
        if (size == 0) {
            Node newNode = new Node(item, sentinel, sentinel);
            sentinel.next = newNode;
            sentinel.prev = newNode;  // 修复：确保哨兵的 prev 也正确指向新节点
        } else {
            Node n = sentinel.prev;
            Node n2 = new Node(item, sentinel, n);
            //下面这两行的顺序是错的 ？
            //sentinel.prev = n2;
            //n.next = n2;
            sentinel.prev.next = n2;
            sentinel.prev = n2;
        }
        size++;
    }

    //判空 接口已有默认实现


    //容量
    public int size() {
        return size;
    }

    //打印队列
    public void printDeque() {
        Node n = sentinel.next;
        while (n != sentinel) {
            System.out.print(n.item + " ");
            n = n.next;
        }
        System.out.println();
    }

    //头部删除
    public T removeFirst() {
        if (size != 0) {
            //有哨兵节点的好处就是不用判断是不是只剩最后一个元素了

            Node n = sentinel.next;
            T item = n.item;
            if (size == 1) {
                //当只有一个元素的时候，修复sentinel!!!
                sentinel.next = null;
                sentinel.prev = null;
            } else {
                sentinel.next = n.next;
                sentinel.next.prev = sentinel;
            }
            n.prev = null;
            n.next = null;
            size--;
            return item;
        }
        return null;
    }

    //尾部删除
    public T removeLast() {
        if (size != 0) {
            T item = sentinel.prev.item;
            Node n = sentinel.prev; //最后一个节点
            if (size == 1) {
                sentinel.next = null;
                sentinel.prev = null;
            } else {
                n.prev.next = sentinel;
                sentinel.prev = n.prev;
            }
            n.next = null;
            n.prev = null;
            size--;
            return item;
        }
        return null;
    }

    //根据下标索引
    public T get(int index) {
        if (index >= 0 && index < size) {
            int i = 0;
            Node n = sentinel;
            while (i <= index) {
                n = n.next;
                i++;
            }
            return n.item;
        }
        return null;
    }

    //迭代器
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private Node current = sentinel.next;

        @Override
        public boolean hasNext() {
            return current != sentinel;
        }

        @Override
        public T next() {
            if(!hasNext()){
                throw new NoSuchElementException();
            }
            T item = current.item;
            current = current.next;
            return item;
        }
    }


    //判等
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof LinkedListDeque)){
            return false;
        }
        //将o强制转换为linkedlistdeque
        LinkedListDeque<T> other = (LinkedListDeque<T>) o;
        if (size != other.size){
            return false;
        }
        Node current = sentinel.next;
        Node otherCurrent = other.sentinel.next;
        while (current != this.sentinel && otherCurrent != other.sentinel){
            if (!current.item.equals(otherCurrent.item)){
                return false;
            }
        }
        return true;
    }
}
