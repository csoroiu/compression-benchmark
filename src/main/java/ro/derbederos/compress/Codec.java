package ro.derbederos.compress;

import java.io.IOException;

interface Codec {

    String getName();

    byte[] compress(byte[] data) throws IOException;

    byte[] decompress(byte[] data, int decompressedSizeEstimate) throws IOException;
}
