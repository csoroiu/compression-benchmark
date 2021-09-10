package ro.derbederos.compress;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.time.StopWatch;

import ro.derbederos.compress.lab.ShannonEntropy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        byte[] data = readInputFile(INPUT_FILE, 1 << 29);
        double entropy = ShannonEntropy.entropy(data);
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

    private static GzipParameters getGzipParameters(int compressionLevel) {
        GzipParameters result = new GzipParameters();
        result.setCompressionLevel(compressionLevel);
        return result;
    }

    private static LZ4Compressor getLZ4Parameters(int compressionLevel) {
        return LZ4Factory.fastestInstance().highCompressor(compressionLevel);
    }


    private static void initCodecs() {
        codecs = new ArrayList<>();

//        BrotliLibraryLoader.loadBrotli();
//        codecs.add(new StreamCodec("Brotli(1)",
//                outputStream -> new BrotliOutputStream(outputStream, getBrotliParameters(1)),
//                BrotliInputStream::new));
//        codecs.add(new StreamCodec("Brotli(2)",
//                outputStream -> new BrotliOutputStream(outputStream, getBrotliParameters(2)),
//                BrotliInputStream::new));
//        codecs.add(new StreamCodec("Brotli(3)",
//                outputStream -> new BrotliOutputStream(outputStream, getBrotliParameters(3)),
//                BrotliInputStream::new));
//        codecs.add(new StreamCodec("Brotli(4)",
//                outputStream -> new BrotliOutputStream(outputStream, getBrotliParameters(4)),
//                BrotliInputStream::new));
//        codecs.add(new StreamCodec("Brotli(6)",
//                outputStream -> new BrotliOutputStream(outputStream, getBrotliParameters(6)),
//                BrotliInputStream::new));
//        codecs.add(new StreamCodec("Brotli(9)",
//                outputStream -> new BrotliOutputStream(outputStream, getBrotliParameters(9)),
//                BrotliInputStream::new));
        codecs.add(new StreamCodec("zstd(1) - fastest",
                outStream -> new ZstdOutputStream(outStream, 1),
                ZstdInputStream::new));
        codecs.add(new StreamCodec("zstd(5) - fast",
                outStream -> new ZstdOutputStream(outStream, 5),
                ZstdInputStream::new));
        codecs.add(new StreamCodec("zstd(11) - normal",
                outStream -> new ZstdOutputStream(outStream, 11),
                ZstdInputStream::new));
        codecs.add(new StreamCodec("zstd(17) - maximum",
                outStream -> new ZstdOutputStream(outStream, 17),
                ZstdInputStream::new));
        codecs.add(new StreamCodec("java-Gzip",
                GZIPOutputStream::new,
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Gzip(fast)",
                out -> new GzipCompressorOutputStream(out, getGzipParameters(Deflater.BEST_SPEED)),
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Gzip(default)",
                GzipCompressorOutputStream::new,
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Gzip(best)",
                out -> new GzipCompressorOutputStream(out, getGzipParameters(Deflater.BEST_COMPRESSION)),
                GZIPInputStream::new));
        codecs.add(new StreamCodec("Lz4(default/fast)",
                LZ4BlockOutputStream::new,
                LZ4BlockInputStream::new));
        codecs.add(new StreamCodec("Lz4(4)",
                out -> new LZ4BlockOutputStream(out, 1 << 16, getLZ4Parameters(4)),
                LZ4BlockInputStream::new));
        codecs.add(new StreamCodec("Lz4(6) - normal",
                out -> new LZ4BlockOutputStream(out, 1 << 16, getLZ4Parameters(6)),
                LZ4BlockInputStream::new));
        codecs.add(new StreamCodec("Lz4(9) - maximum",
                out -> new LZ4BlockOutputStream(out, 1 << 16, getLZ4Parameters(9)),
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

    private static byte[] readInputFile(String inputFile, int maxSize) throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        File f = new File(inputFile);
        int size = (int) (Math.min(f.length(), Integer.MAX_VALUE - 8));
        size = Math.min(size, maxSize);
        System.out.printf("Reading file %s (%d bytes).%n", inputFile, size);
        byte b[] = new byte[size];
        IOUtils.readFully(new FileInputStream(f), b, 0, size);
        System.out.println(stopWatch.getTime(TimeUnit.MILLISECONDS));
        return b;
    }
}
