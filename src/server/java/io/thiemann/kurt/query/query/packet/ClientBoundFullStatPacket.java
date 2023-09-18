package io.thiemann.kurt.query.query.packet;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class ClientBoundFullStatPacket extends ClientBoundPacket {

    private final String hostName;
    private final String gameType;
    private final String gameId;
    private final String version;
    private final String plugins;
    private final String map;
    private final int numPlayers;
    private final int maxPlayers;
    private final int port;
    private final String hostIp;
    private final String[] players;

    public ClientBoundFullStatPacket(int sessionId, String hostName, String gameType, String gameId, String version, String plugins, String map, int numPlayers, int maxPlayers, int port, String hostIp, String[] players) {
        super(PacketType.STAT, sessionId);
        this.hostName = hostName;
        this.gameType = gameType;
        this.gameId = gameId;
        this.version = version;
        this.plugins = plugins;
        this.map = map;
        this.numPlayers = numPlayers;
        this.maxPlayers = maxPlayers;
        this.port = port;
        this.hostIp = hostIp;
        this.players = players;
    }

    private void putKV(String key, String value, ByteBuffer buffer) {
        buffer.put(this.encodeString(key));
        buffer.put(this.encodeString(value));
    }

    @Override
    protected byte[] serializePayload() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(new byte[]{0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, (byte) 0x80, 0x00});

        this.putKV("hostname", this.hostName, buffer);
        this.putKV("gametype", this.gameType, buffer);
        this.putKV("game_id", this.gameId, buffer);
        this.putKV("version", this.version, buffer);
        this.putKV("plugins", this.plugins, buffer);
        this.putKV("map", this.map, buffer);
        this.putKV("numplayers", String.valueOf(this.numPlayers), buffer);
        this.putKV("maxplayers", String.valueOf(this.maxPlayers), buffer);
        this.putKV("hostport", String.valueOf(this.port), buffer);
        this.putKV("hostip", this.hostIp, buffer);
        buffer.put((byte) 0);

        buffer.put(new byte[]{0x01, 0x70, 0x6C, 0x61, 0x79, 0x65, 0x72, 0x5F, 0x00, 0x00});

        for (String player : this.players) {
            buffer.put(this.encodeString(player));
        }
        buffer.put((byte) 0);

        byte[] response = new byte[buffer.position()];
        ((Buffer) buffer).rewind();
        buffer.get(response);
        return response;
    }
}
