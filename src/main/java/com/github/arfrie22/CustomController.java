package com.github.arfrie22;

import com.fazecast.jSerialComm.SerialPort;
import edu.wpi.first.networktables.NetworkTableEntry;

import java.util.Arrays;

public class CustomController {
    SerialPort port;
    int protocolVersion;
    int teamNumber;

    public static boolean isCustomController(SerialPort port) throws InterruptedException {
        port.openPort();
        byte[] buf = new byte[64];
        Arrays.fill(buf, (byte) 1);
        port.writeBytes(new byte[]{(byte) 0x0FD}, 1);
        Thread.sleep(100);
        port.readBytes(buf, 64, 0);
        port.closePort();
        return port.getPortDescription().contentEquals(new StringBuffer(new String(buf).substring(1, port.getPortDescription().length() + 1)));
    }

    public static CustomController scan() throws InterruptedException {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (isCustomController(port)) {
                return new CustomController(port);
            }
        }

        return null;
    }

    public CustomController(SerialPort port) throws InterruptedException {
        this.port = port;
        port.openPort();
        byte[] buf = new byte[64];

        port.writeBytes(new byte[]{(byte) 0x01}, 1);
        Thread.sleep(100);
        port.readBytes(buf, 64, 0);
        this.protocolVersion = (Byte.toUnsignedInt(buf[1]) << 8) | Byte.toUnsignedInt(buf[0]);
        System.out.println("Protocol version: " + protocolVersion);

        port.writeBytes(new byte[]{(byte) 0x02}, 1);
        Thread.sleep(100);
        port.readBytes(buf, 64, 0);
        this.teamNumber = (Byte.toUnsignedInt(buf[1]) << 24) | (Byte.toUnsignedInt(buf[2]) << 16) | (Byte.toUnsignedInt(buf[3]) << 8) | Byte.toUnsignedInt(buf[4]);
        System.out.println("Team number: " + teamNumber);

        port.closePort();
    }

    public void open(NetworkTableEntry responseEntry, NetworkTableEntry hasResponseEntry) {
        port.openPort();
        NTSerialDataListener ntSerialDataListener = new NTSerialDataListener(this, responseEntry, hasResponseEntry);
        port.addDataListener(ntSerialDataListener);
    }

    public void send(byte[] bytes) {
        port.writeBytes(bytes, bytes.length);
    }

    public void close() {
        port.removeDataListener();
        port.closePort();
    }

    public boolean isOpen() {
        return port.isOpen();
    }
}
