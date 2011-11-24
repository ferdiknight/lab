package com.github.zhongl.store;

import com.google.common.io.ByteProcessor;
import com.google.common.io.Files;
import com.google.common.primitives.Longs;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link com.github.zhongl.store.Page} File structure :
 * <ul>
 * <p/>
 * <li>{@link com.github.zhongl.store.Page} = bytesCapacity:4bytes {@link com.github.zhongl.store.Item}* </li>
 * <li>{@link com.github.zhongl.store.Item} = length:4bytes bytes</li>
 * </ul>
 *
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
@NotThreadSafe
public class Page implements Closeable {
    public static final int SKIP_CAPACITY_BYTES = 8;
    private final File file;
    private final long bytesCapacity;
    private final Appender appender;
    private final Getter getter;

    private Page(File file, long bytesCapacity) throws IOException {
        this.file = file;
        this.bytesCapacity = bytesCapacity;
        getter = new Getter();
        appender = new Appender();
    }

    public Appender appender() {
        return appender;
    }

    public Getter getter() {
        return getter;
    }

    public static Builder openOn(File file) {
        return new Builder(file);
    }

    public void close() throws IOException {
        appender.close();
        getter.close();
    }

    public long bytesCapacity() {
        return bytesCapacity;
    }

    private abstract class Operator implements Closeable {
        protected final RandomAccessFile randomAccessFile;

        public Operator(RandomAccessFile randomAccessFile) {
            this.randomAccessFile = randomAccessFile;
        }

        @Override
        public void close() throws IOException {
            randomAccessFile.close();
        }
    }


    public class Appender extends Operator {

        private Appender() throws IOException {
            super(new RandomAccessFile(file, "rw"));
            if (randomAccessFile.length() < bytesCapacity) {
                randomAccessFile.seek(randomAccessFile.length());
            }
        }

        /**
         * Append item to the page.
         * <p/>
         * Caution: item will not sync to disk util {@link com.github.zhongl.store.Page.Appender#flush()} invoked.
         *
         * @param item {@link Item}
         *
         * @return offset of the {@link com.github.zhongl.store.Item}.
         * @throws OverflowException if no remains for new item.
         * @throws IOException
         */
        public long append(Item item) throws IOException {
            checkOverFlowIfAppend(item.length());
            long offset = randomAccessFile.getFilePointer() - SKIP_CAPACITY_BYTES;
            item.writeTo(randomAccessFile);
            return offset;
        }

        /**
         * Sync pendings of {@link com.github.zhongl.store.Page} to disk.
         *
         * @throws IOException
         */
        public void flush() throws IOException {
            randomAccessFile.getChannel().force(true);
        }

        private void checkOverFlowIfAppend(int length) throws IOException {
            long appendedLength = randomAccessFile.getFilePointer() + length + Item.LENGTH_BYTES;
            if (appendedLength > bytesCapacity) throw new OverflowException();
        }

    }

    public class Getter extends Operator {

        private Getter() throws IOException {
            super(new RandomAccessFile(file, "r"));
        }

        /**
         * Get a {@link com.github.zhongl.store.Item} by offset from a {@link com.github.zhongl.store.Page}.
         *
         * @param offset
         *
         * @return
         * @throws IOException
         */
        public Item get(long offset) throws IOException {
            seek(offset);
            return Item.readFrom(randomAccessFile);
        }

        private void seek(long offset) throws IOException {
            randomAccessFile.seek(SKIP_CAPACITY_BYTES + offset);
        }
    }

    public static class Builder {
        public static final long DEFAULT_BYTES_CAPACITY = 64 * 1024 * 1024; // 64M;

        private final File file;

        private long bytesCapacity = DEFAULT_BYTES_CAPACITY;
        private boolean create = false;
        private boolean overwrite = false;

        private Builder(File file) {
            this.file = file;
        }

        /** {@link com.github.zhongl.store.Page} will be create if it does not exist. */
        public Builder createIfNotExist() {
            create = true;
            return this;
        }

        /** {@link com.github.zhongl.store.Page} will be overwrote if it already exists. */
        public Builder overwriteIfExist() {
            overwrite = true;
            return this;
        }

        /**
         * {@link com.github.zhongl.store.Page} will use this value as it capacity of bytes only if it is new one or overwrite.
         *
         * @param value should not less than {@link com.github.zhongl.store.Page#SKIP_CAPACITY_BYTES} + {@link com.github.zhongl.store.Item#LENGTH_BYTES},
         *              default is 67108864 (64M).
         */
        public Builder bytesCapacity(long value) {
            long minValue = Page.SKIP_CAPACITY_BYTES + Item.LENGTH_BYTES;
            checkArgument(value >= minValue, "value should not less than %s", minValue);
            bytesCapacity = value;
            return this;
        }

        public Page build() throws IOException {
            if (file.exists()) {
                checkArgument(file.isFile(), "%s exists but is not a file.", file);
                if (overwrite) format();
                else loadBytesCapacity();
            } else {
                checkArgument(create, "Can't build a page on %s, because it does not exist.", file);
                Files.createParentDirs(file);
                format();
            }

            return new Page(file, bytesCapacity);
        }

        private void loadBytesCapacity() throws IOException {
            bytesCapacity = Files.readBytes(file, new LoadBytesCapacityByteProcessor());
        }

        private void format() throws IOException {
            Files.write(Longs.toByteArray(bytesCapacity), file); // write item size 0 to the file.
        }

        private static class LoadBytesCapacityByteProcessor implements ByteProcessor<Long> {
            private byte[] buf;

            @Override
            public boolean processBytes(byte[] buf, int off, int len) throws IOException {
                this.buf = buf;
                return false;
            }

            @Override
            public Long getResult() {
                return Longs.fromByteArray(buf);
            }
        }
    }
}
