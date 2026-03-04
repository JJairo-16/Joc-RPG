package combat;

import java.util.Random;
import java.util.List;

import static combat.Action.*;

import models.characters.Character;
import models.characters.Result;
import models.characters.Statistics;

import models.weapons.AttackResult;
import models.weapons.Target;
import models.weapons.Weapon;
import models.weapons.passives.HitContext;
import models.weapons.passives.HitContext.Phase;

import utils.ui.Ansi;

/**
 * Sistema de combat per torns amb accions simultànies.
 *
 * <p>
 * Resol dos torns per round (P1→P2 i P2→P1), mostra un resum de dany/estat i,
 * si ningú ha mort, aplica regeneració i en mostra el resultat.
 * </p>
 *
 * <p>
 * Notes:
 * </p>
 * <ul>
 * <li>El dany s'aplica dins {@code Character.dodge/defend/getDamage}.</li>
 * <li>Si {@code Target.SELF}, l'atacant es fa mal a si mateix però no es mostra
 * missatge del dany rebut.</li>
 * <li>Amb {@code DODGE}/{@code DEFEND} i dany 0, es preserven els missatges
 * “graciosos”.</li>
 * </ul>
 *
 * <p>
 * Nova arquitectura:
 * </p>
 * <ul>
 * <li>Pipeline per fases per a passius: BEFORE_ATTACK, MODIFY_DAMAGE, ...</li>
 * <li>Context mutable {@link HitContext} per donar flexibilitat a habilitats i
 * passives.</li>
 * </ul>
 */
public class CombatSystem {
    private final Character player1;
    private final Character player2;

    private final Random combatRng = new Random();
    private final TurnPriorityPolicy priorityPolicy;

    /** Crea un combat 1vs1. */
    public CombatSystem(Character p1, Character p2) {
        this(p1, p2, new DefaultTurnPriorityPolicy());
    }

    /** Crea un combat 1vs1. */
    public CombatSystem(Character p1, Character p2, TurnPriorityPolicy policy) {
        this.player1 = p1;
        this.player2 = p2;
        this.priorityPolicy = policy;
    }

    /**
     * Executa un round amb les accions dels dos jugadors.
     *
     * <p>
     * Mostra:
     * </p>
     * <ul>
     * <li>Log d'accions</li>
     * <li>Resum de dany rebut i barres d'estat</li>
     * <li>Si segueixen vius, regeneració i nou estat</li>
     * </ul>
     *
     * @param a1 acció del jugador 1
     * @param a2 acció del jugador 2
     * @return guanyador si el combat acaba; {@code NONE} si continua
     */
    public Winner play(Action a1, Action a2) {
        Statistics p1Stats = player1.geStatistics();
        Statistics p2Stats = player2.geStatistics();

        // Guardem vida inicial abans del round (per calcular dany rebut)
        double p1HealthBefore = p1Stats.getHealth();
        double p2HealthBefore = p2Stats.getHealth();

        // ── Executem torns segons política de prioritat ───────────────────────────
        // Nota: requireix aquests camps al CombatSystem:
        // private final TurnPriorityPolicy priorityPolicy;
        // private final java.util.Random combatRng = new java.util.Random();
        boolean p1First = priorityPolicy.player1First(player1, a1, player2, a2, combatRng);

        if (p1First) {
            playPlayerTurn(player1, player2, a1, a2);
            playPlayerTurn(player2, player1, a2, a1);
        } else {
            playPlayerTurn(player2, player1, a2, a1);
            playPlayerTurn(player1, player2, a1, a2);
        }

        // Calculem dany total rebut aquest round
        double p1HealthAfterAttacks = p1Stats.getHealth();
        double p2HealthAfterAttacks = p2Stats.getHealth();

        double p1DamageTaken = p1HealthBefore - p1HealthAfterAttacks;
        double p2DamageTaken = p2HealthBefore - p2HealthAfterAttacks;

        // Separació visual + resum del round (SENSE regeneració encara)
        System.out.println();
        printRoundSummary(player1, p1DamageTaken);
        printRoundSummary(player2, p2DamageTaken);

        boolean player1Alive = player1.isAlive();
        boolean player2Alive = player2.isAlive();

        // Si s'ha acabat el combat: NO regenerem, només retornem el guanyador
        if (!player1Alive || !player2Alive) {
            if (player1Alive)
                return Winner.PLAYER1;
            if (player2Alive)
                return Winner.PLAYER2;
            return Winner.TIE;
        }

        // Guardem estat abans de regenerar per calcular quant s'ha curat / recuperat
        // mana
        double p1HealthPreRegen = p1Stats.getHealth();
        double p1ManaPreRegen = p1Stats.getMana();

        double p2HealthPreRegen = p2Stats.getHealth();
        double p2ManaPreRegen = p2Stats.getMana();

        // Regenerem (això hauria d'afectar vida i mana)
        player1.regen();
        player2.regen();

        // Calculem increments reals (clamp inclòs)
        double p1HealthRegen = p1Stats.getHealth() - p1HealthPreRegen;
        double p1ManaRegen = p1Stats.getMana() - p1ManaPreRegen;

        double p2HealthRegen = p2Stats.getHealth() - p2HealthPreRegen;
        double p2ManaRegen = p2Stats.getMana() - p2ManaPreRegen;

        System.out.println();
        System.out.println("=== Regeneració ===");
        printRegenSummary(player1, p1HealthRegen, p1ManaRegen);
        printRegenSummary(player2, p2HealthRegen, p2ManaRegen);

        return Winner.NONE;
    }

    /**
     * Mètode públic per imprimir les barres de vida i mana d'un personatge.
     * Pensat per reutilitzar-lo com a dependència interna o API pública.
     *
     * @param character personatge del qual es vol mostrar l'estat
     */
    public static void printStatusBars(Character character) {
        Statistics stats = character.geStatistics();

        double currentHealth = stats.getHealth();
        double maxHealth = stats.getMaxHealth();

        double currentMana = stats.getMana();
        double maxMana = stats.getMaxMana();

        int barSize = 20;

        System.out.println("   ─────────────");

        System.out.printf("Vida: %s %.2f / %.2f%n",
                buildBar(currentHealth, maxHealth, barSize, Ansi.BRIGHT_RED),
                currentHealth,
                maxHealth);

        System.out.printf("Mana: %s %.2f / %.2f%n",
                buildBar(currentMana, maxMana, barSize, Ansi.BRIGHT_BLUE),
                currentMana,
                maxMana);
    }

    /**
     * Resolveix l'efecte d'un atac sobre un objectiu segons la seva acció
     * defensiva.
     *
     * <p>
     * Amb {@code DODGE}/{@code DEFEND} s'invoca sempre el mètode, encara que el
     * dany sigui 0,
     * per mantenir els missatges especials.
     * </p>
     *
     * @param damage       dany entrant
     * @param target       objectiu que rep (o intenta evitar) el dany
     * @param targetAction acció del target davant l'atac
     * @return resultat (inclou missatge i dany rebut)
     */
    private Result resolveAttack(double damage, Character target, Action targetAction) {
        return switch (targetAction) {
            case DODGE -> target.dodge(damage);
            case DEFEND -> target.defend(damage);
            default -> {
                if (damage <= 0)
                    yield new Result(-1, "");
                yield target.getDamage(damage);
            }
        };
    }

    /**
     * Executa el torn d'un atacant contra un defensor.
     *
     * <p>
     * Casos especials:
     * </p>
     * <ul>
     * <li>Si l'atacant no ataca, el defensor pot igualment "executar" la seva
     * defensa amb dany 0.</li>
     * <li>Si l'atac apunta a {@code SELF}, s'aplica dany a l'atacant sense mostrar
     * el dany rebut.</li>
     * </ul>
     */
    private void playPlayerTurn(Character attacker, Character defender, Action attackerAction, Action defenderAction) {

        // Si l'atacant NO ataca, el defensor igualment pot "executar" la seva acció
        // defensiva.
        if (attackerAction != ATTACK) {
            Result defenderResult = resolveAttack(0, defender, defenderAction);

            if (defenderResult.message() != null && !defenderResult.message().isBlank()) {
                System.out.println(defenderResult.message());
            }
            return;
        }

        // Atac normal
        AttackResult attackResult = attacker.attack();
        String attackerMsg = attackResult.message();

        // Determinar objectiu real segons AttackResult.target()
        Character realTarget = chooseTarget(attacker, defender, attackResult);

        Weapon w = attacker.getWeapon();

        // SELF: aplicar dany però NO mostrar missatge del dany rebut.
        if (realTarget == attacker) {
            double dmg = attackResult.damage();
            if (dmg > 0) {
                attacker.getDamage(dmg);
            }
            System.out.printf("%s %s%n", attacker.getName(), attackerMsg);
            return;
        }

        // ── Pipeline amb context mutable ───────────────────────────
        HitContext ctx = new HitContext(attacker, defender, w, attacker.rng(), attackerAction, defenderAction);
        ctx.setAttackResult(attackResult);

        // Metadades útils (context extra per a passives)
        if (w != null) {
            ctx.putMeta("CRIT", w.lastWasCritic());
            ctx.putMeta("WEAPON_NAME", w.getName());
        } else {
            ctx.putMeta("CRIT", false);
            ctx.putMeta("WEAPON_NAME", "Fists");
        }
        ctx.putMeta("RAW_DAMAGE", ctx.baseDamage());

        // BEFORE_ATTACK: ideal per marcar tags, checks, etc.
        if (w != null)
            printMsgs(w.triggerPhase(ctx, attacker.rng(), Phase.BEFORE_ATTACK));

        // MODIFY_DAMAGE: aquí es modifica el dany que entrarà a la defensa
        if (w != null)
            printMsgs(w.triggerPhase(ctx, attacker.rng(), Phase.MODIFY_DAMAGE));

        // BEFORE_DEFENSE: informació sobre la defensa imminent
        if (w != null)
            printMsgs(w.triggerPhase(ctx, attacker.rng(), Phase.BEFORE_DEFENSE));

        double damageToResolve = ctx.damageToResolve();

        // Aplicació segons l'acció del defensor
        Result defenderResult = resolveAttack(damageToResolve, defender, defenderAction);
        ctx.setDefenderResult(defenderResult);
        ctx.setDamageDealt(defenderResult.recivied());

        // AFTER_DEFENSE: ja tenim el resultat de defensa (missatge + dany rebut)
        if (w != null)
            printMsgs(w.triggerPhase(ctx, attacker.rng(), Phase.AFTER_DEFENSE));

        // AFTER_HIT: només si hi ha dany real
        if (w != null && ctx.damageDealt() > 0) {
            printMsgs(w.triggerPhase(ctx, attacker.rng(), Phase.AFTER_HIT));
        }

        // Si no hi ha dany i tampoc hi ha missatge útil (cas damage<=0 i acció
        // "default"),
        // només mostrem l'atacant.
        if (defenderResult.recivied() == -1) {
            System.out.printf("%s %s%n", attacker.getName(), attackerMsg);
            return;
        }

        // Hi ha resultat (amb missatge). El missatge del defensor ja inclou el seu nom.
        System.out.printf("%s %s -> %s%n", attacker.getName(), attackerMsg, defenderResult.message());
    }

    /**
     * Determina l'objectiu real d'un atac.
     *
     * <p>
     * Per defecte (target nul o {@code ENEMY}) l'objectiu és el defensor.
     * </p>
     *
     * @return {@code defender} si ENEMY (o null), {@code attacker} si SELF
     */
    private Character chooseTarget(Character attacker, Character defender, AttackResult attackResult) {
        Target t = attackResult.target();
        if (t == null || t == Target.ENEMY)
            return defender;
        return attacker; // Target.SELF
    }

    /**
     * Imprimeix el resum del round d'un personatge:
     * dany rebut i barres de vida/mana amb els valors actuals.
     */
    private void printRoundSummary(Character character, double damageTaken) {
        System.out.printf("%s ha rebut %.2f de dany.%n", character.getName(), damageTaken);
        printStatusBars(character);
        System.out.println();
    }

    /**
     * Imprimeix el resum de regeneració:
     * quant s'ha recuperat i l'estat resultant (barres + valors).
     */
    private void printRegenSummary(Character character, double hpRegen, double manaRegen) {
        System.out.printf("%s regenera: +%.2f vida, +%.2f mana.%n",
                character.getName(),
                Math.max(0, hpRegen),
                Math.max(0, manaRegen));

        printStatusBars(character);
        System.out.println();
    }

    /**
     * Imprimeix missatges de passius en format consistent.
     */
    private static void printMsgs(List<String> msgs) {
        if (msgs == null || msgs.isEmpty())
            return;
        for (String msg : msgs) {
            if (msg != null && !msg.isBlank()) {
                System.out.println("  + " + msg);
            }
        }
    }

    /**
     * Construeix una barra proporcional (█/░) per un valor actual i un màxim.
     *
     * @param current valor actual
     * @param max     valor màxim
     * @param size    longitud de la barra
     * @param color   codi ANSI per pintar la barra
     * @return barra amb color + reset; o "[ERROR]" si els valors no són vàlids
     */
    private static String buildBar(double current, double max, int size, String color) {
        if (max <= 0 || max < current)
            return "[ERROR]";

        double ratio = Math.clamp(current / max, 0, 1);
        int filled = (int) Math.round(ratio * size);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");

        return color + bar.toString() + Ansi.RESET;
    }
}