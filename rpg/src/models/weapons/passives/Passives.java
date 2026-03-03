package models.weapons.passives;

/**
 * Fàbrica d'efectes passius d'arma.
 * Conté helpers per crear {@link WeaponPassive} reutilitzables.
 */
public class Passives {
    private Passives() {
        // Classe utilitària: no instanciable.
    }

    /**
     * Crea un passiu que cura l'atacant un percentatge del dany real infligit.
     *
     * @param pct percentatge de robatori de vida (p. ex. 0.15 = 15%)
     * @return passiu que s'aplica després d'encertar
     */
    public static WeaponPassive lifeSteal(double pct) {
        return (weapon, ctx, rng) -> {
            double healAmount = ctx.damageDealt() * pct;
            double realHealed = ctx.attacker().geStatistics().heal(healAmount);

            if (realHealed <= 0)
                return null;

            return String.format("%s roba %.1f HP",
                    ctx.attacker().getName(),
                    realHealed);

        };
    }

    public static WeaponPassive trueHarm(double pct) {
        return (weapon, ctx, rng) -> {
            double opponentHealth = ctx.defender().geStatistics().getMaxHealth();
            ctx.defender().geStatistics().damage(opponentHealth * pct);

            return String.format("%s connecta un dany verdader del %.2f%%", ctx.attacker().getName(), pct);
        };
    }
}