package utils.cache;

import java.util.Arrays;

import models.weapons.Arsenal;

public final class WeaponCardCache {

    // tamaño exacto necesario: armas * combinaciones de flags
    private static final int CACHE_SIZE = Arsenal.values().length * 4;
    private final String[] cache = new String[CACHE_SIZE];

    public int keyOf(Arsenal w, boolean showEquipTag, boolean equippable) {
        if (w == null)
            throw new IllegalArgumentException("weapon null");

        int key = w.ordinal();
        key = (key << 1) | (showEquipTag ? 1 : 0);
        key = (key << 1) | (equippable ? 1 : 0);

        return key;
    }

    public String cardOf(int key) {
        return (key >= 0 && key < cache.length) ? cache[key] : null;
    }

    public void save(int key, String card) {
        if (key < 0 || key >= cache.length) return;
        cache[key] = card;
    }

    public void clear() {
        Arrays.fill(cache, null);
    }
}