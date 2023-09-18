package io.thiemann.kurt.query;

import com.fox2code.foxloader.loader.ServerMod;
import com.fox2code.foxloader.network.NetworkPlayer;
import io.thiemann.kurt.query.query.QueryServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.server.PropertyManager;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class QueryModServer extends QueryMod implements ServerMod {

    private QueryServer queryServer;

    @Override
    public void onServerStart(NetworkPlayer.ConnectionType connectionType) {
        MinecraftServer server = MinecraftServer.getInstance();
        PropertyManager props = server.propertyManagerObj;

        boolean queryEnabled = props.getBooleanProperty("enable-query", false);

        if (!queryEnabled) {
            getLogger().info("Query server is disabled");
            return;
        }

        int queryPort = props.getIntProperty("query.port", 25565);
        String queryAddressString = props.getStringProperty("server-ip", "");

        InetAddress inetAddress = null;
        if (queryAddressString.length() > 0) {
            try {
                inetAddress = InetAddress.getByName(queryAddressString);
            } catch (UnknownHostException ignore) {
            }
        }

        try {
            this.queryServer = new QueryServer(server, queryPort, inetAddress);
        } catch (SocketException e) {
            getLogger().warning("Failed to start query server on " + queryAddressString + ":" + queryPort);
            return;
        }

        getLogger().info("Query server started on " + queryAddressString + ":" + queryPort);
    }

    @Override
    public void onServerStop(NetworkPlayer.ConnectionType connectionType) {
        if (this.queryServer != null) {
            this.queryServer.close();
        }
    }
}
