package server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 3 – Group channel management.
 * Channels are created on first /join and destroyed when empty.
 */
public class ChannelManager {

    // channel name → set of member usernames
    private final ConcurrentHashMap<String, Set<String>> channels = new ConcurrentHashMap<>();

    public void joinChannel(String channel, String username) {
        channels.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(username);
        ServerLogger.log("[Channel] " + username + " joined #" + channel);
    }

    public void leaveChannel(String channel, String username) {
        Set<String> members = channels.get(channel);
        if (members != null) {
            members.remove(username);
            if (members.isEmpty()) {
                channels.remove(channel);
                ServerLogger.log("[Channel] #" + channel + " disbanded (empty)");
            }
        }
    }

    /** Remove a user from every channel they're in (called on disconnect). */
    public void leaveAllChannels(String username) {
        for (String channel : channels.keySet()) {
            leaveChannel(channel, username);
        }
    }

    public Set<String> getMembers(String channel) {
        return channels.getOrDefault(channel, ConcurrentHashMap.newKeySet());
    }

    public boolean isMember(String channel, String username) {
        Set<String> members = channels.get(channel);
        return members != null && members.contains(username);
    }

    public String getChannelList() {
        if (channels.isEmpty()) return "(no active channels)";
        StringBuilder sb = new StringBuilder();
        channels.forEach((ch, members) ->
            sb.append("#").append(ch).append("(").append(members.size()).append(") "));
        return sb.toString().trim();
    }
}
