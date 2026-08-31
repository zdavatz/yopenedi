package com.ywesee.java.yopenedi.common;

import java.util.Map;

/**
 * Last line of defence against a stuck run.
 *
 * Starts a daemon thread that, after the configured budget, dumps the stack of
 * every thread and kills the JVM. The dump means a hang documents itself in the
 * log instead of needing someone to catch the process alive with jstack.
 */
public class Watchdog {
    /** Exit code used when the watchdog kills the process. */
    public static final int EXIT_CODE = 3;

    public static void start(String jobName, int budgetSeconds) {
        if (budgetSeconds <= 0) {
            System.out.println("Watchdog disabled for " + jobName);
            return;
        }
        System.out.println("Watchdog armed for " + jobName + ": " + budgetSeconds + "s");
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(Timeouts.millis(budgetSeconds));
            } catch (InterruptedException e) {
                return;
            }
            System.err.println("=== WATCHDOG: " + jobName + " exceeded " + budgetSeconds
                    + "s, killing process. Thread dump follows. ===");
            dumpThreads();
            System.err.flush();
            System.out.flush();
            // halt(), not exit(): shutdown hooks could block on the same thing we are stuck on.
            Runtime.getRuntime().halt(EXIT_CODE);
        }, "yopenedi-watchdog");
        t.setDaemon(true);
        t.start();
    }

    public static void dumpThreads() {
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            System.err.println("\"" + thread.getName() + "\" state=" + thread.getState()
                    + (thread.isDaemon() ? " daemon" : ""));
            for (StackTraceElement frame : entry.getValue()) {
                System.err.println("\tat " + frame);
            }
            System.err.println();
        }
    }
}
