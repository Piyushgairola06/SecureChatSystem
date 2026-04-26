package server;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private ConcurrentHashMap<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long LIMIT = 1000; // 1 second gap

    public boolean allowMessage(String user) {

        long currentTime = System.currentTimeMillis();

        if (!lastMessageTime.containsKey(user)) {
            lastMessageTime.put(user, currentTime);
            return true;
        }

        long lastTime = lastMessageTime.get(user);

        if (currentTime - lastTime < LIMIT) {
            return false;
        }

        lastMessageTime.put(user, currentTime);
        return true;
    }
}
