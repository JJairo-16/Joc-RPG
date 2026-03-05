package utils.ui;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LoadingDots implements AutoCloseable {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;
    private final PrintStream out;

    public LoadingDots() {
        this(System.out, 350, new String[] { "Loading   ", "Loading.  ", "Loading.. ", "Loading..." });
    }

    public LoadingDots(PrintStream out, long frameMillis, String[] frames) {
        this.out = out;

        this.worker = new Thread(() -> {
            int i = 0;
            int n = frames.length;

            while (running.get()) {
                // \r vuelve al inicio de la línea, sin salto
                out.print("\r" + frames[i++ % n]);
                out.flush();

                try {
                    Thread.sleep(frameMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "loading-dots");

        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void close() {
        running.set(false);
        worker.interrupt();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        out.print("\r" + " ".repeat(32) + "\r");
        out.flush();
    }
}