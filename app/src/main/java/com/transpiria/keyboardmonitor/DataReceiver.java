package com.transpiria.keyboardmonitor;

import java.net.InetAddress;

public interface DataReceiver {
    void DataReceive(byte[] data, int length, InetAddress address);
}
