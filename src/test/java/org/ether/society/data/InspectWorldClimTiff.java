package org.ether.society.data;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class InspectWorldClimTiff {
    @Test
    public void inspect() throws Exception {
        File file = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_1.tif");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[8];
            raf.readFully(header);
            boolean isLittleEndian = (header[0] == 'I' && header[1] == 'I');
            long ifdOffset = isLittleEndian
                    ? (header[4] & 0xFFL) | ((header[5] & 0xFFL) << 8) | ((header[6] & 0xFFL) << 16) | ((header[7] & 0xFFL) << 24)
                    : ((header[4] & 0xFFL) << 24) | ((header[5] & 0xFFL) << 16) | ((header[6] & 0xFFL) << 8) | (header[7] & 0xFFL);

            System.out.println("TIFF endian: " + (isLittleEndian ? "LittleEndian" : "BigEndian") + ", IFD offset: " + ifdOffset);
            raf.seek(ifdOffset);
            int numEntries = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8))
                                            : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());
            System.out.println("IFD num entries: " + numEntries);

            for (int i = 0; i < numEntries; i++) {
                byte[] entry = new byte[12];
                raf.readFully(entry);
                ByteBuffer bb = ByteBuffer.wrap(entry).order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                int tag = bb.getShort() & 0xFFFF;
                int type = bb.getShort() & 0xFFFF;
                long count = bb.getInt() & 0xFFFFFFFFL;
                long valOrOffset = bb.getInt() & 0xFFFFFFFFL;
                System.out.printf("  Tag %d: type=%d, count=%d, valOrOffset=%d%n", tag, type, count, valOrOffset);
            }
        }
    }
}
