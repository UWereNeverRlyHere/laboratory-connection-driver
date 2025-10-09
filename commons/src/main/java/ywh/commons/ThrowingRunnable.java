package ywh.commons;


@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
