package com.transpiria.keyboardmonitor;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Created by Josh on 5/17/2015.
 */
public class Communicator implements ReceiveDataSubscriber {

    public DatagramSocket Socket;
    public int ListenerPort;

    public Communicator() {
        try {
            ThreadedDatagram data = new ThreadedDatagram();
            Socket = data.Socket;
            ListenerPort = data.Socket.getLocalPort();
            data.BeginReceive(this);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void Discover(int port) {
        try {
            ThreadedDatagram socket = new ThreadedDatagram();
            socket.Socket.setBroadcast(true);

            for (BroadcastInfo broadcastInfo : GetBroadcastInformation()) {
                byte[] content;

            }

        } catch (SocketException ex) {
            ex.printStackTrace();
        }

//            var content = GenerateCommand(MessageType.Discover, broadcastInfo.Address);
//            LoggerInstance.LogWriter.DebugFormat("Sending Discover to {0}", new IPEndPoint(broadcastInfo.BroadcastAddress, port));
//            Send(socket, content, new IPEndPoint(broadcastInfo.BroadcastAddress, port));
    }

    private Iterable<BroadcastInfo> GetBroadcastInformation() {
        ArrayList<BroadcastInfo> info = new ArrayList<BroadcastInfo>();

        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface i = e.nextElement();
                if (i.isUp()) {
                    for (InterfaceAddress interfaceAddress : i.getInterfaceAddresses()) {
                        InetAddress address = interfaceAddress.getAddress();
                        if (address instanceof Inet4Address) {
                            BroadcastInfo bInfo = new BroadcastInfo();
                            interfaceAddress.getNetworkPrefixLength();
                            bInfo.Address = address;
                            bInfo.BroadcastAddress = interfaceAddress.getBroadcast();
                            info.add(bInfo);
                        }

                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        return info;
    }

    public class BroadcastInfo {
        public InetAddress Address;
        public InetAddress ListenAddress;
        public InetAddress BroadcastAddress;
    }


    @Override
    public void DataReceived(ThreadedDatagram socket, byte[] data, int length, InetAddress address) {

    }

}

