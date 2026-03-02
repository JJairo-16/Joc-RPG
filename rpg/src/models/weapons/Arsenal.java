package models.weapons;

import static models.weapons.WeaponType.*;

import java.util.List;

public enum Arsenal {
    EXPLOSIVE_CROSSBOW(
            "Ballesta explosiva",
            "Una ballesta inestable que converteix cada tret en una aposta: pot esclatar amb força devastadora contra l’enemic… o girar-se contra tu mateix.",
            120, 0.15, 1.6,
            RANGE,
            (weapon, stats, rng) -> {
                double base = weapon.basicAttack(stats, rng);

                // Multiplicadors de la ruleta
                double selfMultiplier = 0.50; // reducció si et dispares a tu mateix
                double enemyMultiplier = 1.10; // bonus si encertes l'enemic

                int luck = stats.getLuck();

                // Probabilitat d'autodispar influenciada per la sort
                double selfShotProb = 0.22 - 0.0047 * luck; // càlcul lineal
                selfShotProb = Math.clamp(selfMultiplier, 0.08, 0.22); // clamp [8%, 22%]

                boolean selfShot = rng.nextDouble() < selfShotProb;

                double finalDamage = base * (selfShot ? selfMultiplier : enemyMultiplier);

                boolean crit = weapon.lastWasCritic();
                if (selfShot) {
                    stats.damage(finalDamage);

                    if (crit) {
                        return new AttackResult(finalDamage,
                                "S'ha pegat un tir crític a sí mateix. (crec que alla no era)");
                    } else {
                        return new AttackResult(finalDamage, "S'ha pegat un tir a sí mateix. (crec que alla no era)");
                    }
                }

                if (crit) {
                    return new AttackResult(finalDamage, "Ha pegat un tir crític.");
                }

                return new AttackResult(finalDamage, "Ha pegat un tir.");
            });

    private final String name;
    private final String description;

    private final int baseDamage;
    private final double criticalProb;
    private final double criticalDamage;

    private final WeaponType type;
    private final Attack attack;

    private Arsenal(String name, String description, int baseDamage, double criticalProb, double criticalDamage,
            WeaponType type, Attack attack) {
        this.name = name;
        this.description = description;
        this.baseDamage = baseDamage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attack = attack;
    }

    public Weapon create() {
        return new Weapon(name, baseDamage, criticalProb, criticalDamage, type, attack);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private static List<String> namesList = List.of(Arsenal.values()).stream()
            .map(w -> w.getName() + ": " + w.getDescription())
            .toList();

    public static List<String> getNamesList() {
        return namesList;
    }

    private static Arsenal[] weapons = Arsenal.values();

    public static Weapon getWeaponByIdx(int idx) {
        if (idx < 0 || idx > weapons.length)
            throw new IllegalArgumentException("L'index està fora del rang.");

        return weapons[idx].create();
    }
}
