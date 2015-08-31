package com.transpiria.keyboardmonitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ThreadedDatagram {

    public DatagramSocket Socket;

    public ThreadedDatagram()
            throws SocketException {
        this(0);
    }

    public ThreadedDatagram(int port)
            throws SocketException {
        Socket = new DatagramSocket(port);
    }

    public void BeginReceive(ReceiveDataSubscriber callback) {
        new Thread(new ReceiveDatagram(this, callback)).start();
    }

    public void BeginSend(byte[] content, InetAddress address, int port) {
        new Thread(new SendDatagram(this, content, address, port)).start();
    }

    public class ReceiveDatagram implements Runnable {

        public ThreadedDatagram Socket;
        public ReceiveDataSubscriber Callback;

        public ReceiveDatagram(ThreadedDatagram socket) {
            Socket = socket;
        }

        public ReceiveDatagram(ThreadedDatagram socket, ReceiveDataSubscriber dataReceiver) {
            this(socket);
            Callback = dataReceiver;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[512];
            DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
            try {
                Socket.Socket.receive(pack);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Callback != null) {
                Callback.DataReceived(Socket, buffer, pack.getLength(), pack.getAddress());
            }
        }
    }

    public class SendDatagram implements Runnable {

        public SendDataSubscriber Callback;
        public ThreadedDatagram Socket;
        public byte[] Content;
        public InetAddress Address;
        public int Port;

        public SendDatagram(ThreadedDatagram socket, byte[] content, InetAddress address, int port) {
            Socket = socket;
            Content = content;
            Address = address;
            Port = port;
        }

        public SendDatagram(ThreadedDatagram socket, byte[] content, InetAddress address, int port, SendDataSubscriber callback) {
            this(socket, content, address, port);
            Callback = callback;
        }

        @Override
        public void run() {
            byte[] buffer = Content;
            DatagramPacket pack = new DatagramPacket(buffer, buffer.length, Address, Port);
            try {
                Socket.Socket.send(pack);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Callback != null) {
                Callback.DataSent(Socket);
            }
        }
    }

}
