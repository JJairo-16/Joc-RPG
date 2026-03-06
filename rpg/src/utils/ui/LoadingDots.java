package utils.ui;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animació simple de "Loading..." a la consola.
 *
 * <p>
 * Mostra punts animats en una mateixa línia utilitzant {@code \r}.
 * Funciona bé amb {@code try-with-resources} gràcies a {@link AutoCloseable}.
 * </p>
 */
public final class LoadingDots implements AutoCloseable {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;
    private final PrintStream out;

    /**
     * Constructor amb configuració per defecte.
     */
    public LoadingDots() {
        this(
                System.out,
                350,
                new String[] { "Loading   ", "Loading.  ", "Loading.. ", "Loading..." });
    }

    /**
     * Crea una animació de càrrega personalitzada.
     *
     * @param out         flux de sortida
     * @param frameMillis temps entre fotogrames
     * @param frames      seqüència de textos a mostrar
     */
    public LoadingDots(PrintStream out, long frameMillis, String[] frames) {
        this.out = out;

        this.worker = new Thread(() -> {
            int i = 0;
            int n = frames.length;

            while (running.get()) {
                // \r torna a l'inici de la línia sense salt
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

    /**
     * Atura l'animació i neteja la línia de consola.
     */
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