package models.characters;

/**
 * Enumeració d'estadístiques del personatge.
 * Algunes poden ser "focusables" (millorables amb focus).
 */
public enum Stat {

    STRENGTH("Força"),
    DEXTERITY("Destresa"),
    CONSTITUTION("Constitució"),
    INTELLIGENCE("Intel·ligència"),
    WISDOM("Saviesa", false),
    CHARISMA("Carisma", false),
    LUCK("Sort");

    private final String name;
    private final boolean canFocus;

    Stat(String name) {
        this.name = name;
        this.canFocus = true;
    }

    Stat(String name, boolean canFocus) {
        this.name = name;
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

    public String getName() {
        return name;
    }
}