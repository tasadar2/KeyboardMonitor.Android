package com.transpiria.keyboardmonitor;

import java.net.InetAddress;

public interface ReceiveDataSubscriber {
    void DataReceived(ThreadedDatagram socket, byte[] data, int length, InetAddress address);
}


