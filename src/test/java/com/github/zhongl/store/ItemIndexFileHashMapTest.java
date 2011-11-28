package com.github.zhongl.store;

import com.github.zhongl.store.benchmark.*;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class ItemIndexFileHashMapTest extends FileBase {

    private ItemIndexFileHashMap map;

    @Override
    public void tearDown() throws Exception {
        if (map != null) map.close();
        super.tearDown();
    }

    @Test
    public void putAndGet() throws Exception {
        file = testFile("putAndGet");
        map = newItemIndexFileHashMap(100);

        Md5Key key = Md5Key.valueOf("key".getBytes());
        ItemIndex itemIndex = new ItemIndex(0, 18L);
        assertThat(map.put(key, itemIndex), is(nullValue()));
        assertThat(map.get(key), is(itemIndex));
    }

    @Test
    public void remove() throws Exception {
        file = testFile("remove");
        map = newItemIndexFileHashMap(100);

        Md5Key key = Md5Key.valueOf("key".getBytes());
        ItemIndex itemIndex = new ItemIndex(3, 29L);
        assertThat(map.put(key, itemIndex), is(nullValue()));
        assertThat(map.remove(key), is(itemIndex));
        assertThat(map.get(key), is(nullValue()));
    }

    @Test
    public void benchmark() throws Exception {
        file = testFile("benchmark");
        map = newItemIndexFileHashMap(100);

        CallableFactory addFactory = new AddFactory(map);
        CallableFactory replaceFactory = new ReplaceFactory(map);
        CallableFactory getFactory = new GetFactory(map);
        CallableFactory removeFactory = new RemoveFactory(map);


        CallableFactory concatCallableFactory = new ConcatCallableFactory(
                new FixInstanceSizeFactory(100, addFactory),
                new FixInstanceSizeFactory(100, replaceFactory),
                new FixInstanceSizeFactory(100, getFactory),
                new FixInstanceSizeFactory(100, removeFactory)
        );

        Collection<Statistics> statisticses = new Benchmarker(concatCallableFactory, 1, 400).benchmark();
        for (Statistics statisticse : statisticses) {
            System.out.println(statisticse);
        }
    }

    @Test
    public void put141ItemIndexInOneBucket() throws Exception {
        file = testFile("put141ItemIndexInOneBucket");
        map = newItemIndexFileHashMap(1);

        for (int i = 0; i < 141; i++) {
            map.put(Md5Key.valueOf(Ints.toByteArray(i)), new ItemIndex(0, 0L));
        }
    }

    @Test
    public void putItemIndexInReleasedSlot() throws Exception {
        file = testFile("putItemIndexInReleasedSlot");
        map = newItemIndexFileHashMap(1);

        for (int i = 0; i < 141; i++) {
            map.put(Md5Key.valueOf(Ints.toByteArray(i)), new ItemIndex(0, 0L));
        }

        Md5Key key0 = Md5Key.valueOf(Ints.toByteArray(0));
        Md5Key key1 = Md5Key.valueOf(Ints.toByteArray(1));
        ItemIndex itemIndex = new ItemIndex(0, 0L);
        map.remove(key0);
        map.remove(key1);
        assertThat(map.put(key1, itemIndex), is(nullValue()));
        assertThat(map.put(key0, itemIndex), is(nullValue())); // no exception means new item index put in released slot.
    }

    @Test
    public void getAndRemoveEmplyBucket() throws Exception {
        file = testFile("getAndRemoveEmplyBucket");
        map = newItemIndexFileHashMap(1);

        Md5Key key = Md5Key.valueOf(Ints.toByteArray(1));
        assertThat(map.get(key), is(nullValue()));
        assertThat(map.remove(key), is(nullValue()));
    }

    @Test
    public void getAndRemoveByInvalidKey() throws Exception {
        file = testFile("getAndRemoveByInvalidKey");
        map = newItemIndexFileHashMap(1);

        for (int i = 0; i < 141; i++) {
            map.put(Md5Key.valueOf(Ints.toByteArray(i)), new ItemIndex(0, 0L));
        }

        Md5Key invalidKey = Md5Key.valueOf(Ints.toByteArray(141));

        assertThat(map.get(invalidKey), is(nullValue()));
        assertThat(map.remove(invalidKey), is(nullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void unknownSlotState() throws Exception {
        file = testFile("unknownSlotState");
        map = newItemIndexFileHashMap(1);
        Files.write(new byte[] {3}, file);  // write a unknown slot state
        map.get(Md5Key.valueOf(Ints.toByteArray(1))); // trigger exception
    }

    private ItemIndexFileHashMap newItemIndexFileHashMap(int buckets) throws IOException {
        int initCapacity = 4 * 1024 * buckets;
        return new ItemIndexFileHashMap(file, initCapacity);
    }

    abstract class OperationFactory implements CallableFactory {
        protected final ItemIndexFileHashMap map;
        private int count;

        public OperationFactory(ItemIndexFileHashMap map) {this.map = map;}

        protected byte[] genKey() {
            return Ints.toByteArray(count++);
        }
    }

    private class AddFactory extends OperationFactory {

        public AddFactory(ItemIndexFileHashMap map) {
            super(map);
        }

        @Override
        public Callable<?> create() {
            return new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return map.put(Md5Key.valueOf(genKey()), genItemIndex());
                }
            };
        }

        protected ItemIndex genItemIndex() {
            return new ItemIndex(0, 0L);
        }

    }

    private class ReplaceFactory extends AddFactory {

        public ReplaceFactory(ItemIndexFileHashMap map) {
            super(map);
        }

        @Override
        public Callable<?> create() {
            return new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return map.put(Md5Key.valueOf(genKey()), genItemIndex());
                }
            };
        }


    }

    private class GetFactory extends OperationFactory {

        public GetFactory(ItemIndexFileHashMap map) {
            super(map);
        }

        @Override
        public Callable<?> create() {
            return new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return map.get(Md5Key.valueOf(genKey()));
                }
            };
        }

    }

    private class RemoveFactory extends OperationFactory {

        public RemoveFactory(ItemIndexFileHashMap map) {
            super(map);
        }

        @Override
        public Callable<?> create() {
            return new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return map.remove(Md5Key.valueOf(genKey()));
                }
            };
        }

    }
}
