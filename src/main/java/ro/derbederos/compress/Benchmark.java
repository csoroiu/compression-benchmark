package ro.derbederos.compress;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Benchmark {

    private static final String INPUT_FILE = "corpus.bin";

    private static List<Codec> codecs;

    public static void main(String[] args) throws IOException {
        initCodecs();
        byte[] data = readInputFile(INPUT_FILE);
        double entropy = entropy(data);
        System.out.println("Entropy of data is: " + entropy);
        System.out.println("Estimated compressed size: " + (int) Math.ceil(entropy * data.length / 8L));
        for (Codec codec : codecs) {
            try {
                benchmark(data, codec);
            } catch (IOException e) {
                System.err.println("Compression test failed");
                e.printStackTrace();
            }
            System.gc();
        }
    }

    private static GzipParameters getGzipParamaters(int compressionLevel) {
        GzipParameters result = new GzipParameters();
        result.setCompressionLevel(compressionLevel);
        return result;
    }

    private static LZ4Compressor getLZ4Paramaters(int compressionLevel) {
        return LZ4Factory.fastestInstance().highCompressor(compressionLevel);
    }

    private static void initCodecs() {
        codecs = new ArrayList<>();
        codecs.add(new StreamCodec("java-Gzip",
                GZIPOutputStream::new,
                GZIPInputStream::new));
//        codecs.add(new StreamCodec("Brotli(default)",
//                Brot::new,
//                BrotliInputStream::new));
        codecs.add(new StreamCodec("Gzip(fast)",
                out -> new GzipCompressorOutputStream(out, getGzipParamaters(Deflater.BEST_SPEED)),
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Gzip(default)",
                GzipCompressorOutputStream::new,
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Gzip(best)",
                out -> new GzipCompressorOutputStream(out, getGzipParamaters(Deflater.BEST_COMPRESSION)),
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Lz4(default/fast)",
                LZ4BlockOutputStream::new,
                LZ4BlockInputStream::new));
        codecs.add(new StreamCodec("Lz4(4)",
                out -> new LZ4BlockOutputStream(out, 1 << 16, getLZ4Paramaters(4)),
                LZ4BlockInputStream::new));
        codecs.add(new StreamCodec("Lz4(6)",
                out -> new LZ4BlockOutputStream(out, 1 << 16, getLZ4Paramaters(6)),
                LZ4BlockInputStream::new));
        codecs.add(new StreamCodec("Lz4(9)",
                out -> new LZ4BlockOutputStream(out, 1 << 16, getLZ4Paramaters(9)),
                LZ4BlockInputStream::new));
    }

    private static void benchmark(byte[] testData, Codec codec) throws IOException {
        System.out.print(codec.getName());
        StopWatch stopWatchCompress = StopWatch.createStarted();
        byte[] compressedData = codec.compress(testData);
        stopWatchCompress.stop();

        StopWatch stopWatchDecompress = StopWatch.createStarted();
        codec.decompress(compressedData, testData.length);
        stopWatchDecompress.stop();
        System.out.printf(",%d,%d,%d\n",
                compressedData.length,
                stopWatchCompress.getTime(TimeUnit.MILLISECONDS),
                stopWatchDecompress.getTime(TimeUnit.MILLISECONDS)
        );
    }

    private static byte[] readInputFile(String inputFile) throws IOException {
        System.out.println("Reading file " + inputFile);
        StopWatch stopWatch = StopWatch.createStarted();
        File f = new File(inputFile);
        ByteArrayOutputStream os = new ByteArrayOutputStream((int) f.length());
        try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
            IOUtils.copy(is, os);
        }
        System.out.println(stopWatch.getTime(TimeUnit.MILLISECONDS));
        return os.toByteArray();
    }

    private static double entropy(byte[] data) {
        int[] frequencies = new int[256];
        for (byte byteValue : data) {
            int intValue = ((int) byteValue) & 0xFF;
            frequencies[intValue]++;
        }
        double entropy = 0;
        double total = data.length;
        double log2 = Math.log(2);
        for (int frequency : frequencies) {
            if (frequency != 0) {
                // calculate the probability of a particular byte occuring
                double probabilityOfByte = (double) frequency / total;
                // calculate the next value to sum to previous entropy calculation
                double value = probabilityOfByte * (Math.log(probabilityOfByte) / log2);
                entropy = entropy + value;
            }
        }
        entropy *= -1;
        return entropy;
    }
}