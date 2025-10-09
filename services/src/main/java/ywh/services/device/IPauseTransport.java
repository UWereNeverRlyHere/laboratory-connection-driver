package ywh.services.device;

import ywh.services.data.enums.SpecialBytes;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface IPauseTransport {

    CompletableFuture<Void> send(byte[] data);
    CompletableFuture<Void> send(byte data);
    CompletableFuture<Void> send(SpecialBytes data);
    void setSendPause(Duration pause);
}
