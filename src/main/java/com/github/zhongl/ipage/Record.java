package com.github.zhongl.ipage;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * {@link Record} is wrapper of bytes, a minimized store unit.
 *
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
@ThreadSafe
public final class Record {
    public static final int LENGTH_BYTES = 4;
    private final ByteBuffer buffer;

    public static Record readFrom(ByteBuffer byteBuffer) throws IOException {
        int length = byteBuffer.getInt();
        int limit = byteBuffer.position() + length;
        byteBuffer.limit(limit);
        return new Record(byteBuffer.slice());
    }

    public Record(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public Record(byte[] bytes) {
        buffer = ByteBuffer.wrap(bytes);
    }

    public int length() {
        return buffer.limit();
    }

    public int writeTo(ByteBuffer buffer) throws IOException {
        this.buffer.position(0);
        buffer.putInt(length()).put(this.buffer);
        return LENGTH_BYTES + length();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        if (buffer != null ? !buffer.equals(record.buffer) : record.buffer != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return buffer != null ? buffer.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{buffer=").append(buffer);
        sb.append('}');
        return sb.toString();
    }

    public void writeTo(RandomAccessFile randomAccessFile) {
        throw new UnsupportedOperationException();
    }

    public static Record readFrom(RandomAccessFile randomAccessFile) {
        throw new UnsupportedOperationException();
    }

    byte[] toBytes() {
        if (buffer.isDirect()) {
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            return bytes;
        } else {
            return buffer.array();
        }
    }
}
