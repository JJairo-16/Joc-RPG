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
import models.weapons.passives.HitContext.Event;
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
 * especials.</li>
 * </ul>
 *
 * <p>
 * Nova arquitectura:
 * </p>
 * <ul>
 * <li>Pipeline per fases per a passius, incloent {@code ROLL_CRIT}.</li>
 * <li>Context mutable {@link HitContext} per donar flexibilitat a habilitats i
 * passives.</li>
 * <li>Registre d'esdeveniments de combat dins el context per desacoblar
 * reaccions
 * derivades del cop.</li>
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

    /**
     * Crea un combat 1vs1.
     *
     * @param p1 jugador 1
     * @param p2 jugador 2
     */
    public CombatSystem(Character p1, Character p2) {
        this(p1, p2, new DefaultTurnPriorityPolicy());
    }

    /**
     * Crea un combat 1vs1 amb política de prioritat personalitzada.
     *
     * @param p1     jugador 1
     * @param p2     jugador 2
     * @param policy política de prioritat
     */
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
     * <li>log d'accions</li>
     * <li>resum de dany rebut i barres d'estat</li>
     * <li>si segueixen vius, regeneració i nou estat</li>
     * </ul>
     *
     * @param a1 acció del jugador 1
     * @param a2 acció del jugador 2
     * @return guanyador si el combat acaba; {@code NONE} si continua
     */
    public Winner play(Action a1, Action a2) {
        Statistics p1Stats = player1.geStatistics();
        Statistics p2Stats = player2.geStatistics();

        EndRoundRegenBonus p1Bonus = new EndRoundRegenBonus();
        EndRoundRegenBonus p2Bonus = new EndRoundRegenBonus();

        double p1HealthBefore = p1Stats.getHealth();
        double p2HealthBefore = p2Stats.getHealth();

        boolean p1First = priorityPolicy.player1First(player1, a1, player2, a2, combatRng);

        printRoundHeader();

        if (p1First) {
            playPlayerTurn(player1, player2, a1, a2, p2Bonus);
            System.out.println();
            playPlayerTurn(player2, player1, a2, a1, p1Bonus);
        } else {
            playPlayerTurn(player2, player1, a2, a1, p1Bonus);
            System.out.println();
            playPlayerTurn(player1, player2, a1, a2, p2Bonus);
        }

        double p1HealthAfterAttacks = p1Stats.getHealth();
        double p2HealthAfterAttacks = p2Stats.getHealth();

        double p1DamageTaken = p1HealthBefore - p1HealthAfterAttacks;
        double p2DamageTaken = p2HealthBefore - p2HealthAfterAttacks;

        System.out.println();
        printRoundSummary(player1, p1DamageTaken);
        printRoundSummary(player2, p2DamageTaken);

        boolean player1Alive = player1.isAlive();
        boolean player2Alive = player2.isAlive();

        if (!player1Alive || !player2Alive) {
            if (player1Alive) {
                return Winner.PLAYER1;
            }
            if (player2Alive) {
                return Winner.PLAYER2;
            }
            return Winner.TIE;
        }

        double p1HealthPreRegen = p1Stats.getHealth();
        double p1ManaPreRegen = p1Stats.getMana();

        double p2HealthPreRegen = p2Stats.getHealth();
        double p2ManaPreRegen = p2Stats.getMana();

        player1.regen();
        player2.regen();

        applyEndRoundRegenBonus(player1, p1Bonus);
        applyEndRoundRegenBonus(player2, p2Bonus);

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

    /**
     * Afegeix a un {@link StringBuilder} les barres de vida i mana d'un personatge.
     *
     * @param sb        buffer de sortida
     * @param character personatge del qual es vol mostrar l'estat
     */
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

        sb.append("Mana: ").append(buildBar(currentMana, maxMana, BAR_SIZE, Ansi.BRIGHT_BLUE));
        sb.append(" ").append(round2(currentMana)).append(" / ").append(round2(maxMana));
        sb.append("\n");
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static String healthColor(double current, double max) {
        if (max <= 0) {
            return Ansi.BRIGHT_RED;
        }

        double r = Math.clamp(current / max, 0, 1);

        if (r > 0.60) {
            return Ansi.GREEN;
        }
        if (r > 0.30) {
            return Ansi.YELLOW;
        }
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
     * dany sigui 0, per mantenir els missatges especials.
     * </p>
     *
     * @param damage       dany a aplicar
     * @param target       objectiu
     * @param targetAction acció defensiva
     * @return resultat defensiu
     */
    private Result resolveAttack(double damage, Character target, Action targetAction) {
        return switch (targetAction) {
            case DODGE -> target.dodge(damage);
            case DEFEND -> target.defend(damage);
            default -> {
                if (damage <= 0) {
                    yield new Result(-1, "");
                }
                yield target.getDamage(damage);
            }
        };
    }

    /**
     * Executa el torn d'un atacant contra un defensor.
     *
     * @param attacker       atacant
     * @param defender       defensor
     * @param attackerAction acció de l'atacant
     * @param defenderAction acció del defensor
     */
    private void playPlayerTurn(Character attacker, Character defender, Action attackerAction, Action defenderAction,
            EndRoundRegenBonus defenderBonus) {
        if (attackerAction != ATTACK) {
            Result defenderResult = resolveAttack(0, defender, defenderAction);

            String msg = defenderResult.message();
            if (msg != null && !msg.isBlank()) {
                System.out.println(msg);
            }
            return;
        }

        Weapon preWeapon = attacker.getWeapon();
        if (isGrimori(preWeapon)) {
            System.out.println();
            System.out.println(Ansi.DARK_GRAY + "… el Grimori s'activa …" + Ansi.RESET);
            System.out.println();
        }

        AttackResult attackResult = attacker.attack();
        String attackerMsg = attackResult.message();

        Character realTarget = chooseTarget(attacker, defender, attackResult);

        Weapon w = attacker.getWeapon();
        boolean hasWeapon = (w != null);

        if (realTarget == attacker) {
            double dmg = attackResult.damage();
            if (dmg > 0) {
                attacker.getDamage(dmg);
            }

            System.out.printf("%s%s%s %s%n",
                    Ansi.BOLD, attacker.getName(), Ansi.RESET,
                    attackerMsg);
            return;
        }

        Random attackerRng = attacker.rng();
        Random defenderRng = defender.rng();

        HitContext ctx = new HitContext(attacker, defender, w, attackerRng, attackerAction, defenderAction);
        ctx.setAttackResult(attackResult);

        if (hasWeapon) {
            Statistics attackerStats = attacker.geStatistics();

            double rolledDamage = Math.max(0.0001, w.lastAttackDamage());
            double nonCritDamage = Math.max(0.0, w.lastNonCriticalDamage());

            double skillMultiplier = (rolledDamage > 0.0)
                    ? attackResult.damage() / rolledDamage
                    : 1.0;

            double rebuiltBaseDamage = round2(nonCritDamage * skillMultiplier);
            if (rebuiltBaseDamage <= 0 && attackResult.damage() > 0) {
                rebuiltBaseDamage = attackResult.damage();
            }

            ctx.setBaseDamage(rebuiltBaseDamage);
            ctx.setCriticalChance(w.resolveCriticalChance(attackerStats));
            ctx.setCriticalMultiplier(w.resolveCriticalMultiplier(attackerStats));

            ctx.putMeta("WEAPON_NAME", w.getName());
            ctx.putMeta("RAW_DAMAGE", rebuiltBaseDamage);
            ctx.putMeta("ORIGINAL_WEAPON_CRIT", w.lastWasCritic());
        } else {
            ctx.setBaseDamage(attackResult.damage());
            ctx.setCriticalChance(0.0);
            ctx.setCriticalMultiplier(1.0);
            ctx.putMeta("WEAPON_NAME", "Fists");
            ctx.putMeta("RAW_DAMAGE", attackResult.damage());
        }

        ctx.putMeta("CRIT", false);

        System.out.printf("%s%s%s %s%n",
                Ansi.BOLD, attacker.getName(), Ansi.RESET,
                attackerMsg);

        List<String> msgs = new ArrayList<>();

        attacker.triggerEffects(ctx, Phase.START_TURN, attackerRng, msgs);
        printMsgs(msgs);
        msgs.clear();

        attacker.triggerEffects(ctx, Phase.BEFORE_ATTACK, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.BEFORE_ATTACK, defenderRng, msgs);
        if (hasWeapon) {
            w.triggerPhase(ctx, attackerRng, Phase.BEFORE_ATTACK, msgs);
        }

        attacker.triggerEffects(ctx, Phase.ROLL_CRIT, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.ROLL_CRIT, defenderRng, msgs);
        if (hasWeapon) {
            w.triggerPhase(ctx, attackerRng, Phase.ROLL_CRIT, msgs);
        }

        boolean critical = ctx.resolveCritical();
        if (critical) {
            msgs.add("cop crític!");
        }

        attacker.triggerEffects(ctx, Phase.MODIFY_DAMAGE, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.MODIFY_DAMAGE, defenderRng, msgs);
        if (hasWeapon) {
            w.triggerPhase(ctx, attackerRng, Phase.MODIFY_DAMAGE, msgs);
        }

        attacker.triggerEffects(ctx, Phase.BEFORE_DEFENSE, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.BEFORE_DEFENSE, defenderRng, msgs);
        if (hasWeapon) {
            w.triggerPhase(ctx, attackerRng, Phase.BEFORE_DEFENSE, msgs);
        }

        printMsgs(msgs);
        msgs.clear();

        double damageToResolve = ctx.damageToResolve();

        Result defenderResult = resolveAttack(damageToResolve, defender, defenderAction);
        ctx.setDefenderResult(defenderResult);
        ctx.setDamageDealt(defenderResult.recivied());

        registerEndRoundDefenseRegenBonus(defenderAction, defenderResult, damageToResolve, defenderBonus);

        if (hasWeapon) {
            w.registerResolvedAttack(ctx.wasCritical(), damageToResolve);
        }

        if (defenderAction == DODGE) {
            ctx.registerEvent(Event.ON_DODGE);
        } else if (defenderAction == DEFEND) {
            ctx.registerEvent(Event.ON_DEFEND);
        }

        if (ctx.damageDealt() > 0) {
            ctx.registerEvent(Event.ON_HIT);
            ctx.registerEvent(Event.ON_DAMAGE_DEALT);
            ctx.registerEvent(Event.ON_DAMAGE_TAKEN);

            if (!defender.isAlive()) {
                ctx.registerEvent(Event.ON_KILL);
            }
        }

        if (defenderResult.recivied() != -1) {
            String msg = defenderResult.message();
            if (msg != null && !msg.isBlank()) {
                System.out.println(Ansi.DARK_GRAY + "  -> " + Ansi.RESET + msg);
            }
        }

        attacker.triggerEffects(ctx, Phase.AFTER_DEFENSE, attackerRng, msgs);
        defender.triggerEffects(ctx, Phase.AFTER_DEFENSE, defenderRng, msgs);
        if (hasWeapon) {
            w.triggerPhase(ctx, attackerRng, Phase.AFTER_DEFENSE, msgs);
        }

        if (ctx.damageDealt() > 0) {
            attacker.triggerEffects(ctx, Phase.AFTER_HIT, attackerRng, msgs);
            defender.triggerEffects(ctx, Phase.AFTER_HIT, defenderRng, msgs);
            if (hasWeapon) {
                w.triggerPhase(ctx, attackerRng, Phase.AFTER_HIT, msgs);
            }
        }

        printMsgs(msgs);
        msgs.clear();

        attacker.triggerEffects(ctx, Phase.END_TURN, attackerRng, msgs);
        printMsgs(msgs);
    }

    private void registerEndRoundDefenseRegenBonus(
            Action defenderAction,
            Result defenderResult,
            double incomingDamage,
            EndRoundRegenBonus bonus) {

        if (bonus == null || incomingDamage <= 0) {
            return;
        }

        double damageReceived = defenderResult.recivied();

        if (defenderAction == DODGE && wasSuccessfulDodge(damageReceived, incomingDamage)) {
            bonus.add(0.005, 0.01);
            return;
        }

        if (defenderAction == DEFEND && wasSuccessfulDefense(damageReceived, incomingDamage)) {
            bonus.add(0.004, 0.01);
        }
    }

    private boolean wasSuccessfulDodge(double damageReceived, double incomingDamage) {
        return (incomingDamage > 0 && damageReceived == 0);
    }

    private boolean wasSuccessfulDefense(double damageReceived, double incomingDamage) {
        return incomingDamage > 0 && damageReceived < incomingDamage;
    }

    private void applyEndRoundRegenBonus(Character character, EndRoundRegenBonus bonus) {
        if (character == null || bonus == null) {
            return;
        }

        Statistics stats = character.geStatistics();

        double hpAmount = stats.getMaxHealth() * bonus.bonusHealthPct();
        double manaAmount = stats.getMaxMana() * bonus.bonusManaPct();

        System.out.println(hpAmount);

        if (hpAmount > 0) {
            stats.heal(hpAmount);
        }

        if (manaAmount > 0) {
            stats.restoreMana(manaAmount);
        }
    }

    /**
     * Indica si l'arma és el Grimori.
     *
     * @param w arma
     * @return {@code true} si és el Grimori
     */
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
     * @param attacker     atacant
     * @param defender     defensor
     * @param attackResult resultat de l'atac
     * @return defensor si l'objectiu és enemic o nul; atacant si és SELF
     */
    private Character chooseTarget(Character attacker, Character defender, AttackResult attackResult) {
        Target t = attackResult.target();
        if (t == null || t == Target.ENEMY) {
            return defender;
        }
        return attacker;
    }

    /**
     * Imprimeix el resum del round d'un personatge.
     *
     * @param character   personatge
     * @param damageTaken dany rebut en el round
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
     * Imprimeix el resum de regeneració.
     *
     * @param character personatge
     * @param hpRegen   vida recuperada
     * @param manaRegen mana recuperat
     */
    private void printRegenSummary(Character character, double hpRegen, double manaRegen) {
        System.out.printf("%s%s%s recupera %s+%.2f%s vida i %s+%.2f%s mana.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.GREEN, Math.max(0, hpRegen), Ansi.RESET,
                Ansi.BRIGHT_BLUE, Math.max(0, manaRegen), Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

    private static final StringBuilder msgList = new StringBuilder();

    /**
     * Imprimeix missatges de passius en format consistent.
     *
     * @param msgs missatges a mostrar
     */
    private static void printMsgs(List<String> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return;
        }

        StringBuilder sb = msgList;
        sb.setLength(0);

        for (String msg : msgs) {
            if (msg == null || msg.isBlank()) {
                continue;
            }

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
     * @param current valor actual
     * @param max     valor màxim
     * @param size    longitud de la barra
     * @param color   color ANSI
     * @return barra renderitzada; o {@code [ERROR]} si les dades no són vàlides
     */
    private static String buildBar(double current, double max, int size, String color) {
        if (max <= 0 || max < current) {
            return "[ERROR]";
        }

        double ratio = Math.clamp(current / max, 0, 1);
        int filled = (int) Math.round(ratio * size);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i < filled) {
                bar.append(color).append("█").append(Ansi.RESET);
            } else {
                bar.append(Ansi.DARK_GRAY).append("░").append(Ansi.RESET);
            }
        }
        bar.append("]");

        return bar.toString();
    }
}