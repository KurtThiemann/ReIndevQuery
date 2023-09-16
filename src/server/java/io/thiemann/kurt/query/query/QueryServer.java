package io.thiemann.kurt.query.query;

import net.minecraft.server.MinecraftServer;
import org.luaj.vm2.ast.Str;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

//Async udp server to handle Minecraft query requests
public class QueryServer {
    private static final byte[] challenge = "9513307\0".getBytes();
    private final MinecraftServer server;
    private final DatagramSocket socket;
    private final Thread thread;

    public QueryServer(MinecraftServer server, int port, InetAddress laddr) throws SocketException {
        this.server = server;
        this.socket = new DatagramSocket(port, laddr);
        this.thread = new Thread(this::run);
        this.thread.start();
    }

    private void run() {
        while (!this.socket.isClosed()) {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.socket.receive(packet);
                this.handlePacket(packet);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Handle incoming packet
     * <a href="https://wiki.vg/Query#Client_to_Server_Packet_Format">Packet format</a>
     *
     * @param packet incoming packet
     */
    private void handlePacket(DatagramPacket packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        buffer.order(ByteOrder.BIG_ENDIAN);
        short magic = buffer.getShort();
        if (magic != (short)0xFEFD) {
            return;
        }
        byte type = buffer.get();
        int sessionId = buffer.getInt();
        if (type == 9) {
            //send handshake response
            byte[] response = makeHandshakeResponse(sessionId);
            DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
            try {
                this.socket.send(responsePacket);
            } catch (IOException ignored) {}
        }

        if(type != 0) {
            return;
        }

        //get payload as byte array
        byte[] payload = new byte[packet.getLength() - 7];
        boolean isFull = payload.length == 8;

        byte[] response;
        if(isFull) {
            //send full stat response
            response = makeFullStatResponse(sessionId);
        } else {
            //send basic stat response
            response = makeBasicStatResponse(sessionId);
        }

        DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
        try {
            this.socket.send(responsePacket);
        } catch (IOException ignored) {}
    }

    /**
     * Generate a Handshake response
     * <a href="https://wiki.vg/Query#Response">Packet format</a>
     * Instead of an actual challenge we just send a constant value
     *
     * @param sessionId session id
     * @return payload
     */
    private byte[] makeHandshakeResponse(int sessionId) {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte)9);
        buffer.putInt(sessionId);
        buffer.put(challenge);
        return buffer.array();
    }

    /**
     * Generate a basic stat response
     * <a href="https://wiki.vg/Query#Response_2">Packet format</a>
     *
     * @param sessionId session id
     * @return payload
     */
    private byte[] makeBasicStatResponse(int sessionId) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte)0);
        buffer.putInt(sessionId);
        buffer.put((this.server.getMotd() + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.put("SMP\0".getBytes());
        buffer.put("world\0".getBytes());
        buffer.put((this.server.configManager.playersOnline() + "\0").getBytes());
        buffer.put((this.server.configManager.getMaxPlayers() + "\0").getBytes());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short)this.getMinecraftServerPort());
        buffer.put((this.getMinecraftServerIp() + "\0").getBytes());

        //only return written length as byte array
        byte[] response = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(response);
        return response;
    }

    /**
     * Generate a full stat response
     * <a href="https://wiki.vg/Query#Response_3">Packet format</a>
     *
     * @param sessionId session id
     * @return payload
     */
    private byte[] makeFullStatResponse(int sessionId) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte)0);
        buffer.putInt(sessionId);
        buffer.put(new byte[] {0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, (byte)0x80, 0x00});

        buffer.put("hostname\0".getBytes());
        buffer.put((this.server.getMotd() + "\0").getBytes(StandardCharsets.UTF_8));

        buffer.put("gametype\0".getBytes());
        buffer.put("SMP\0".getBytes());

        buffer.put("game_id\0".getBytes());
        buffer.put("MINECRAFT\0".getBytes());

        buffer.put("version\0".getBytes());
        buffer.put("Beta 1.7\0".getBytes());

        buffer.put("plugins\0".getBytes());
        buffer.put("FoxLoader\0".getBytes());

        buffer.put("map\0".getBytes());
        buffer.put("world\0".getBytes());

        buffer.put("numplayers\0".getBytes());
        buffer.put((this.server.configManager.playersOnline() + "\0").getBytes());

        buffer.put("maxplayers\0".getBytes());
        buffer.put((this.server.configManager.getMaxPlayers() + "\0").getBytes());

        buffer.put("hostport\0".getBytes());
        buffer.put((this.getMinecraftServerPort() + "\0").getBytes());

        buffer.put("hostip\0".getBytes());
        buffer.put((this.getMinecraftServerIp() + "\0").getBytes());

        buffer.put((byte)0x00);

        buffer.put(new byte[] {0x01, 0x70, 0x6C, 0x61, 0x79, 0x65, 0x72, 0x5F, 0x00, 0x00});

        this.server.configManager.playerEntities.forEach((player) -> {
            System.out.println(player.getPlayerName());
            buffer.put((player.getPlayerName() + "\0").getBytes(StandardCharsets.UTF_8));
        });


        buffer.put((byte)0x00);

        //only return written length as byte array
        byte[] response = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(response);
        return response;
    }

    private int getMinecraftServerPort() {
        return this.server.propertyManagerObj.getIntProperty("server-port", 25565);
    }

    private String getMinecraftServerIp() {
        String address = this.server.propertyManagerObj.getStringProperty("server-ip", "");
        if (address.length() == 0) {
            address = "127.0.0.1";
        }
        return address;
    }

    public void close() {
        this.socket.close();
        this.thread.interrupt();
    }
}
