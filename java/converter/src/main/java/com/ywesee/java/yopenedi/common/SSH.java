package com.ywesee.java.yopenedi.common;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Shared JSch hardening. JSch defaults to no timeout at all: a connection that
 * dies silently (NAT drop, firewall, hung peer) leaves get/put/ls blocked forever.
 */
public class SSH {
    public static void applyTimeouts(Session session) throws JSchException {
        // SO_TIMEOUT on the session socket, so every read on it can fail instead of hanging.
        session.setTimeout(Timeouts.millis(Timeouts.READ));
        // Detect a peer that went away without closing the TCP connection.
        session.setServerAliveInterval(Timeouts.millis(Timeouts.SSH_ALIVE_INTERVAL));
        session.setServerAliveCountMax(Timeouts.SSH_ALIVE_COUNT);
    }

    /** Never throws, so it is safe to call from a finally block. */
    public static void disconnect(Channel channel, Session session) {
        if (channel != null) {
            try {
                channel.disconnect();
            } catch (Exception e) {
                System.out.println("Error while closing SFTP channel: " + e);
            }
        }
        if (session != null) {
            try {
                session.disconnect();
            } catch (Exception e) {
                System.out.println("Error while closing SSH session: " + e);
            }
        }
    }
}
