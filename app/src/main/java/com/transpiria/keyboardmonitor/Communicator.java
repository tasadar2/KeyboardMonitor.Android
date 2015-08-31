package com.transpiria.keyboardmonitor;

import android.support.annotation.NonNull;
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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * Created by Josh on 5/17/2015.
 */
public class Communicator implements ReceiveDataSubscriber {

    public interface IEndpointDiscovered {
        void EndpointDiscovered(InetAddress ipAddress, int port);
    }

    public interface IMessageReceived {
        void MessageReceived(byte[] content);
    }

    public final Event<IEndpointDiscovered> EndpointDiscoveredEvent = new Event<>();
    public final Event<IMessageReceived> IMessageReceivedEvent = new Event<>();

    private final short EndMessage = 0x512B;
    private final byte[] EndCommandBytes = ByteBuffer.allocate(2).putShort(EndMessage).array();

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

    protected void OnEndpointDiscovered(InetAddress ipAddress, int port) {
        for (IEndpointDiscovered observer : EndpointDiscoveredEvent.Observers) {
            observer.EndpointDiscovered(ipAddress, port);
        }
    }

    protected void OnMessageReceived(byte[] content) {
        for (IMessageReceived observer : IMessageReceivedEvent.Observers) {
            observer.MessageReceived(content);
        }
    }

    public void Discover(int port) {
        try {
            ThreadedDatagram data = new ThreadedDatagram();
            data.Socket.setBroadcast(true);

            for (BroadcastInfo broadcastInfo : GetBroadcastInformation()) {
                byte[] content = GenerateCommand(MessageType.Discover);
                data.BeginSend(content, broadcastInfo.BroadcastAddress, port);
            }

        } catch (SocketException ex) {
            ex.printStackTrace();
        }

    }

    public void Subscribe(InetAddress ipAddress, int port) {
        byte[] content = GenerateCommand(MessageType.Subscribe);
        Send(content, ipAddress, port);
    }

    public void Unsubscribe(InetAddress ipAddress, int port) {
        byte[] content = GenerateCommand(MessageType.Unsubscribe);
        Send(content, ipAddress, port);
    }

    private byte[] GenerateCommand(MessageType messageType) {
        ByteBuffer command = ByteBuffer.allocate(6);
        command.put(messageType.Bytes);
        command.putShort((short) ListenerPort);
        command.put(EndCommandBytes);
        return command.array();
    }

    private Iterable<BroadcastInfo> GetBroadcastInformation() {
        ArrayList<BroadcastInfo> info = new ArrayList<>();

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
                            if (bInfo.BroadcastAddress != null) {
                                info.add(bInfo);
                            }
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
        if (length > 0) {
            ProcessData(address, data, length);
        }

        socket.BeginReceive(this);
    }

    private void ProcessData(InetAddress address, byte[] data, int length) {
        if (length > 2) {
            long hash;
            ByteBuffer buffer = ByteBuffer.wrap(data);

            short command = buffer.getShort();

            // Discovered
            // FF 16		Start
            // FF FF        Port
            // 51 2B		End
            if (command == MessageType.Discovered.Value) {
                int port = buffer.getShort();
                OnEndpointDiscovered(address, port);
            }

            //  Message
            //  FF 19		Start
            //  FF FF FF FF	Message Identifier
            //  FF FF FF FF	Message Length
            //  FF FF		Message Parts
            //  FF FF		Message Part
            //  FF FF FF FF	Message Part Start
            //  FF FF		Message Part Length
            //  FF .. .. FF	Message Part Content
            //  51 2B		End
            else if (command == MessageType.Message.Value) {
                BuildReceivedMessage(buffer, length);
            }
        }
    }

    private HashMap<Integer, Message> Messages = new HashMap<>();

    private void BuildReceivedMessage(ByteBuffer buffer, int length) {
        if (length > 20) {
            int messageIdentifier = buffer.getInt();
            long messageLength = buffer.getInt();
            int messageParts = buffer.getShort();
            int messagePart = buffer.getShort();
            long messagePartStart = buffer.getInt();
            int messagePartLength = buffer.getShort();

            if (length >= 0x14 + messagePartLength && buffer.getShort(0x14 + messagePartLength) == EndMessage) {
                Message message;
                message = Messages.get(messageIdentifier);
                if (message == null) {
                    Messages.put(messageIdentifier, message = new Message(messageIdentifier, messageLength, messageParts));
                }

                message.Parts.remove(messagePart);
                buffer.get(message.Content, (int) messagePartStart, messagePartLength);

                if (message.Parts.isEmpty()) {
                    Messages.remove(messageIdentifier);
                    OnMessageReceived(message.Content);
                }
            }
        }
    }

    public class Message {
        public final int Identifier;
        public final byte[] Content;
        public final long Length;
        public final HashSet<Integer> Parts;

        public Message(int identifier, long length, int parts) {
            Identifier = identifier;
            Content = new byte[(int) length];
            Length = length;
            Parts = new HashSet<>(parts);
            for (int part = 0; part < parts; part++) {
                Parts.add(part);
            }
        }
    }

    private void Send(byte[] content, InetAddress address, int port) {
        try {
            ThreadedDatagram data = new ThreadedDatagram();
            data.BeginSend(content, address, port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

}

