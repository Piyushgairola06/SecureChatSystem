package server;

import java.util.*;

public class ChannelManager {

    private Map<String, Set<String>> channels = new HashMap<>();

    public void joinChannel(String user, String channel) {
        channels.putIfAbsent(channel, new HashSet<>());
        channels.get(channel).add(user);
    }

    public void leaveChannel(String user, String channel) {
        if (channels.containsKey(channel)) {
            channels.get(channel).remove(user);
        }
    }

    public Set<String> getUsers(String channel) {
        return channels.getOrDefault(channel, new HashSet<>());
    }
}
