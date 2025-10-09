package ywh.logging;

public interface IAppLogger {
     void log(String msg);
     void info(String msg);
     void error(String msg);
     void error(String msg, Throwable ex);
     void close();
}
