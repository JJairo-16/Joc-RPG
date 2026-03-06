package utils.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class Cleaner {
    public Cleaner() {
        cls = getCls();
    }

    private ProcessBuilder cls;
    private static final int DEFAULT_AUX = 20;

    private final ExecutorService textExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cleaner-text-generator");
        t.setDaemon(true);
        return t;
    });

    /** Obté el método per netejar la consola. */
    private static ProcessBuilder getCls() {
        boolean isWindows = System.getProperty("os.name", "").startsWith("Windows");
        ProcessBuilder processBuilder;

        if (isWindows) {
            processBuilder = new ProcessBuilder("cmd.exe", "/d", "/q", "/c", "cls");
        } else {
            processBuilder = new ProcessBuilder("clear");
        }

        return processBuilder.inheritIO();
    }

    /** Neteja la consola. En cas d'error, escriu líneas per emular-ho. */
    public void clear(int aux) {
        try {
            cls.start().waitFor();
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) { // ? ignored
        }

        emulate(aux); // ? Emulació
    }

    /**
     * Neteja la consola. En cas d'error, escriu {@value Cleaner#DEFAULT_AUX} líneas
     * per emular-ho.
     */
    public void clear() {
        clear(DEFAULT_AUX);
    }

    private void emulate(int space) {
        if (space <= 0)
            return;
        System.out.print("\n".repeat(space));
    }

    /**
     * Neteja la pantalla mentre es genera el text.
     *
     * El text es genera en paral·lel mentre s'executa el {@code clear}.
     * Quan ambdues operacions finalitzen, el text s'imprimeix.
     *
     * @param textSupplier codi que genera el text
     * @param newLine      si {@code true} usa println, si no print
     */
    private void clearAndPrint(Supplier<String> textSupplier, boolean newLine) {
        Future<String> future = textExecutor.submit(textSupplier::get);
        String text = "";

        try {
            cls.start().waitFor();
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            emulate(DEFAULT_AUX);
        }

        if (!future.isCancelled()) {
            try {
                text = future.get();
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
            } catch (java.util.concurrent.ExecutionException e) {
                text = "";
            }
        }

        System.out.print(text);
        if (newLine) {
            System.out.println();
        }
    }

    /**
     * Neteja la pantalla mentre es genera el text.
     *
     * El text es genera en paral·lel mentre s'executa el {@code clear}.
     * Quan ambdues operacions finalitzen, el text s'imprimeix.
     *
     * @param textSupplier codi que genera el text
     */
    public void clearAndPrintln(Supplier<String> textSupplier) {
        clearAndPrint(textSupplier, true);
    }

    /**
     * Neteja la pantalla mentre es genera el text.
     *
     * El text es genera en paral·lel mentre s'executa el {@code clear}.
     * Quan ambdues operacions finalitzen, el text s'imprimeix.
     *
     * @param textSupplier codi que genera el text
     */
    public void clearAndPrint(Supplier<String> textSupplier) {
        clearAndPrint(textSupplier, false);
    }
}
