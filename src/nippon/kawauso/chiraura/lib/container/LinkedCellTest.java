/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class LinkedCellTest {

    /**
     * 挿入の検査。
     */
    @Test
    public void testInsertNext() {
        final int length = 10;

        final List<Integer> answer = new LinkedList<>();

        // 空挿入と先頭挿入。
        final LinkedCell<Integer> cell = new LinkedCell<>(0);
        answer.add(0);
        for (int i = 1; i < length; i++) {
            cell.insertNext(new LinkedCell<>(i));
            answer.add(1, i);
        }

        LinkedCell<Integer> cur = cell;
        Assert.assertNull(cur.getPrevious());
        for (int i = 0; i < answer.size(); i++) {
            Assert.assertEquals(answer.get(i), cur.get());
            cur = cur.getNext();
        }
        Assert.assertNull(cur);

        // 途中挿入。
        cell.getNext().insertNext(new LinkedCell<>(length));
        answer.add(2, length);

        cur = cell;
        Assert.assertNull(cur.getPrevious());
        for (int i = 0; i < answer.size(); i++) {
            Assert.assertEquals(answer.get(i), cur.get());
            cur = cur.getNext();
        }
        Assert.assertNull(cur);

        // 末尾挿入。
        for (cur = cell; cur.getNext() != null; cur = cur.getNext()) {
        }
        cur.insertNext(new LinkedCell<>(length + 1));
        answer.add(length + 1);

        cur = cell;
        Assert.assertNull(cur.getPrevious());
        for (int i = 0; i < answer.size(); i++) {
            Assert.assertEquals(answer.get(i), cur.get());
            cur = cur.getNext();
        }
        Assert.assertNull(cur);
    }

    /**
     * 削除の検査。
     */
    @Test
    public void testRemove() {
        final int length = 10;

        final List<Integer> answer = new LinkedList<>();

        // リストの用意。
        final LinkedCell<Integer> cell = new LinkedCell<>(0);
        answer.add(0);
        for (int i = 1; i < length; i++) {
            cell.insertNext(new LinkedCell<>(i));
            answer.add(1, i);
        }

        // 途中削除。
        Assert.assertEquals(answer.remove(1), cell.getNext().remove().get());

        LinkedCell<Integer> cur = cell;
        Assert.assertNull(cur.getPrevious());
        for (int i = 0; i < answer.size(); i++) {
            Assert.assertEquals(answer.get(i), cur.get());
            cur = cur.getNext();
        }
        Assert.assertNull(cur);

        // 末尾削除。
        for (cur = cell; cur.getNext() != null; cur = cur.getNext()) {
        }
        Assert.assertEquals(answer.remove(answer.size() - 1), cur.remove().get());

        cur = cell;
        Assert.assertNull(cur.getPrevious());
        for (int i = 0; i < answer.size(); i++) {
            Assert.assertEquals(answer.get(i), cur.get());
            cur = cur.getNext();
        }
        Assert.assertNull(cur);

        // 先頭削除。
        cur = cell.getNext();
        Assert.assertEquals(answer.remove(0), cell.remove().get());

        Assert.assertNull(cur.getPrevious());
        for (int i = 0; i < answer.size(); i++) {
            Assert.assertEquals(answer.get(i), cur.get());
            cur = cur.getNext();
        }
        Assert.assertNull(cur);
    }

    /**
     * 連結リストへの挿入の検査。
     */
    @Test
    public void testListAdd() {
        final int length = 10;

        final List<Integer> answer = new LinkedList<>();

        // 通常挿入。
        final LinkedCell<Integer> list = LinkedCell.newLinkedList();
        for (int i = 0; i < length; i++) {
            list.addHead(new LinkedCell<>(i));
            answer.add(0, i);
        }

        LinkedCell<Integer> cur = list;
        for (int i = 0; i < answer.size(); i++) {
            cur = cur.getNext();
            Assert.assertEquals(answer.get(i), cur.get());
        }
        Assert.assertSame(list, cur.getNext());
        Assert.assertSame(cur, list.getPrevious());

        // 途中挿入。
        list.getNext().insertNext(new LinkedCell<>(length));
        answer.add(1, length);

        cur = list;
        for (int i = 0; i < answer.size(); i++) {
            cur = cur.getNext();
            Assert.assertEquals(answer.get(i), cur.get());
        }
        Assert.assertSame(list, cur.getNext());
        Assert.assertSame(cur, list.getPrevious());

        // 末尾挿入。
        list.getPrevious().insertNext(new LinkedCell<>(length + 1));
        answer.add(length + 1);

        cur = list;
        for (int i = 0; i < answer.size(); i++) {
            cur = cur.getNext();
            Assert.assertEquals(answer.get(i), cur.get());
        }
        Assert.assertSame(list, cur.getNext());
        Assert.assertSame(cur, list.getPrevious());
    }

    /**
     * 連結リストでの削除の検査。
     */
    @Test
    public void testListRemove() {
        final int length = 10;

        final List<Integer> answer = new LinkedList<>();

        // リストの用意。
        final LinkedCell<Integer> list = LinkedCell.newLinkedList();
        for (int i = 0; i < length; i++) {
            list.addHead(new LinkedCell<>(i));
            answer.add(0, i);
        }

        // 通常削除。
        Assert.assertEquals(answer.remove(answer.size() - 1), list.removeTail().get());

        LinkedCell<Integer> cur = list;
        for (int i = 0; i < answer.size(); i++) {
            cur = cur.getNext();
            Assert.assertEquals(answer.get(i), cur.get());
        }
        Assert.assertSame(list, cur.getNext());
        Assert.assertSame(cur, list.getPrevious());

        // 先頭削除。
        Assert.assertEquals(answer.remove(0), list.getNext().remove().get());

        cur = list;
        for (int i = 0; i < answer.size(); i++) {
            cur = cur.getNext();
            Assert.assertEquals(answer.get(i), cur.get());
        }
        Assert.assertSame(list, cur.getNext());
        Assert.assertSame(cur, list.getPrevious());

        // 途中削除。
        Assert.assertEquals(answer.remove(answer.size() - 2), list.getPrevious().getPrevious().remove().get());

        cur = list;
        for (int i = 0; i < answer.size(); i++) {
            cur = cur.getNext();
            Assert.assertEquals(answer.get(i), cur.get());
        }
        Assert.assertSame(list, cur.getNext());
        Assert.assertSame(cur, list.getPrevious());

        // 通常の全削除。
        while (true) {
            cur = list.removeTail();
            if (cur == null) {
                Assert.assertEquals(new ArrayList<Integer>(0), answer);
                break;
            }
            Assert.assertEquals(answer.remove(answer.size() - 1), cur.get());
        }
        Assert.assertSame(list, list.getPrevious());
        Assert.assertSame(list, list.getNext());

        // 途中削除での全削除。
        final Random random = new Random();
        final List<LinkedCell<Integer>> cells = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            final LinkedCell<Integer> cell = new LinkedCell<>(random.nextInt());
            list.addHead(cell);
            cells.add(cell);
        }
        Collections.sort(cells, new Comparator<LinkedCell<Integer>>() {
            @Override
            public int compare(final LinkedCell<Integer> o1, final LinkedCell<Integer> o2) {
                return o1.get().compareTo(o2.get());
            }
        });

        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).remove();
        }
        Assert.assertSame(list, list.getPrevious());
        Assert.assertSame(list, list.getNext());

    }

}
