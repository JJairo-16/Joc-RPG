package models.weapons;

import static models.weapons.Target.*;

import java.util.Random;
import java.util.Scanner;
import java.util.function.UnaryOperator;

import static utils.ui.Ansi.*;

import models.characters.Statistics;
import utils.rng.GrimoriCodeGenerator;

/**
 * Habilitats especials relacionades amb armes.
 */
public class Skills {

    private Skills() {
    }

    private static final GrimoriCodeGenerator grimoriCodeGenerator = new GrimoriCodeGenerator(5, 1, 4, 2);

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
     * Si impacta a l'enemic, aplica un petit bonus de dany.
     * Si impacta a un mateix, aplica una reducció de dany i canvia l'objectiu a
     * {@link Target#SELF}.
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
        return new AttackResult(totalDamage,
                "ha atacat amb una talla transversal [" + criticalPercent + "% crític].");
    }

    /**
     * Llença una disrupció arcana que consumeix mana.
     *
     * Té una probabilitat de fallar influenciada per la sort.
     * Si té èxit, aplica l'atac bàsic.
     * En cas de crític, consumeix un 50% addicional del cost de mana.
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques (inclou consum de mana i sort)
     * @param rng    generador aleatori
     * @return resultat de l'habilitat amb dany o fallida
     */
    public static AttackResult arcaneDisruption(Weapon weapon, Statistics stats, Random rng) {
        double baseManaCost = weapon.getManaPrice();
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

            return new AttackResult(damage,
                    "llença la disrupció arcana amb un crític devastador!");
        }

        return new AttackResult(damage, "llença la disrupció arcana.");
    }

    /**
     * Dispara múltiples projectils consecutius amb una ballesta.
     *
     * Cada tret pot permetre continuar amb un altre segons la sort.
     * La probabilitat de continuar decreix progressivament i el dany
     * es redueix lleugerament a cada projectil.
     *
     * Es limita a un màxim de 4 trets consecutius.
     *
     * @param weapon arma utilitzada
     * @param stats  estadístiques del personatge (la sort influeix en la
     *               continuïtat)
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

        totalDamage = Math.round(totalDamage * 100.0) / 100.0;

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

    public static AttackResult grimoriCipher(Weapon weapon, Statistics stats, Random rng) {
        double manaCost = weapon.getManaPrice();
        if (manaCost > 0 && !stats.consumeMana(manaCost)) {
            return new AttackResult(0, "intenta llegir el grimori, però no té prou mana.");
        }

        String expectedStr = grimoriCodeGenerator.generate();
        StringBuilder expected = new StringBuilder(expectedStr);

        // ── UI: més neta i “centrada” (sense caràcters estranys)
        printGrimorieGame(expected);

        long start = System.nanoTime();

        // IMPORTANT: no tancar System.in
        Scanner sc = new Scanner(System.in);
        String typed = sc.nextLine();

        long end = System.nanoTime();
        double seconds = (end - start) / 1_000_000_000.0;

        System.out.println();

        boolean correct = typed != null && typed.trim().equals(expected.toString());

        // ── Destreza: dona marge perquè el temps no decaigui tant
        int dex = stats.getDexterity();
        // fins a ~+0.9s de "perdó" (cap a partir de DEX alta), sense trencar el balanç
        double dexGrace = Math.clamp(dex * 0.02, 0.0, 0.6);

        // ── Decaig més ràpid que abans (base més exigent), però DEX ho compensa
        final double fast = 1.85 + (dexGrace * 0.20); // ràpid
        final double ok = 3.1 + (dexGrace * 0.70); // neutre
        final double slow = 5.0 + dexGrace; // lent

        double multiplier;
        if (seconds <= ok) {
            // de 1.25 cap a 1.00
            double t = (seconds - fast) / (ok - fast);
            t = Math.clamp(t, 0.0, 1.0);

            double curve = Math.pow(t, 0.85);

            multiplier = 1.25 - (0.25 * curve);
        } else {
            // de 1.00 cap a 0.72 (una mica més de càstig per tardar)
            double t = (seconds - ok) / (slow - ok);
            t = Math.clamp(t, 0.0, 1.0);

            double curve = Math.pow(t, 1.10);

            multiplier = 1.00 - (0.28 * curve);
        }

        // Clamp principal (mantens el màxim)
        multiplier = Math.clamp(multiplier, 0.72, 1.25);

        // Fallar: penalitza una mica més que abans (però controlat)
        if (!correct) {
            multiplier *= 0.80; // abans 0.85
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

        finalDamage = Math.round(finalDamage * 100.0) / 100.0;
        return new AttackResult(finalDamage, msg);
    }

    private static void printGrimorieGame(StringBuilder expected) {

        final int width = 50;

        String line = "+" + "-".repeat(width) + "+";
        String sep = "|" + "-".repeat(width) + "|";

        String title = BOLD + CYAN + "DESAFIAMENT DEL GRIMORI" + RESET;
        String instr = WHITE + "Escriu el codi (5 digits: 1-4)" + RESET;
        String hint = DIM + "Prem ENTER quan acabis" + RESET;

        String codeSpaced = expected.toString().replace("", " ").trim();
        String codeLabel = BOLD + YELLOW + "[ " + codeSpaced + " ]" + RESET;

        UnaryOperator<String> center = text -> {
            String visible = stripAnsi(text);
            int pad = Math.max(0, (width - visible.length()) / 2);
            int rem = Math.max(0, width - pad - visible.length());
            return "|" + " ".repeat(pad) + text + " ".repeat(rem) + "|";
        };

        System.out.println("\n" + DIM + "El grimori brilla amb energia arcana..." + RESET + "\n");

        System.out.println(line);
        System.out.println(center.apply(title));
        System.out.println(sep);
        System.out.println(center.apply(instr));
        System.out.println(center.apply(hint));
        System.out.println(sep);
        System.out.println(center.apply(codeLabel));
        System.out.println(line);

        System.out.print(BOLD + GREEN + "> " + RESET);
    }

    private static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    /**
     * Llença una daga amb alta penetració.
     *
     * Ignora un percentatge del dany reduït (simulant penetració).
     * Si és crític, augmenta la penetració aplicada.
     *
     * És una habilitat consistent i precisa, amb poc component aleatori.
     */
    public static AttackResult perforatingThrow(Weapon weapon, Statistics stats, Random rng) {

        double baseDamage = weapon.basicAttack(stats, rng);

        // Penetració base
        double penetrationMultiplier = 1.18;

        // DEX augmenta lleugerament la penetració (controlat)
        double dexBonus = Math.clamp(stats.getDexterity() * 0.003, 0.0, 0.10);
        penetrationMultiplier += dexBonus;

        boolean crit = weapon.lastWasCritic();

        if (crit) {
            penetrationMultiplier += 0.10; // crític = més penetració
        }

        double finalDamage = baseDamage * penetrationMultiplier;
        finalDamage = Math.round(finalDamage * 100.0) / 100.0;

        String message = crit
                ? "llença la daga amb una penetració devastadora! (crític)"
                : "llença la daga amb una precisió perforant.";

        return new AttackResult(finalDamage, message);
    }
}