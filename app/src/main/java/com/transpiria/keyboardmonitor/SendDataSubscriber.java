package com.transpiria.keyboardmonitor;

public interface SendDataSubscriber {
    void DataSent(ThreadedDatagram socket);
}
