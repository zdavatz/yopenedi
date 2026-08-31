package com.ywesee.java.yopenedi.common;

/**
 * Central place for all network timeouts.
 *
 * Every value can be overridden at runtime with a system property, so timeouts
 * can be tuned on the server without a rebuild, e.g.
 *
 *   java -Dyopenedi.http.readTimeout=120 -jar email-fetcher-1.0-all.jar ...
 *
 * All values are in seconds. A value of 0 means "wait forever" and should only
 * ever be used to deliberately restore the old behaviour.
 */
public class Timeouts {
    /** Establishing a TCP/TLS connection. */
    public static final int CONNECT = intProperty("yopenedi.connectTimeout", 30);

    /** Waiting for data on an established connection (per read). */
    public static final int READ = intProperty("yopenedi.readTimeout", 120);

    /** Waiting for a blocked write to drain (per write). */
    public static final int WRITE = intProperty("yopenedi.writeTimeout", 120);

    /** HTTP POST of a converted document (AS2 endpoints etc.). */
    public static final int HTTP_CONNECT = intProperty("yopenedi.http.connectTimeout", CONNECT);
    public static final int HTTP_READ = intProperty("yopenedi.http.readTimeout", READ);

    /** SSH keepalive: probe every SSH_ALIVE_INTERVAL s, give up after SSH_ALIVE_COUNT misses. */
    public static final int SSH_ALIVE_INTERVAL = intProperty("yopenedi.ssh.aliveInterval", 30);
    public static final int SSH_ALIVE_COUNT = intProperty("yopenedi.ssh.aliveCount", 3);

    /**
     * Hard upper bound for one whole run of the email fetcher. When exceeded the
     * process dumps all thread stacks and exits, so a stuck run cannot linger
     * forever and cron runs cannot pile up on top of each other.
     */
    public static final int MAX_RUNTIME = intProperty("yopenedi.maxRuntime", 1800);

    public static int millis(int seconds) {
        return seconds * 1000;
    }

    static int intProperty(String name, int fallback) {
        String value = System.getProperty(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Ignoring invalid value for " + name + ": " + value);
            return fallback;
        }
    }
}
