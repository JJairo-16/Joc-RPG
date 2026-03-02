package models.characters;

import java.util.List;

public enum Breed {
    ORC(
            Stat.STRENGTH,
            "Orc",
            "Guerrers ferotges i resistents, amb una força descomunal que els converteix en combatents temibles cos a cos."),
    ELF(
            Stat.DEXTERITY,
            "Elf",
            "Àgils i precisos, experts en armes a distància i moviments ràpids. La seva destresa supera la de qualsevol altra raça."),
    DWARF(
            Stat.CONSTITUTION,
            "Nan",
            "Robustos i tenaços, amb una constitució excepcional que els permet resistir cops devastadors."),
    GNOME(
            Stat.INTELLIGENCE,
            "Gnom",
            "Ments brillants i curioses, especialistes en màgia i coneixement arcà."),
    HUMAN(
            Stat.WISDOM,
            "Humà",
            "Versàtils i adaptables, destaquen per la seva saviesa i capacitat d'aprendre de qualsevol situació."),
    TIEFLING(
            Stat.CHARISMA,
            "Tiflíng",
            "D'origen infernal, carismàtics i enigmàtics, amb una presència que intimida o sedueix amb facilitat."),
    HALFLING(
            Stat.LUCK,
            "Halfling",
            "Petits però sorprenentment afortunats, la sort sovint juga al seu favor en els moments més crítics.");

    private final Stat bonusStat;
    private final String name;
    private final String description;

    private static final double STAT_BONUS_BY_BREED = 1.15;

    Breed(Stat bonusStat, String name, String description) {
        this.bonusStat = bonusStat;
        this.name = name;
        this.description = description;
    }

    public Stat bonusStat() {
        return bonusStat;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static int effectiveStat(int base, Stat stat, Breed breed) {
        if (breed.bonusStat() != stat)
            return base;
        return (int) Math.round(base * STAT_BONUS_BY_BREED);
    }

    private static List<String> namesList = List.of(Breed.values()).stream()
            .map(b -> b.getName() + ": " + b.getDescription())
            .toList();

    public static List<String> getNamesList() {
        return namesList;
    }

    private static Breed[] breeds = Breed.values();

    public static Breed getByIdx(int idx) {
        if (idx < 0 || idx > breeds.length)
            throw new IllegalArgumentException("L'index està fora del rang.");

        return breeds[idx];
    }
}
