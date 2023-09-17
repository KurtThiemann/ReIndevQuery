package io.thiemann.kurt.query.query;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.server.MinecraftServer;
import java.net.*;
import java.util.Collection;


public class QueryServer extends AbstractQueryServer {
    private final MinecraftServer server;

    public QueryServer(MinecraftServer server, int port, InetAddress laddr) throws SocketException {
        super(port, laddr);
        this.server = server;

    }

    @Override
    protected int getMinecraftServerPort() {
        return this.server.propertyManagerObj.getIntProperty("server-port", 25565);
    }

    @Override
    protected String getMinecraftServerIp() {
        String address = this.server.propertyManagerObj.getStringProperty("server-ip", "");
        if (address.length() == 0) {
            address = "127.0.0.1";
        }
        return address;
    }

    @Override
    protected String getMotd() {
        return this.server.getMotd();
    }

    @Override
    protected int getPlayersOnline() {
        return this.server.configManager.playersOnline();
    }

    @Override
    protected int getMaxPlayers() {
        return this.server.configManager.getMaxPlayers();
    }

    @Override
    protected String[] getPlayerList() {
        return this.server.configManager.playerEntities.stream().map(NetworkPlayer::getPlayerName).toArray(String[]::new);
    }

    @Override
    protected String getPluginInfo() {
        Collection<ModContainer> mods = ModLoader.getModContainers();
        String[] modNames = mods.stream().map(mod -> mod.name + " " + mod.version).toArray(String[]::new);

        return "FoxLoader " + BuildConfig.FOXLOADER_VERSION + ": " + String.join("; ", modNames);
    }

    @Override
    protected String getGameId() {
        return "MINECRAFT";
    }

    @Override
    protected String getGameVersion() {
        return "Beta 1.7";
    }
}
