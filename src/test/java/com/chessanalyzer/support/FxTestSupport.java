package com.chessanalyzer.support;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Starts the JavaFX toolkit once per JVM and runs code on the FX thread. Tests skip when no display exists. */
public final class FxTestSupport {

    private static volatile Boolean available;

    private FxTestSupport() {
    }

    /** True if a JavaFX toolkit could be started (needs a display; use xvfb-run on a headless Linux box). */
    public static synchronized boolean start() {
        if (available == null) {
            try {
                boolean linuxWithoutDisplay = System.getProperty("os.name", "").toLowerCase().contains("linux")
                        && System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null;
                if (linuxWithoutDisplay) {
                    available = false; // run under xvfb-run on a headless machine
                } else {
                    CountDownLatch ready = new CountDownLatch(1);
                    try {
                        Platform.startup(ready::countDown);
                    } catch (IllegalStateException alreadyRunning) {
                        ready.countDown();
                    }
                    available = ready.await(15, TimeUnit.SECONDS);
                    Platform.setImplicitExit(false);
                }
            } catch (Throwable t) {
                available = false;
            }
        }
        return available;
    }

    public static <T> T onFx(Callable<T> task) throws Exception {
        FutureTask<T> f = new FutureTask<>(task);
        Platform.runLater(f);
        try {
            return f.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof Exception ex) throw ex;
            if (e.getCause() instanceof Error err) throw err;
            throw e;
        }
    }

    public static void onFx(Runnable task) throws Exception {
        onFx(() -> { task.run(); return null; });
    }
}
