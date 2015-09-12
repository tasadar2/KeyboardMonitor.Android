package com.transpiria.keyboardmonitor;

import java.nio.ByteBuffer;

public enum MessageType {
    Discover(0xff15),
    Discovered(0xff16),
    Subscribe(0xff17),
    Unsubscribe(0xff18),
    Message(0xff19),
    FramesPerSecond(0xff1a);

    public final short Value;
    public final byte[] Bytes;

    MessageType(int value) {
        this((short) value);
    }

    MessageType(short value) {
        Value = value;
        Bytes = ByteBuffer.allocate(2).putShort(value).array();
    }
}
