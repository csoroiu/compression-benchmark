package ro.derbederos.compress;


import org.apache.commons.compress.utils.IOUtils;

import java.io.*;

public class StreamCodec implements Codec {

    private IOStreamFunction<OutputStream, OutputStream> compressor;
    private IOStreamFunction<InputStream, InputStream> decompressor;
    private String name;

    StreamCodec(String name, IOStreamFunction<OutputStream, OutputStream> compressor, IOStreamFunction<InputStream, InputStream> decompressor) {
        this.name = name;
        this.compressor = compressor;
        this.decompressor = decompressor;
    }

    @Override
    public String getName() {
        return name;
    }

    public byte[] compress(byte[] data) throws IOException {
        ByteArrayInputStream decompressedStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream result = new ByteArrayOutputStream(data.length / 5);
        try (OutputStream os = compressor.apply(result)) {
            IOUtils.copy(decompressedStream, os);
        }
        return result.toByteArray();
    }

    public byte[] decompress(byte[] data, int decompressedSizeEstimate) throws IOException {
        if (decompressedSizeEstimate == -1) {
            decompressedSizeEstimate = (int) Math.min((long)data.length * 5, Integer.MAX_VALUE - 8);
        }
        ByteArrayInputStream compressedStream = new ByteArrayInputStream(data);
        byte result[] = new byte[decompressedSizeEstimate];
        try (InputStream is = decompressor.apply(compressedStream)) {
            IOUtils.readFully(is, result);
        }
        return result;
    }
}
