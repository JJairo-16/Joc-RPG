package models.weapons.passives;

import java.util.Random;

import models.characters.Character;
import models.characters.Statistics;
import models.weapons.Weapon;
import utils.ui.Ansi;

/**
 * Fàbrica d'efectes passius d'arma.
 * Conté helpers per crear {@link WeaponPassive} reutilitzables.
 */
public final class Passives {
    private Passives() {
        // Classe utilitària: no instanciable.
    }

    private static final String HP = Ansi.RED + "HP" + Ansi.RESET;

    /**
     * Crea un passiu que cura l'atacant un percentatge del dany real infligit.
     *
     * @param pct percentatge de robatori de vida (p. ex. 0.15 = 15%)
     * @return passiu que s'aplica després d'encertar
     */
    public static WeaponPassive lifeSteal(double pct) {
        return new WeaponPassive() {
            @Override
            public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
                double healAmount = ctx.damageDealt() * pct;
                double realHealed = ctx.attacker().geStatistics().heal(healAmount);

                if (realHealed <= 0)
                    return null;

                return String.format("%s roba %.1f %s",
                        ctx.attacker().getName(),
                        realHealed,
                        HP);
            }
        };
    }

    /**
     * Crea un passiu que aplica dany verdader (percentatge de la vida màxima del
     * rival)
     * després d'un impacte real.
     *
     * @param pct percentatge de vida màxima a convertir en dany (p.ex. 0.003 =
     *            0.3%)
     */
    public static WeaponPassive trueHarm(double pct) {
        return new WeaponPassive() {
            @Override
            public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
                double opponentMaxHealth = ctx.defender().geStatistics().getMaxHealth();
                double extra = opponentMaxHealth * pct;

                if (extra <= 0)
                    return null;

                ctx.defender().geStatistics().damage(extra);

                return String.format("%s connecta un dany verdader del %d%%",
                        ctx.attacker().getName(),
                        roundPercent(pct));
            }
        };
    }

    /**
     * Passiu d'execució: quan l'enemic està per sota d'un llindar de vida,
     * augmenta el dany abans de defensar.
     *
     * @param thresholdLife ratio de vida (0..1). Ex: 0.30 = 30%
     * @param damageBonus   bonus multiplicatiu extra (0..1). Ex: 0.25 = +25%
     */
    public static WeaponPassive executor(double thresholdLife, double damageBonus) {
        return new WeaponPassive() {
            @Override
            public String modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                Character defender = ctx.defender();
                Statistics defenderStats = defender.geStatistics();

                double ratio = defenderStats.getHealth() / defenderStats.getMaxHealth();
                if (ratio > thresholdLife)
                    return null;

                ctx.multiplyDamage(1.0 + damageBonus);

                return String.format("%s prepara una execució (+%d%% de dany)",
                        ctx.attacker().getName(),
                        roundPercent(damageBonus));
            }
        };
    }

    private static int roundPercent(double n) {
        return (int) Math.round(n * 100.0);
    }
}