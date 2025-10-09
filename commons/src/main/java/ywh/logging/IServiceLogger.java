package ywh.logging;

public interface IServiceLogger {
    void log(String msg);
    void error(String msg);
    void error(String msg, Throwable ex);

}
