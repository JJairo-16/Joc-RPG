package models.characters;

public enum Stat {
    STRENGTH,
    DEXTERITY,
    CONSTITUTION,
    INTELLIGENCE,
    WISDOM(false),
    CHARISMA(false),
    LUCK;

    private final boolean canFocus;

    private Stat() {
        canFocus = true;
    }

    private Stat(boolean canFocus) {
        this.canFocus = canFocus;
    }

    private static int focusableCount = -1;

    public static int focusableCount() {
        if (focusableCount >= 0)
            return focusableCount;

        focusableCount = 0;
        for (Stat s : Stat.values())
            if (s.canFocus)
                focusableCount++;

        return focusableCount;
    }

    public boolean canFocus() {
        return canFocus;
    }
}
