package ro.derbederos.compress;

import java.io.IOException;

@FunctionalInterface
public interface IOStreamFunction<T, R> {
    R apply(T t) throws IOException;
}
