package utils.cache;

import java.util.Arrays;

import models.weapons.Arsenal;

/**
 * Memòria cau de "cards" d'armes, indexada per una clau compacta.
 *
 * <p>
 * La clau combina l'arma ({@link Arsenal#ordinal()}) i dues banderes booleanes:
 * {@code showEquipTag} i {@code equippable}.
 * </p>
 */
public final class WeaponCardCache {

    /** Mida exacta necessària: armes * combinacions de banderes (2 booleans = 4). */
    private static final int CACHE_SIZE = Arsenal.values().length * 4;

    private final String[] cache = new String[CACHE_SIZE];

    /**
     * Calcula la clau de memòria cau a partir de l'arma i banderes.
     *
     * @param w            arma (no pot ser {@code null})
     * @param showEquipTag si s'ha de mostrar l'etiqueta d'equipament
     * @param equippable   si l'arma és equipable
     * @return clau enter per indexar la memòria cau
     * @throws IllegalArgumentException si {@code w} és {@code null}
     */
    public int keyOf(Arsenal w, boolean showEquipTag, boolean equippable) {
        if (w == null) {
            throw new IllegalArgumentException("weapon null");
        }

        int key = w.ordinal();
        key = (key << 1) | (showEquipTag ? 1 : 0);
        key = (key << 1) | (equippable ? 1 : 0);

        return key;
    }

    /**
     * Recupera una card de la memòria cau.
     *
     * @param key clau de la memòria cau
     * @return la card, o {@code null} si no existeix o la clau és fora de rang
     */
    public String cardOf(int key) {
        return (key >= 0 && key < cache.length) ? cache[key] : null;
    }

    /**
     * Desa una card a la memòria cau.
     *
     * @param key  clau de la memòria cau
     * @param card contingut de la card
     */
    public void save(int key, String card) {
        if (key < 0 || key >= cache.length) {
            return;
        }
        cache[key] = card;
    }

    /**
     * Buida completament la memòria cau.
     */
    public void clear() {
        Arrays.fill(cache, null);
    }
}