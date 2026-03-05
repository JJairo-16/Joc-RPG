package utils.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Caché per a textos "wrappejats".
 *
 * <p>
 * Evita recalcular el wrap de textos que es mostren repetidament
 * en la UI de terminal.
 * </p>
 *
 * <p>
 * Aquesta implementació és una LRU (Least Recently Used): quan s'arriba al
 * límit
 * d'entrades, s'expulsen automàticament les menys utilitzades per evitar que la
 * caché creixi massa.
 * </p>
 */
public class TextWrapCache {

    private record Key(String text, int width) {
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final int maxEntries;

    /**
     * Caché LRU: accessOrder=true per expulsar les entrades menys recents.
     */
    private final Map<Key, List<String>> cache;

    /**
     * Crea una caché amb un límit d'entrades per defecte.
     *
     * <p>
     * Si vols un valor diferent segons el teu joc, utilitza
     * {@link #TextWrapCache(int)}.
     * </p>
     */
    public TextWrapCache() {
        this(256); // valor per defecte raonable per UI de terminal
    }

    /**
     * Crea una caché amb límit d'entrades.
     *
     * @param maxEntries nombre màxim d'entrades que mantindrà la caché (mínim 1)
     */
    public TextWrapCache(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);

        // Capacitat inicial aproximada per evitar rehash; +1 per marge.
        int initialCapacity = (int) Math.ceil(this.maxEntries / 0.75) + 1;

        this.cache = new LinkedHashMap<Key, List<String>>(initialCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, List<String>> eldest) {
                return size() > TextWrapCache.this.maxEntries;
            }
        };
    }

    /**
     * Retorna el text amb wrap aplicat, utilitzant caché si ja s'ha calculat.
     */
    public List<String> get(String text, int width) {
        if (text == null)
            return List.of();

        final String trimmed = text.trim();
        if (trimmed.isEmpty())
            return List.of();

        final Key key = new Key(trimmed, width);

        // Implementació manual per evitar computeIfAbsent (que faria wrap dins el lock
        // si més endavant sincronitzes). Ara mateix és simple i ràpida.
        List<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        List<String> wrapped = wrap(trimmed, width);
        cache.put(key, wrapped);
        return wrapped;
    }

    /**
     * Permet buidar la caché manualment.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * (Opcional) Retorna el nombre d'entrades actuals (útil per debug).
     */
    public int size() {
        return cache.size();
    }

    /**
     * Algoritme simple de wrap.
     */
    private List<String> wrap(String text, int maxWidth) {

        if (text.length() <= maxWidth) {
            return List.of(text);
        }

        final String[] words = WHITESPACE.split(text);

        final int capacity = Math.clamp(
                Math.max(1, text.length() / Math.max(1, maxWidth)),
                1,
                words.length);

        final ArrayList<String> lines = new ArrayList<>(capacity);
        final StringBuilder line = new StringBuilder(Math.min(text.length(), maxWidth));

        for (String word : words) {

            if (line.isEmpty()) {
                line.append(word);
                continue;
            }

            if (line.length() + 1 + word.length() <= maxWidth) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }
}