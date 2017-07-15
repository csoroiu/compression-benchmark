package ro.derbederos.crc;

import java.util.zip.Checksum;

/*
 * http://en.wikipedia.org/wiki/Cyclic_redundancy_check
 * http://reveng.sourceforge.net/crc-catalogue/
 */
public class CRC32 implements Checksum {

    final private int lookupTable[] = new int[0x100];
    final private int poly;
    final private int initialValue;
    final private boolean refIn; // reflect input data bytes
    final private boolean refOut; // resulted sum needs to be reversed before xor
    final private int xorOut;
    private int crc;

    public CRC32(int poly, int initialValue,
                 boolean refIn, boolean refOut, int xorOut) {
        this.poly = poly;
        this.initialValue = initialValue;
        this.refIn = refIn;
        this.refOut = refOut;
        this.xorOut = xorOut;
        if (refIn) {
            initLookupTableReflected();
        } else {
            initLookupTableUnreflected();
        }
    }

    private void initLookupTableReflected() {
        int poly = Integer.reverse(this.poly);
        for (int i = 0; i < 0x100; i++) {
            int v = i;
            for (int j = 0; j < 8; j++) {
                if ((v & 1) == 1) {
                    v = (v >>> 1) ^ poly;
                } else {
                    v = v >>> 1;
                }
            }
            lookupTable[i] = v;
        }
    }

    private void initLookupTableUnreflected() {
        int poly = this.poly;
        for (int i = 0; i < 0x100; i++) {
            int v = i << 24;
            for (int j = 0; j < 8; j++) {
                if ((v & 0x8000000000000000L) != 0) {
                    v = (v << 1) ^ poly;
                } else {
                    v = (v << 1);
                }
            }
            lookupTable[i] = v;
        }
    }

    public void reset() {
        if (refIn) {
            crc = Integer.reverse(initialValue);
        } else {
            crc = initialValue;
        }
    }

    public void update(int b) {
        if (refIn) {
            crc = (crc >>> 8) ^ lookupTable[(crc ^ b) & 0xff];
        } else {
            crc = (crc << 8) ^ lookupTable[((crc >>> 24) ^ b) & 0xff];
        }
    }

    public void update(byte src[]) {
        update(src, 0, src.length);
    }

    public void update(byte src[], int offset, int len) {
        for (int i = offset; i < offset + len; i++) {
            byte value = src[i];
            update(value);
        }
    }

    public long getValue() {
        if (refOut == refIn) {
            return ((long) (crc ^ xorOut)) & 0xFFFFFFFFL;
        } else {
            return ((long) (Integer.reverse(crc) ^ xorOut)) & 0xFFFFFFFFL;
        }
    }
}