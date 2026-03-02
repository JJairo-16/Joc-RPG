package utils.rng;

import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGeneratorFactory;

import models.characters.*;

/**
 * Generador de stats amb pressupost (budget) i selecció automàtica de raça.
 * <p>
 * IMPORTANT: les stats retornades són les "base" (sense aplicar el bonus de
 * raça).
 * La raça es retorna a part perquè el caller pugui aplicar el bonus quan
 * pertoqui.
 * </p>
 *
 * <p>
 * Fonts d'atac suportades: STR, DEX, INT.
 * Restriccions:
 * <ul>
 * <li>STR i INT són oposades de manera estricta (mai conviuen com a fonts
 * d'atac).</li>
 * <li>DEX no s'oposa estrictament a STR.</li>
 * <li>Si DEX és la principal (rang), la sinergia és només amb STR (i INT queda
 * fora com a atac).</li>
 * <li>Si DEX no és la principal, pot sinergitzar amb STR o INT segons quina
 * sigui la principal (DEX->INT una mica menor).</li>
 * </ul>
 * </p>
 *
 * <p>
 * API pública mínima: només {@link #generate(int, long)}.
 * </p>
 */
public final class StatsBudget {

    private static final Stat[] STATS = Stat.values();
    private static final int STAT_COUNT = STATS.length;

    // Índexos d'estadístiques clau (per evitar repetir ordinal() en lògica interna)
    private static final int STR = Stat.STRENGTH.ordinal();
    private static final int DEX = Stat.DEXTERITY.ordinal();
    private static final int CON = Stat.CONSTITUTION.ordinal();
    private static final int INT = Stat.INTELLIGENCE.ordinal();

    private StatsBudget() {
        // Classe utilitària: no instanciable
    }

    /**
     * Resultat de la generació: stats base + raça escollida.
     * <p>
     * Les stats es retornen en un array en l'ordre de {@link Stat#values()}.
     * </p>
     */
    public static record Result(int[] baseStats, Breed breed) {
        public Result {
            // Defensa bàsica: evitem que des de fora modifiquin l'array retornat
            baseStats = (baseStats == null) ? null : baseStats.clone();
        }

        /**
         * Evita filtracions accidentals a logs o depuració.
         */
        @Override
        public String toString() {
            return "Result[ocult]";
        }
    }

    /**
     * Genera totes les stats base (sense aplicar bonus de raça) i, en base a
     * aquestes,
     * escull una raça coherent amb l'arquetip d'atac i evitant híbrids STR+INT.
     *
     * @param totalPoints total de punts a repartir entre totes les stats
     * @return {@link Result} amb les stats base i la raça escollida
     * @throws IllegalArgumentException si els límits calculats fan impossible
     *                                  repartir els punts
     */
    public static Result generate(int totalPoints) {
        long seed = getNewSeed();
        return generate(totalPoints, seed);
    }

    /**
     * Genera totes les stats base (sense aplicar bonus de raça) i, en base a
     * aquestes,
     * escull una raça coherent amb l'arquetip d'atac i evitant híbrids STR+INT.
     *
     * @param totalPoints total de punts a repartir entre totes les stats
     * @param seed        llavor per fer la generació determinista
     * @return {@link Result} amb les stats base i la raça escollida
     * @throws IllegalArgumentException si els límits calculats fan impossible
     *                                  repartir els punts
     */
    public static Result generate(int totalPoints, long seed) {
        Random random = new Random(seed);

        // 1) Calcular límits i paràmetres automàtics (interns)
        ScaledLimits limits = scaleLimits(totalPoints);
        AutoParams ap = autoParams(totalPoints);

        // 2) Escollir arquetip d'atac (STR / DEX / INT), amb oposicions aplicades
        FocusSelection selection = chooseFocusWithThreeAttackSources(ap.focusCount(), new Random(seed));

        // 3) Generar stats BASE (sense bonus) coherent amb el focus
        int[] baseStats = generateBaseStats(
                totalPoints,
                limits.minValue(),
                limits.maxValue(),
                selection,
                ap.focusShare(),
                ap.dumpCount(),
                ap.dumpCapShare(),
                ap.conFloorShare(),
                random);

        // 4) Escollir raça segons stats BASE (sense aplicar cap bonus)
        Breed breed = chooseBreed(baseStats, selection, random);

        return new Result(baseStats, breed);
    }

    private static long getNewSeed() {
        return RandomGeneratorFactory.getDefault()
                .create()
                .nextLong();

    }

    // --- Tipus interns ---

    /**
     * Selecció interna del focus:
     * <ul>
     * <li>focus: màscara de stats focusejades</li>
     * <li>primaryAttack: font principal d'atac (STR/DEX/INT)</li>
     * <li>forbiddenAttack: atac prohibit per oposició estricta (STR↔INT) o per
     * regla de DEX principal</li>
     * </ul>
     */
    private static final class FocusSelection {
        final boolean[] focus;
        final int primaryAttack; // STR, DEX o INT
        final int forbiddenAttack; // STR o INT quan cal; -1 si no hi ha prohibit

        FocusSelection(boolean[] focus, int primaryAttack, int forbiddenAttack) {
            this.focus = focus;
            this.primaryAttack = primaryAttack;
            this.forbiddenAttack = forbiddenAttack;
        }
    }

    private record ScaledLimits(int minValue, int maxValue) {
    }

    private record AutoParams(
            int focusCount, double focusShare,
            int dumpCount, double dumpCapShare,
            double conFloorShare) {
    }

    // --- Generació base (tot privat) ---

    private static int[] generateBaseStats(
            int totalPoints, int minValue, int maxValue,
            FocusSelection selection,
            double focusShare,
            int dumpCount, double dumpCapShare,
            double conFloorShare,
            Random random) {

        // Validacions bàsiques
        if (minValue * STAT_COUNT > totalPoints || maxValue * STAT_COUNT < totalPoints) {
            throw new IllegalArgumentException("Impossible amb aquests límits");
        }

        // Inicialitzem totes les stats al mínim
        int[] stats = new int[STAT_COUNT];
        Arrays.fill(stats, minValue);

        int remaining = totalPoints - minValue * STAT_COUNT;

        boolean[] focus = selection.focus;

        // Dump: sempre marquem com a "dump" l'atac prohibit (si n'hi ha)
        boolean[] dump = new boolean[STAT_COUNT];
        if (selection.forbiddenAttack != -1) {
            dump[selection.forbiddenAttack] = true;
        }

        // Si l'atac principal NO és DEX, podem marcar dumps extra evitant CON i evitant
        // tocar l'atac principal.
        // Si l'atac principal és DEX, permetem que STR quedi més "net" (sinergia), i
        // evitem sobretot INT (ja prohibit).
        int[] dumpCandidates = new int[STAT_COUNT];
        int dc = 0;
        for (int i = 0; i < STAT_COUNT; i++) {
            if (i == selection.primaryAttack || i == CON) {
                continue;
            }
            if (i == selection.forbiddenAttack || focus[i]) {
                continue;
            }
            dumpCandidates[dc++] = i;
        }

        shuffle(dumpCandidates, dc, random);

        for (int i = 0; i < Math.min(dumpCount, dc); i++) {
            dump[dumpCandidates[i]] = true;
        }

        // Partim pressupost entre focus i resta
        int focusBudget = (int) Math.round(remaining * focusShare);
        focusBudget = Math.clamp(focusBudget, 0, remaining);
        int otherBudget = remaining - focusBudget;

        // Topall d'extra per stats en dump (per evitar que "pugin massa")
        int dumpExtraCap = (int) Math.round(remaining * dumpCapShare);
        dumpExtraCap = Math.max(0, dumpExtraCap);

        // Distribució: primer a focus, després a no-focus
        distributePoints(stats, minValue, maxValue, focusBudget, focus, true, dump, dumpExtraCap, random);
        distributePoints(stats, minValue, maxValue, otherBudget, focus, false, dump, dumpExtraCap, random);

        // Pis de CON (robant punts de stats "donadores")
        applyConstitutionFloor(stats, minValue, maxValue, selection,
                (int) Math.round(remaining * conFloorShare));

        return stats;
    }

    /**
     * Garanteix un mínim per CON redistribuint punts des d'altres stats.
     * <p>
     * Protegeix l'atac principal; no toca mai CON per baixar-la.
     * </p>
     */
    private static void applyConstitutionFloor(
            int[] stats, int minValue, int maxValue,
            FocusSelection selection,
            int conExtraFloor) {

        int target = minValue + Math.max(0, conExtraFloor);
        target = Math.clamp(target, minValue, maxValue);

        int need = target - stats[CON];
        if (need <= 0) {
            return;
        }

        while (need > 0 && stats[CON] < maxValue) {
            int donor = findDonorStat(stats, minValue, selection);
            if (donor == -1) {
                break;
            }

            stats[donor]--;
            stats[CON]++;
            need--;
        }
    }

    /**
     * Troba una stat donadora per baixar 1 punt i passar-lo a CON.
     * Preferència: stats no-crítiques amb valor alt, protegint l'atac principal.
     */
    private static int findDonorStat(int[] stats, int minValue, FocusSelection selection) {
        int primary = selection.primaryAttack;

        int best = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < stats.length; i++) {
            if (i == CON || i == primary || stats[i] <= minValue) {
                continue;
            }

            // Preferim baixar una mica l'atac prohibit si existeix (ja que no volem
            // potenciar-lo)
            int score = stats[i] * 10;
            if (i == selection.forbiddenAttack) {
                score += 500;
            }

            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }

        return best;
    }

    /**
     * Escull focus amb 3 fonts d'atac i regles d'oposició:
     * <ul>
     * <li>STR↔INT mai a la vegada (oposició estricta)</li>
     * <li>DEX pot ser principal o sinergia</li>
     * <li>Si DEX és principal, INT queda fora com a atac (regla de rang)</li>
     * </ul>
     */
    private static FocusSelection chooseFocusWithThreeAttackSources(int focusCount, Random random) {
        if (focusCount < 1) {
            throw new IllegalArgumentException("focusCount invàlid");
        }

        boolean[] focus = new boolean[STAT_COUNT];

        // Triem arquetip: 0=STR, 1=DEX, 2=INT
        int archetype = random.nextInt(3);
        int primary;
        int forbidden;

        switch (archetype) {
            case 0: // STR principal
                primary = STR;
                forbidden = INT;
                break;

            case 2: // INT principal
                primary = INT;
                forbidden = STR;
                break;

            default:// DEX principal
                primary = DEX;
                forbidden = INT;
                break;
        }

        // Atac principal sempre focusejat
        focus[primary] = true;

        // Candidates focusejables, aplicant oposicions
        int[] candidates = new int[STAT_COUNT];
        int c = 0;

        for (int i = 0; i < STAT_COUNT; i++) {
            if (i == primary) {
                continue;
            }
            if (i == forbidden) {
                continue;
            }
            if (!STATS[i].canFocus()) {
                continue;
            }

            // Si primary és DEX, permetem STR com a sinergia, però evitem qualsevol intent
            // d'introduir l'altre atac oposat.
            // (ja està cobert per forbidden=INT)
            candidates[c++] = i;
        }

        int maxPossible = 1 + c;
        if (focusCount > maxPossible) {
            throw new IllegalArgumentException(
                    "focusCount invàlid: només es poden focusear " + maxPossible + " stats (incloent l'atac primari)");
        }

        shuffle(candidates, c, random);

        for (int i = 0; i < focusCount - 1 && i < c; i++) {
            focus[candidates[i]] = true;
        }

        return new FocusSelection(focus, primary, forbidden);
    }

    private static void shuffle(int[] a, int n, Random random) {
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * Distribueix punts de forma aleatòria entre stats elegibles.
     * <p>
     * Respecta maxValue i, si una stat és "dump", aplica un topall d'extra
     * (dumpExtraCap).
     * </p>
     */
    private static void distributePoints(
            int[] stats, int minValue, int maxValue,
            int points,
            boolean[] focusMask, boolean wantFocus,
            boolean[] dump, int dumpExtraCap,
            Random random) {

        final int nStats = stats.length;
        if (points <= 0) {
            return;
        }

        int[] eligible = new int[nStats];
        int eligibleCount = 0;

        // Construïm el conjunt elegible:
        // - matching focus/no-focus
        // - no saturades
        // - si és dump, només si encara li queda marge dins el cap
        for (int i = 0; i < nStats; i++) {
            if (focusMask[i] != wantFocus) {
                continue;
            }
            if (stats[i] >= maxValue) {
                continue;
            }
            if (dump[i]) {
                int dumpRemaining = dumpExtraCap - (stats[i] - minValue);
                if (dumpRemaining <= 0) {
                    continue;
                }
            }
            eligible[eligibleCount++] = i;
        }

        while (points > 0 && eligibleCount > 0) {
            int pickPos = random.nextInt(eligibleCount);
            int idx = eligible[pickPos];

            // Revalidem per seguretat
            if (stats[idx] >= maxValue) {
                eligible[pickPos] = eligible[--eligibleCount];
                continue;
            }
            if (dump[idx]) {
                int dumpRemaining = dumpExtraCap - (stats[idx] - minValue);
                if (dumpRemaining <= 0) {
                    eligible[pickPos] = eligible[--eligibleCount];
                    continue;
                }
            }

            stats[idx]++;
            points--;

            boolean saturated = (stats[idx] >= maxValue)
                    || (dump[idx] && (stats[idx] - minValue) >= dumpExtraCap);

            if (saturated) {
                eligible[pickPos] = eligible[--eligibleCount];
            }
        }
    }

    /**
     * Escull una raça basada en les stats BASE i l'arquetip d'atac.
     * <p>
     * Regles:
     * <ul>
     * <li>Mai bonifica l'atac prohibit (STR↔INT; o INT si DEX és principal).</li>
     * <li>Preferència suau per bonificar l'atac principal.</li>
     * <li>Si l'atac principal és STR o INT, també pot valorar DEX com a sinergia
     * (lleu).</li>
     * <li>Si l'atac principal és DEX, valora STR com a sinergia (lleu).</li>
     * </ul>
     * </p>
     */
    private static Breed chooseBreed(int[] stats, FocusSelection selection, Random random) {
        Stat primaryAttack = STATS[selection.primaryAttack];
        Stat forbiddenAttack = (selection.forbiddenAttack == -1) ? null : STATS[selection.forbiddenAttack];

        Breed[] all = Breed.values();

        // Candidates: excloem la raça que bonifica l'atac prohibit (si existeix)
        Breed[] candidates = new Breed[all.length];
        int c = 0;
        for (Breed b : all) {
            if (forbiddenAttack != null && b.bonusStat() == forbiddenAttack) {
                continue;
            }
            candidates[c++] = b;
        }

        long totalWeight = 0L;
        long[] weights = new long[c];

        for (int i = 0; i < c; i++) {
            Breed b = candidates[i];
            int v = stats[b.bonusStat().ordinal()];

            long base = (long) v * (long) v;
            long noise = random.nextInt(1 + Math.max(0, v));
            long w = base + noise;

            // Preferència si bonifica l'atac principal
            if (b.bonusStat() == primaryAttack) {
                w += base / 3;
            }

            // Preferència suau per sinergies:
            // - STR principal: DEX és sinergia
            // - INT principal: DEX és sinergia (una mica menor)
            // - DEX principal: STR és sinergia (petita)
            if (selection.primaryAttack == STR && b.bonusStat().ordinal() == DEX) {
                w += base / 6;
            } else if (selection.primaryAttack == INT && b.bonusStat().ordinal() == DEX) {
                w += base / 8;
            } else if (selection.primaryAttack == DEX && b.bonusStat().ordinal() == STR) {
                w += base / 10;
            }

            weights[i] = w;
            totalWeight += w;
        }

        // Fallback segur
        if (totalWeight <= 0) {
            return Breed.HUMAN;
        }

        long roll = random.nextLong(totalWeight);
        long acc = 0L;
        for (int i = 0; i < c; i++) {
            acc += weights[i];
            if (roll < acc) {
                return candidates[i];
            }
        }
        return candidates[c - 1];
    }

    // --- Auto-paràmetres (privat) ---

    /**
     * Escala límits (min/max) en funció del total de punts.
     */
    private static ScaledLimits scaleLimits(int totalPoints) {
        int statCount = STAT_COUNT;

        double average = (double) totalPoints / statCount;

        int minValue = (int) Math.round(average * 0.5);
        int maxValue = (int) Math.round(average * 2.0);

        // Correcció de seguretat
        if (minValue < 1) {
            minValue = 1;
        }
        if (maxValue <= minValue) {
            maxValue = minValue + 1;
        }

        // Garantir que el total encaixa dins els límits
        if (minValue * statCount > totalPoints) {
            minValue = totalPoints / statCount;
        }
        if (maxValue * statCount < totalPoints) {
            maxValue = totalPoints;
        }

        return new ScaledLimits(minValue, maxValue);
    }

    /**
     * Càlcul automàtic de quantes stats es focusegen.
     */
    private static int autoFocusCount() {
        // Mateixa heurística que abans, però sempre respectant el límit de stats
        // focusejables
        int focusCount = Math.clamp((int) Math.round(STAT_COUNT * 0.35), 2, Math.min(4, STAT_COUNT));

        int maxFocusCount = 1 + Math.max(0, Stat.focusableCount() - 2);

        return Math.clamp(focusCount, 1, maxFocusCount);
    }

    /**
     * Paràmetres automàtics segons el "slack" del pressupost.
     */
    private static AutoParams autoParams(int totalPoints) {
        int statCount = STAT_COUNT;

        double average = (double) totalPoints / statCount;

        int minValue = (int) Math.round(average * 0.5);
        if (minValue < 1) {
            minValue = 1;
        }

        int remaining = totalPoints - minValue * statCount;
        if (remaining < 0) {
            remaining = 0;
        }

        double slackRatio = (totalPoints <= 0) ? 0.0 : (double) remaining / (double) totalPoints;

        int focusCount = autoFocusCount();
        int dumpCount = Math.clamp((int) Math.round(statCount * 0.15), 0, Math.min(2, statCount - 1));

        double focusShare = Math.clamp(0.78 - 0.35 * slackRatio, 0.55, 0.75);
        double dumpCapShare = Math.clamp(0.16 - 0.10 * slackRatio, 0.08, 0.15);
        double conFloorShare = Math.clamp(0.14 - 0.08 * slackRatio, 0.08, 0.14);

        return new AutoParams(focusCount, focusShare, dumpCount, dumpCapShare, conFloorShare);
    }
}