package au.steve.hyperlauncher.compat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Reads DT_NEEDED from stripped ARM64 ELF files without loading native code. */
public final class ElfDependencies {
    private ElfDependencies() {}
    private static void check(byte[] data, long offset, long size) throws IOException {
        if (offset < 0 || size < 0 || offset > data.length || size > data.length - offset)
            throw new IOException("ELF range is outside the file");
    }
    public static List<String> read(byte[] data) throws IOException {
        check(data, 0, 64);
        if (data[0] != 0x7f || data[1] != 'E' || data[2] != 'L' || data[3] != 'F'
                || data[4] != 2 || data[5] != 1)
            throw new IOException("Expected little-endian ELF64");
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getShort(18) != 183) throw new IOException("Expected ARM64 ELF");
        long phoff = b.getLong(32);
        int phsize = Short.toUnsignedInt(b.getShort(54));
        int count = Short.toUnsignedInt(b.getShort(56));
        if (phsize < 56 || count == 0xffff) throw new IOException("Unsupported program header layout");
        check(data, phoff, (long) phsize * count);
        List<long[]> loads = new ArrayList<>();
        long dynamic = -1, dynamicSize = 0;
        for (int i = 0; i < count; i++) {
            int p = (int) (phoff + (long) i * phsize);
            int type = b.getInt(p);
            long offset = b.getLong(p + 8), vaddr = b.getLong(p + 16), size = b.getLong(p + 32);
            if (type == 1 || type == 2) check(data, offset, size);
            if (type == 1) loads.add(new long[]{vaddr, offset, size});
            if (type == 2) { dynamic = offset; dynamicSize = size; }
        }
        if (dynamic == -1) return new ArrayList<>();
        long strtab = -1, strsize = -1;
        List<Long> needed = new ArrayList<>();
        for (long p = dynamic; p + 16 <= dynamic + dynamicSize; p += 16) {
            long tag = b.getLong((int) p), value = b.getLong((int) p + 8);
            if (tag == 0) break;
            if (tag == 1) needed.add(value);
            if (tag == 5) strtab = value;
            if (tag == 10) strsize = value;
        }
        if (needed.isEmpty()) return new ArrayList<>();
        if (strtab < 0 || strsize < 0) throw new IOException("ELF dynamic string table missing");
        long stringOffset = -1;
        for (long[] load : loads) {
            if (strtab >= load[0] && strtab - load[0] < load[2]) {
                long delta = strtab - load[0];
                if (strsize > load[2] - delta) throw new IOException("ELF string table exceeds load segment");
                stringOffset = load[1] + delta;
                break;
            }
        }
        if (stringOffset < 0) throw new IOException("ELF string table not mapped");
        check(data, stringOffset, strsize);
        TreeSet<String> result = new TreeSet<>();
        for (long relative : needed) {
            if (relative < 0 || relative >= strsize) throw new IOException("Invalid dependency string offset");
            int start = (int) (stringOffset + relative), end = start;
            long limit = stringOffset + strsize;
            while (end < limit && data[end] != 0) end++;
            if (end == limit) throw new IOException("Unterminated dependency string");
            result.add(new String(data, start, end - start, StandardCharsets.UTF_8));
        }
        return new ArrayList<>(result);
    }
}
