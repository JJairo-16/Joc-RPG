package combat;

import java.util.Random;

import models.characters.Character;
import models.characters.Statistics;

/**
 * Política per defecte de prioritat.
 *
 * <p>
 * Regla principal ("una altra lliga"):
 * </p>
 * <ul>
 *   <li>DEFEND/DODGE sempre tenen prioritat sobre ATTACK.</li>
 *   <li>Per la resta de casos (mateixa lliga), es calcula iniciativa.</li>
 * </ul>
 *
 * <p>
 * Iniciativa (mateixa lliga):
 * </p>
 * <ul>
 *   <li>DEX com a base</li>
 *   <li>LUCK com a bonus petit</li>
 *   <li>un roll petit per evitar empats constants</li>
 * </ul>
 */
public class DefaultTurnPriorityPolicy implements TurnPriorityPolicy {
    private static final double DEX_VALUE = 1.0;
    private static final double LUCK_VALUE = 0.25;

    @Override
    public boolean player1First(Character p1, Action a1, Character p2, Action a2, Random rng) {

        // ── Lliga 1: DEFEND/DODGE (sempre per davant d'ATTACK) ─────
        boolean p1Fast = isFastAction(a1);
        boolean p2Fast = isFastAction(a2);

        // Si un està a la lliga ràpida i l'altre ataca, guanya el ràpid.
        if (p1Fast && !p2Fast) return true;
        if (p2Fast && !p1Fast) return false;

        // ── Mateixa lliga: decidim per iniciativa ──────────────────
        double i1 = initiativeScore(p1, rng);
        double i2 = initiativeScore(p2, rng);

        if (i1 > i2) return true;
        if (i2 > i1) return false;

        // Empat: moneda a l'aire
        return rng.nextBoolean();
    }

    /**
     * Accions que considerem "ràpides" (lliga superior).
     */
    private boolean isFastAction(Action a) {
        return a == Action.DEFEND || a == Action.DODGE;
    }

    /**
     * Calcula un score d'iniciativa per desempatar dins la mateixa lliga.
     */
    private double initiativeScore(Character c, Random rng) {
        Statistics s = c.geStatistics();

        double dex = s.getDexterity();
        double luck = s.getLuck();

        double roll = rng.nextDouble() * 10.0; // 0..10
        return dex * DEX_VALUE + luck * LUCK_VALUE + roll;
    }
}