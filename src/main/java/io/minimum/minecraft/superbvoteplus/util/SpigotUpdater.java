package io.minimum.minecraft.superbvoteplus.util;

import com.google.common.io.ByteStreams;
import io.minimum.minecraft.superbvoteplus.SuperbVotePlus;;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SpigotUpdater implements Runnable, Listener {
    private String newVersion;

    private static String getLatestVersion() throws IOException {
        URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=11626");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "SuperbVotePlus/" + SuperbVotePlus.getPlugin().getDescription().getVersion());
        String version;
        try (InputStream is = connection.getInputStream()) {
            version = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        }

        if (version.length() >= 32) {
            // Probably not a version number, and more than likely an error.
            return null;
        }

        return version;
    }

    @Override
    public void run() {
        String myVersion = SuperbVotePlus.getPlugin().getDescription().getVersion();
        if (myVersion.endsWith("-SNAPSHOT")) {
            // Nothing to do.
            return;
        }

        try {
            newVersion = getLatestVersion();
        } catch (IOException e) {
            SuperbVotePlus.getPlugin().getLogger().warning("Unable to contact SpigotMC.org to determine if a new version is available.");
        }

        if (newVersion != null && !newVersion.equals(myVersion)) {
            SuperbVotePlus.getPlugin().getLogger().info("A new version of SuperbVotePlus (" + newVersion + ") is available.");
            SuperbVotePlus.getPlugin().getLogger().info("Download it from https://www.spigotmc.org/resources/superbvote.11626/");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        String myVersion = SuperbVotePlus.getPlugin().getDescription().getVersion();
        if (newVersion != null && !newVersion.equals(myVersion) && e.getPlayer().hasPermission("superbvote.admin")) {
            Bukkit.getScheduler().runTaskLater(SuperbVotePlus.getPlugin(), () -> {
                e.getPlayer().sendMessage("");
                e.getPlayer().sendMessage(ChatColor.YELLOW + "A new version of SuperbVotePlus (" + newVersion + ") is available.");
                e.getPlayer().sendMessage(ChatColor.YELLOW + "Download it from https://www.spigotmc.org/resources/superbvote.11626/");
                e.getPlayer().sendMessage("");
            }, 100);
        }
    }
}
