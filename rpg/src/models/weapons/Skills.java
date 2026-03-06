package models.weapons;

import static models.weapons.Target.SELF;
import static utils.ui.Ansi.*;

import java.util.Random;
import java.util.Scanner;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import models.characters.Statistics;
import utils.rng.GrimoriCodeGenerator;

/**
 * Habilitats especials relacionades amb armes.
 */
public final class Skills {

    private static final Scanner IN = new Scanner(System.in);
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    private static final GrimoriCodeGenerator grimoriCodeGenerator = new GrimoriCodeGenerator(5, 1, 4, 2);

    private static final int GRIMORI_WIDTH = 50;
    private static final String GRIMORI_LINE = "+" + "-".repeat(GRIMORI_WIDTH) + "+";
    private static final String GRIMORI_SEP = "|" + "-".repeat(GRIMORI_WIDTH) + "|";

    private Skills() {
    }

    /**
     * No aplica cap habilitat especial.
     * Executa simplement l'atac bàsic de l'arma amb missatge inclòs.
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques del personatge
     * @param rng    generador aleatori
     * @return resultat de l'atac bàsic
     */
    public static AttackResult nothing(Weapon weapon, Statistics stats, Random rng) {
        return weapon.basicAttackWithMessage(stats, rng);
    }

    /**
     * Dispar explosiu que pot impactar a l'enemic o, amb una certa probabilitat
     * influenciada per la sort, a un mateix.
     *
     * <p>
     * Si impacta a l'enemic, aplica un petit bonus de dany.
     * Si impacta a un mateix, aplica una reducció de dany i canvia l'objectiu a
     * {@link Target#SELF}.
     * </p>
     *
     * @param weapon arma usada per calcular el dany base i crítics
     * @param stats  estadístiques (la sort afecta la probabilitat d'autodispar)
     * @param rng    generador aleatori
     * @return resultat de l'atac amb dany, missatge i objectiu si escau
     */
    public static AttackResult explosiveShot(Weapon weapon, Statistics stats, Random rng) {
        double base = weapon.basicAttack(stats, rng);

        double selfMultiplier = 0.50;
        double enemyMultiplier = 1.10;

        int luck = stats.getLuck();

        double selfShotProb = 0.22 - 0.0047 * luck;
        selfShotProb = Math.clamp(selfShotProb, 0.08, 0.22);

        boolean selfShot = rng.nextDouble() < selfShotProb;
        double finalDamage = base * (selfShot ? selfMultiplier : enemyMultiplier);

        boolean crit = weapon.lastWasCritic();
        if (selfShot) {
            if (crit) {
                return new AttackResult(
                        finalDamage,
                        "s'ha pegat un tir crític a sí mateix. La mala puntería es perdona, pero la explosió no.",
                        SELF);
            }
            return new AttackResult(finalDamage, "s'ha pegat un tir a sí mateix. (crec que alla no era)", SELF);
        }

        if (crit) {
            return new AttackResult(finalDamage, "ha pegat un tir crític.");
        }

        return new AttackResult(finalDamage, "ha pegat un tir.");
    }

    /**
     * Realitza dos atacs consecutius amb l'arma i acumula el dany total.
     * Indica el percentatge d'atacs que han estat crítics.
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques del personatge
     * @param rng    generador aleatori
     * @return resultat amb el dany total i informació de crítics
     */
    public static AttackResult crossCut(Weapon weapon, Statistics stats, Random rng) {
        final int attacks = 2;

        double totalDamage = 0.0;
        int crit = 0;

        for (int i = 0; i < attacks; i++) {
            totalDamage += weapon.basicAttack(stats, rng);
            if (weapon.lastWasCritic()) {
                crit++;
            }
        }

        double criticalPercent = crit * 100.0 / attacks;

        return new AttackResult(
                totalDamage,
                "ha atacat amb una talla transversal [" + criticalPercent + "% crític].");
    }

    /**
     * Llença una disrupció arcana que consumeix mana.
     *
     * <p>
     * Té una probabilitat de fallar influenciada per la sort.
     * Si té èxit, aplica l'atac bàsic.
     * En cas de crític, consumeix un 50% addicional del cost de mana.
     * </p>
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques (inclou consum de mana i sort)
     * @param rng    generador aleatori
     * @return resultat de l'habilitat amb dany o fallida
     */
    public static AttackResult arcaneDisruption(Weapon weapon, Statistics stats, Random rng) {
        double baseManaCost = weapon.getManaPrice();

        if (baseManaCost > 0 && !stats.consumeMana(baseManaCost)) {
            return new AttackResult(0, "no té prou mana per llençar la disrupció arcana.");
        }

        stats.consumeMana(baseManaCost);

        double luck = stats.getLuck();
        double failChance = 0.30 - (luck * (0.08 / 30.0));
        failChance = Math.clamp(failChance, 0.22, 0.30);

        if (rng.nextDouble() < failChance) {
            return new AttackResult(
                    0,
                    "llença la disrupció arcana falla i la màgia es dissipa en el no-res. (molt útil ._.)");
        }

        double damage = weapon.basicAttack(stats, rng);

        if (weapon.lastWasCritic()) {
            double extraMana = baseManaCost * 0.50;
            stats.consumeMana(extraMana);

            return new AttackResult(damage, "llença la disrupció arcana amb un crític devastador!");
        }

        return new AttackResult(damage, "llença la disrupció arcana.");
    }

    /**
     * Dispara múltiples projectils consecutius amb una ballesta.
     *
     * <p>
     * Cada tret pot permetre continuar amb un altre segons la sort.
     * La probabilitat de continuar decreix progressivament i el dany
     * es redueix lleugerament a cada projectil.
     * </p>
     *
     * <p>
     * Es limita a un màxim de 4 trets consecutius.
     * </p>
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques del personatge (la sort influeix en la continuïtat)
     * @param rng    generador aleatori
     * @return resultat amb el dany total acumulat i informació de crítics
     */
    public static AttackResult luckyBallista(Weapon weapon, Statistics stats, Random rng) {
        double totalDamage = 0.0;
        int shots = 0;
        int crits = 0;

        double luck = stats.getLuck();

        double continueChance = 0.50 + (luck * 0.005);
        continueChance = Math.clamp(continueChance, 0.40, 0.75);

        double decayChance = 0.18;

        double damageMultiplier = 1.0;
        double damageDecay = 0.15;

        boolean loop = true;
        while (shots < 4 && loop) {
            if (shots > 0 && rng.nextDouble() > continueChance) {
                loop = false;
                continue;
            }

            double baseDamage = weapon.basicAttack(stats, rng);
            double finalDamage = baseDamage * damageMultiplier;

            totalDamage += finalDamage;
            shots++;

            if (weapon.lastWasCritic()) {
                crits++;
                continueChance += 0.05;
            }

            continueChance -= decayChance;
            continueChance = Math.clamp(continueChance, 0.05, 0.90);

            damageMultiplier -= damageDecay;
            damageMultiplier = Math.max(0.40, damageMultiplier);
        }

        totalDamage = round2(totalDamage);

        String message = switch (shots) {
            case 1 -> "La ballista dispara un únic projectil.";
            case 4 -> "La sort desferma una pluja de quatre projectils!";
            default -> "La ballista dispara " + shots + " projectils consecutius.";
        };

        if (crits > 0) {
            message += " (" + crits + " crític" + (crits > 1 ? "s" : "") + ")";
        }

        return new AttackResult(totalDamage, message);
    }

    /**
     * Minijoc de mecanografiat: el jugador ha de recitar un codi i el dany
     * s'ajusta segons el temps i l'encert.
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques (mana i destresa afecten el resultat)
     * @param rng    generador aleatori
     * @return resultat amb multiplicador aplicat i missatge
     */
    public static AttackResult grimoriCipher(Weapon weapon, Statistics stats, Random rng) {
        double manaCost = weapon.getManaPrice();
        if (manaCost > 0 && !stats.consumeMana(manaCost)) {
            return new AttackResult(0, "intenta llegir el grimori, però no té prou mana.");
        }

        String expectedStr = grimoriCodeGenerator.generate();

        printGrimorieGame(expectedStr);

        long start = System.nanoTime();
        String typed = IN.nextLine();
        long end = System.nanoTime();

        double seconds = (end - start) / 1_000_000_000.0;

        System.out.println();

        boolean correct = typed != null && typed.trim().equals(expectedStr);

        int dex = stats.getDexterity();
        double dexGrace = Math.clamp(dex * 0.02, 0.0, 0.6);

        final double fast = 1.8 + (dexGrace * 0.20);
        final double ok = 3.0 + (dexGrace * 0.70);
        final double slow = 5.0 + dexGrace;

        double multiplier;
        if (seconds <= ok) {
            double t = (seconds - fast) / (ok - fast);
            t = clamp01(t);

            double curve = Math.pow(t, 0.85);
            multiplier = 1.25 - (0.25 * curve);
        } else {
            double t = (seconds - ok) / (slow - ok);
            t = clamp01(t);

            double curve = Math.pow(t, 1.10);
            multiplier = 1.00 - (0.28 * curve);
        }

        multiplier = Math.clamp(multiplier, 0.72, 1.25);

        if (!correct) {
            multiplier *= 0.80;
            multiplier = Math.clamp(multiplier, 0.55, 1.25);
        }

        double baseDamage = weapon.basicAttack(stats, rng);
        double finalDamage = baseDamage * multiplier;

        boolean crit = weapon.lastWasCritic();

        String msg = correct
                ? String.format("recita el codi en %.2fs i altera el dany (x%.2f).", seconds, multiplier)
                : String.format("s'equivoca amb el codi en %.2fs i el grimori el castiga (x%.2f).", seconds,
                        multiplier);

        if (crit) {
            msg += " (crític)";
        }

        finalDamage = round2(finalDamage);
        return new AttackResult(finalDamage, msg);
    }

    /**
     * Llença una daga amb alta penetració.
     *
     * <p>
     * Ignora un percentatge del dany reduït (simulant penetració).
     * Si és crític, augmenta la penetració aplicada.
     * És una habilitat consistent i precisa, amb poc component aleatori.
     * </p>
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques (destresa afecta la penetració)
     * @param rng    generador aleatori
     * @return resultat amb dany i missatge
     */
    public static AttackResult perforatingThrow(Weapon weapon, Statistics stats, Random rng) {
        double baseDamage = weapon.basicAttack(stats, rng);

        double penetrationMultiplier = 1.18;

        double dexBonus = Math.clamp(stats.getDexterity() * 0.003, 0.0, 0.10);
        penetrationMultiplier += dexBonus;

        boolean crit = weapon.lastWasCritic();
        if (crit) {
            penetrationMultiplier += 0.10;
        }

        double finalDamage = baseDamage * penetrationMultiplier;
        finalDamage = round2(finalDamage);

        String message = crit
                ? "llença la daga amb una penetració devastadora! (crític)"
                : "llença la daga amb una precisió perforant.";

        return new AttackResult(finalDamage, message);
    }

    public static AttackResult chronoWeave(Weapon weapon, Statistics stats, Random rng) {
        final int simulations = 3;

        double[] damages = new double[simulations];
        boolean[] crits = new boolean[simulations];

        for (int i = 0; i < simulations; i++) {
            damages[i] = weapon.basicAttack(stats, rng);
            crits[i] = weapon.lastWasCritic();
        }

        for (int i = 0; i < simulations - 1; i++) {
            boolean swapped = false;

            for (int j = i + 1; j < simulations; j++) {
                if (damages[j] < damages[i]) {
                    double tmpD = damages[i];
                    damages[i] = damages[j];
                    damages[j] = tmpD;

                    boolean tmpC = crits[i];
                    crits[i] = crits[j];
                    crits[j] = tmpC;

                    swapped = true;
                }
            }

            if (!swapped) {
                break;
            }
        }

        int intelligence = stats.getIntelligence();
        int luck = stats.getLuck();

        double bestChance = 0.20 + intelligence * 0.005 + luck * 0.003;
        bestChance = Math.clamp(bestChance, 0.20, 0.60);

        double worstChance = 0.30 - intelligence * 0.004;
        worstChance = Math.clamp(worstChance, 0.10, 0.30);

        double roll = rng.nextDouble();

        int chosenIndex;
        if (roll < bestChance) {
            chosenIndex = 0; // ++
        } else if (roll < bestChance + (1.0 - bestChance - worstChance)) {
            chosenIndex = 1; // ===
        } else {
            chosenIndex = 2; // --
        }

        double finalDamage = damages[chosenIndex];
        boolean finalCrit = crits[chosenIndex];

        finalDamage = round2(finalDamage);

        String message = switch (chosenIndex) {
            case 2 -> "entreveu múltiples futurs i tria el més favorable.";
            case 1 -> "teixeix el temps amb resultat estable.";
            default -> "el temps es distorsiona i pren el pitjor camí possible.";
        };

        if (finalCrit) {
            message += " (crític)";
        }

        return new AttackResult(finalDamage, message);
    }

    // -------------------------------------------------------------------------
    // UI / helpers interns
    // -------------------------------------------------------------------------

    private static void printGrimorieGame(String expected) {
        final String title = BOLD + CYAN + "DESAFIAMENT DEL GRIMORI" + RESET;
        final String instr = WHITE + "Escriu el codi (5 digits: 1-4)" + RESET;
        final String hint = DIM + "Prem ENTER quan acabis" + RESET;

        String codeSpaced = spaced(expected);
        final String codeLabel = BOLD + YELLOW + "[ " + codeSpaced + " ]" + RESET;

        UnaryOperator<String> center = text -> {
            String visible = stripAnsi(text);

            int pad = Math.max(0, (GRIMORI_WIDTH - visible.length()) / 2);
            int rem = Math.max(0, GRIMORI_WIDTH - pad - visible.length());

            return "|" + " ".repeat(pad) + text + " ".repeat(rem) + "|";
        };

        StringBuilder sb = new StringBuilder(1024);

        sb.append('\n')
                .append(DIM).append("El grimori brilla amb energia arcana...").append(RESET)
                .append("\n\n");

        sb.append(GRIMORI_LINE).append('\n');
        sb.append(center.apply(title)).append('\n');
        sb.append(GRIMORI_SEP).append('\n');
        sb.append(center.apply(instr)).append('\n');
        sb.append(center.apply(hint)).append('\n');
        sb.append(GRIMORI_SEP).append('\n');
        sb.append(center.apply(codeLabel)).append('\n');
        sb.append(GRIMORI_LINE).append('\n');
        sb.append(BOLD).append(GREEN).append("> ").append(RESET);

        System.out.print(sb.toString());
    }

    private static String stripAnsi(String s) {
        return ANSI_PATTERN.matcher(s).replaceAll("");
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static double clamp01(double n) {
        return Math.clamp(n, 0.0, 1.0);
    }

    private static String spaced(String s) {
        int n = s.length();
        if (n == 0) {
            return s;
        }

        StringBuilder sb = new StringBuilder(n * 2 - 1);
        sb.append(s.charAt(0));

        for (int i = 1; i < n; i++) {
            sb.append(' ').append(s.charAt(i));
        }

        return sb.toString();
    }
}