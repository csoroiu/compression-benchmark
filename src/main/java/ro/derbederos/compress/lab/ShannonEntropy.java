package ro.derbederos.compress.lab;

import static java.lang.Byte.toUnsignedInt;

public class ShannonEntropy {
    public static double entropy(byte[] data) {
        //computing the histogram
        int[] frequencies = computeFrequencies(data);
        //computing the entropy out of histogram: -sum(pi * log2 pi)
        double entropy = 0;
        double total = data.length;
        double log2 = Math.log(2);
        //entropy = -Sum(probi * log2 (probi))
        for (int frequency : frequencies) {
            if (frequency != 0) {
                entropy += frequency * Math.log(frequency);
            }
        }
        entropy /= total;
        entropy -= Math.log(total);
        entropy *= -1 / log2;
        return entropy;
    }

    private static int[] computeFrequencies(byte[] data) {
        int[] frequencies = new int[256];
        for (byte byteValue : data) {
            int intValue = toUnsignedInt(byteValue);
            frequencies[intValue]++;
        }
        return frequencies;
    }
}
