package ro.derbederos.compress;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.github.luben.zstd.util.Native;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ZstdCheckTest {

	private static StreamCodec ZSTD_1 = new StreamCodec("zstd(1) - fastest",
														outStream -> new ZstdOutputStream(outStream, 1),
														ZstdInputStream::new);
	private static StreamCodec ZSTD_5 = new StreamCodec("zstd(5) - fast",
														outStream -> new ZstdOutputStream(outStream, 5),
														ZstdInputStream::new);
	private static StreamCodec ZSTD_11 = new StreamCodec("zstd(11) - normal",
														 outStream -> new ZstdOutputStream(outStream, 11),
														 ZstdInputStream::new);
	private static StreamCodec ZSTD_17 = new StreamCodec("zstd(17) - maximum",
														 outStream -> new ZstdOutputStream(outStream, 17),
														 ZstdInputStream::new);

	private static final Object[][] DATA = {
			{"ZSTD_1 - empty", ZSTD_1, ""},
			{"ZSTD_1 - alabala", ZSTD_1, "alabalaalabalaalabalaalabalaalabalaalabalaalabala"},
			{"ZSTD_5 - empty", ZSTD_5, ""},
			{"ZSTD_5 - alabala", ZSTD_5, "alabalaalabalaalabalaalabalaalabalaalabalaalabala"},
			{"ZSTD_11 - empty", ZSTD_11, ""},
			{"ZSTD_11 - alabala", ZSTD_11, "alabalaalabalaalabalaalabalaalabalaalabalaalabala"},
			{"ZSTD_17 - empty", ZSTD_17, ""},
			{"ZSTD_17 - alabala", ZSTD_17, "alabalaalabalaalabalaalabalaalabalaalabalaalabala"},
			};
	private final StreamCodec streamCodec;
	private final String input;

	public ZstdCheckTest(String name, StreamCodec streamCodec, String input) {
		this.streamCodec = streamCodec;
		this.input = input;
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] data() {
		return DATA;
	}

	@Test
	public void testZstdLoad() {
		Native.load();
	}

	@Test
	public void testCompressDecompress() throws IOException {
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		assertThat(streamCodec.decompress(streamCodec.compress(bytes), bytes.length), CoreMatchers.is(bytes));
	}
}