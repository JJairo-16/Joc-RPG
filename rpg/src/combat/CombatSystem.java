package combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static combat.Action.*;
import static utils.ui.Ansi.RESET;

import models.characters.Character;
import models.characters.Result;
import models.characters.Statistics;
import models.weapons.Arsenal;
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

    // ── UI constants ───────────────────────────────────────────────────────────
    private static final int BAR_SIZE = 20;
    private static final int DIV_WIDTH = 44;

    private static final String DIV = Ansi.DARK_GRAY + "─".repeat(DIV_WIDTH) + Ansi.RESET;
    private static final String BIG_DIV = Ansi.DARK_GRAY + "═".repeat(DIV_WIDTH) + Ansi.RESET;

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

    boolean x = true;

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
        boolean p1First = priorityPolicy.player1First(player1, a1, player2, a2, combatRng);

        // Header suau del round (no interfereix amb el grimori perquè encara no hi ha
        // prompt)
        printRoundHeader();

        if (p1First) {
            playPlayerTurn(player1, player2, a1, a2);
            System.out.println();
            playPlayerTurn(player2, player1, a2, a1);
        } else {
            playPlayerTurn(player2, player1, a2, a1);
            System.out.println();
            playPlayerTurn(player1, player2, a1, a2);
        }

        // Calculem dany total rebut aquest round
        double p1HealthAfterAttacks = p1Stats.getHealth();
        double p2HealthAfterAttacks = p2Stats.getHealth();

        double p1DamageTaken = p1HealthBefore - p1HealthAfterAttacks;
        double p2DamageTaken = p2HealthBefore - p2HealthAfterAttacks;

        // Resum del round (SENSE regeneració encara)
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

        // Regenerem
        player1.regen();
        player2.regen();

        // Calculem increments reals
        double p1HealthRegen = p1Stats.getHealth() - p1HealthPreRegen;
        double p1ManaRegen = p1Stats.getMana() - p1ManaPreRegen;

        double p2HealthRegen = p2Stats.getHealth() - p2HealthPreRegen;
        double p2ManaRegen = p2Stats.getMana() - p2ManaPreRegen;

        System.out.println();
        printRegenHeader();
        printRegenSummary(player1, p1HealthRegen, p1ManaRegen);
        printRegenSummary(player2, p2HealthRegen, p2ManaRegen);

        return Winner.NONE;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ───────────────────────────────────────────────────────────────────────────

    private static void printRoundHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.BOLD + "          COMBAT ROUND" + Ansi.RESET);
        System.out.println(BIG_DIV);
        System.out.println();
    }

    private static void printRegenHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.CYAN + Ansi.BOLD + "           REGENERACIÓ" + Ansi.RESET);
        System.out.println(BIG_DIV);
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

        System.out.println("   " + Ansi.DARK_GRAY + "─────────────" + Ansi.RESET);

        String hpColor = healthColor(currentHealth, maxHealth);

        System.out.printf("Vida: %s %.2f / %.2f%n",
                buildBar(currentHealth, maxHealth, BAR_SIZE, hpColor),
                currentHealth,
                maxHealth);

        System.out.printf("Mana: %s %.2f / %.2f%n",
                buildBar(currentMana, maxMana, BAR_SIZE, Ansi.BRIGHT_BLUE),
                currentMana,
                maxMana);
    }

    public static void appendStatusBars(StringBuilder sb, Character character) {
        Statistics stats = character.geStatistics();

        double currentHealth = stats.getHealth();
        double maxHealth = stats.getMaxHealth();

        double currentMana = stats.getMana();
        double maxMana = stats.getMaxMana();

        sb.append("  ").append(Ansi.DARK_GRAY).append("─────────────").append(RESET).append("\n");
     
        String hpColor = healthColor(currentHealth, maxHealth);

        sb.append("Vida: ").append(buildBar(currentHealth, maxHealth, BAR_SIZE, hpColor));
        sb.append(" ").append(round2(currentHealth)).append(" / ").append(round2(maxHealth));
        sb.append("\n");

        sb.append("Vida: ").append(buildBar(currentMana, maxMana, BAR_SIZE, Ansi.BRIGHT_BLUE));
        sb.append(" ").append(round2(currentMana)).append(" / ").append(round2(maxMana));
        sb.append("\n");
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static String healthColor(double current, double max) {
        if (max <= 0)
            return Ansi.BRIGHT_RED;

        double r = Math.clamp(current / max, 0, 1);

        if (r > 0.60)
            return Ansi.GREEN;
        if (r > 0.30)
            return Ansi.YELLOW;
        return Ansi.BRIGHT_RED;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Combat logic
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Resolveix l'efecte d'un atac sobre un objectiu segons la seva acció
     * defensiva.
     *
     * <p>
     * Amb {@code DODGE}/{@code DEFEND} s'invoca sempre el mètode, encara que el
     * dany sigui 0,
     * per mantenir els missatges especials.
     * </p>
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

            String msg = defenderResult.message();
            if (msg != null && !msg.isBlank()) {
                System.out.println(msg);
            }
            return;
        }

        // Si és Grimori: donem espai abans del mini-joc perquè no es barregi amb logs.
        Weapon preWeapon = attacker.getWeapon();
        if (isGrimori(preWeapon)) {
            System.out.println();
            System.out.println(Ansi.DARK_GRAY + "… el Grimori s'activa …" + Ansi.RESET);
            System.out.println();
        }

        // Atac normal (pot disparar el mini-joc del grimori a dins)
        AttackResult attackResult = attacker.attack();
        String attackerMsg = attackResult.message();

        // Determinar objectiu real segons AttackResult.target()
        Character realTarget = chooseTarget(attacker, defender, attackResult);

        Weapon w = attacker.getWeapon();
        boolean hasWeapon = (w != null);

        // SELF: aplicar dany però NO mostrar missatge del dany rebut.
        if (realTarget == attacker) {
            double dmg = attackResult.damage();
            if (dmg > 0)
                attacker.getDamage(dmg);

            System.out.printf("%s%s%s %s%n",
                    Ansi.BOLD, attacker.getName(), Ansi.RESET,
                    attackerMsg);
            return;
        }

        // ── Context ───────────────────────────────────────────────────────────────
        Random attackerRng = attacker.rng();
        Random defenderRng = defender.rng();

        HitContext ctx = new HitContext(attacker, defender, w, attackerRng, attackerAction, defenderAction);
        ctx.setAttackResult(attackResult);

        if (hasWeapon) {
            ctx.putMeta("CRIT", w.lastWasCritic());
            ctx.putMeta("WEAPON_NAME", w.getName());
        } else {
            ctx.putMeta("CRIT", false);
            ctx.putMeta("WEAPON_NAME", "Fists");
        }
        ctx.putMeta("RAW_DAMAGE", ctx.baseDamage());

        // 1) Log principal de l'atacant
        System.out.printf("%s%s%s %s%n",
                Ansi.BOLD, attacker.getName(), Ansi.RESET,
                attackerMsg);

        // Missatges (reutilitzem la mateixa llista per mantenir ordre i evitar
        // al·locacions)
        List<String> msgs = new ArrayList<>();

        // ── START_TURN: NOMÉS el jugador que té el torn (evita consumir 2 cops per
        // ronda) ──
        attacker.triggerEffects(ctx, Phase.START_TURN, attackerRng, msgs);
        printMsgs(msgs);
        msgs.clear();

        // ── BEFORE_ATTACK / MODIFY_DAMAGE / BEFORE_DEFENSE: efectes d'ambdós +
        // passives arma ──
        attacker.triggerEffects(ctx, Phase.BEFORE_ATTACK, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.BEFORE_ATTACK, defenderRng, msgs);
        if (hasWeapon)
            w.triggerPhase(ctx, attackerRng, Phase.BEFORE_ATTACK, msgs);

        attacker.triggerEffects(ctx, Phase.MODIFY_DAMAGE, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.MODIFY_DAMAGE, defenderRng, msgs);
        if (hasWeapon)
            w.triggerPhase(ctx, attackerRng, Phase.MODIFY_DAMAGE, msgs);

        attacker.triggerEffects(ctx, Phase.BEFORE_DEFENSE, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.BEFORE_DEFENSE, defenderRng, msgs);
        if (hasWeapon)
            w.triggerPhase(ctx, attackerRng, Phase.BEFORE_DEFENSE, msgs);

        printMsgs(msgs);
        msgs.clear();

        // ── Resoldre defensa ──────────────────────────────────────────────────────
        double damageToResolve = ctx.damageToResolve();

        Result defenderResult = resolveAttack(damageToResolve, defender, defenderAction);
        ctx.setDefenderResult(defenderResult);
        ctx.setDamageDealt(defenderResult.recivied());

        // Mostrar resultat de defensa si toca
        if (defenderResult.recivied() != -1) {
            String msg = defenderResult.message();
            if (msg != null && !msg.isBlank()) {
                System.out.println(Ansi.DARK_GRAY + "  -> " + Ansi.RESET + msg);
            }
        }

        // ── AFTER_DEFENSE / AFTER_HIT: efectes d'ambdós + passives arma ───────────
        attacker.triggerEffects(ctx, Phase.AFTER_DEFENSE, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.AFTER_DEFENSE, defenderRng, msgs);
        if (hasWeapon)
            w.triggerPhase(ctx, attackerRng, Phase.AFTER_DEFENSE, msgs);

        if (ctx.damageDealt() > 0) {
            attacker.triggerEffects(ctx, Phase.AFTER_HIT, attackerRng, msgs);
            defender.triggerEffects(ctx, Phase.AFTER_HIT, defenderRng, msgs);
            if (hasWeapon)
                w.triggerPhase(ctx, attackerRng, Phase.AFTER_HIT, msgs);
        }

        printMsgs(msgs);
        msgs.clear();

        // ── END_TURN: NOMÉS el jugador que té el torn (tick duració/cooldown 1 cop per
        // torn) ──
        attacker.triggerEffects(ctx, Phase.END_TURN, attackerRng, msgs);
        printMsgs(msgs);
    }

    private static boolean isGrimori(Weapon w) {
        try {
            return w != null && w.getId() == Arsenal.GRIMORIE;
        } catch (Exception e) {
            return false;
        }
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
        System.out.println(DIV);

        System.out.printf("%s%s%s ha rebut %s%.2f%s de dany.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.BRIGHT_RED, damageTaken, Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

    /**
     * Imprimeix el resum de regeneració:
     * quant s'ha recuperat i l'estat resultant (barres + valors).
     */
    private void printRegenSummary(Character character, double hpRegen, double manaRegen) {
        System.out.printf("%s%s%s recupera %s+%.2f%s vida i %s+%.2f%s mana.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.GREEN, Math.max(0, hpRegen), Ansi.RESET,
                Ansi.BRIGHT_BLUE, Math.max(0, manaRegen), Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

    private static StringBuilder msgList = new StringBuilder();

    /**
     * Imprimeix missatges de passius en format consistent.
     */
    private static void printMsgs(List<String> msgs) {
        if (msgs == null || msgs.isEmpty())
            return;

        StringBuilder sb = msgList;
        sb.setLength(0);

        for (String msg : msgs) {
            if (msg == null || msg.isBlank())
                continue;

            if (msg.charAt(0) == '-') {
                sb.append("  ").append(Ansi.RED).append("-").append(Ansi.RESET);
                sb.append(" ").append(msg.substring(1).trim()).append("\n");
            } else {
                sb.append("  ").append(Ansi.GREEN).append("+").append(Ansi.RESET);
                sb.append(" ").append(msg).append("\n");
            }
        }

        System.out.println(sb.toString());
    }

    /**
     * Construeix una barra proporcional (█/░) per un valor actual i un màxim.
     *
     * @return barra amb color + reset; o "[ERROR]" si els valors no són vàlids
     */
    private static String buildBar(double current, double max, int size, String color) {
        if (max <= 0 || max < current)
            return "[ERROR]";

        double ratio = Math.clamp(current / max, 0, 1);
        int filled = (int) Math.round(ratio * size);

        StringBuilder bar = new StringBuilder("[");
        bar.ensureCapacity(size + 1);

        for (int i = 0; i < size; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");

        return color + bar.toString() + Ansi.RESET;
    }
}