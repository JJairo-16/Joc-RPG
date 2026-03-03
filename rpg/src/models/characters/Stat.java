package models.characters;

/**
 * Enumeració d'estadístiques del personatge.
 * Algunes poden ser "focusables" (millorables amb focus).
 */
public enum Stat {

    STRENGTH,
    DEXTERITY,
    CONSTITUTION,
    INTELLIGENCE,
    WISDOM(false),
    CHARISMA(false),
    LUCK;

    private final boolean canFocus;

    Stat() {
        this.canFocus = true;
    }

    Stat(boolean canFocus) {
        this.canFocus = canFocus;
    }

    // Cache del recompte d'estadístiques focusables
    private static int focusableCount = -1;

    /**
     * Retorna el nombre d'estadístiques que permeten focus.
     * Es calcula una sola vegada i es guarda en memòria.
     */
    public static int focusableCount() {
        if (focusableCount >= 0) return focusableCount;

        focusableCount = 0;
        for (Stat s : Stat.values()) {
            if (s.canFocus) focusableCount++;
        }

        return focusableCount;
    }

    /** Indica si aquesta estadística pot rebre focus. */
    public boolean canFocus() {
        return canFocus;
    }
}