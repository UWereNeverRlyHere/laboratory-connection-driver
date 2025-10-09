package ywh.services.port_sender.impl;


import ywh.services.port_sender.IPortSender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpClientSenderImpl implements IPortSender {
    private final String ip;
    private final int port;

    public TcpClientSenderImpl(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void send(byte[] data) {

        try (Socket socket = new Socket(ip, port);
             //BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
          /*  OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            BufferedWriter writer = new BufferedWriter(out);*/
             DataInputStream inFromClient = new DataInputStream(socket.getInputStream());
             DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("Client connected to socket.");
            System.out.println();
            System.out.println("Client writing channel = oos & reading channel = ois initialized.");
            System.out.println("Sending data to server side - STARTED..");
            outToClient.write(data);
            outToClient.flush();
            System.out.println("Sending data to server side - DONE.");
            System.out.println("Closing connections & channels on client Side - DONE.");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
