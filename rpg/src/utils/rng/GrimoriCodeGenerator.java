package utils.rng;

import java.security.SecureRandom;

/**
 * Generador de codis per al minijoc del Grimori.
 *
 * Genera codis de longitud configurable amb dígits dins d'un rang,
 * evitant ratxes excessives del mateix número consecutiu.
 */
public class GrimoriCodeGenerator {

    private final SecureRandom rng = new SecureRandom();

    private final int length;
    private final int minDigit;
    private final int maxConsecutive; // màxim consecutius iguals permesos
    private final int range; // quantitat de dígits possibles

    /**
     * Crea un nou generador de codis.
     *
     * @param length         longitud del codi
     * @param minDigit       dígit mínim permès (inclòs)
     * @param maxDigit       dígit màxim permès (inclòs)
     * @param maxConsecutive màxim de dígits consecutius iguals permesos (>= 1)
     */
    public GrimoriCodeGenerator(int length, int minDigit, int maxDigit, int maxConsecutive) {
        if (length < 0) {
            throw new IllegalArgumentException("La longitud ha de ser >= 0");
        }
        if (minDigit > maxDigit) {
            throw new IllegalArgumentException("minDigit ha de ser <= maxDigit");
        }
        if (maxConsecutive < 1) {
            throw new IllegalArgumentException("maxConsecutive ha de ser >= 1");
        }

        this.length = length;
        this.minDigit = minDigit;
        this.maxConsecutive = maxConsecutive;

        this.range = maxDigit - minDigit + 1;
        if (this.range <= 0) {
            // Cobertura extra per rangs estranys (overflow teòric)
            throw new IllegalArgumentException("Rang de dígits invàlid");
        }

        // Si només hi ha un dígit possible, l'única seqüència vàlida és repetir-lo 'length' cops.
        if (range == 1 && length > 0 && maxConsecutive < length) {
            throw new IllegalArgumentException(
                    "Configuració impossible: només hi ha un dígit al rang i maxConsecutive és massa petit");
        }
    }

    /**
     * Genera un nou codi complint les restriccions configurades.
     *
     * @return el codi generat en format String
     */
    public String generate() {
        if (length == 0) {
            return "";
        }

        StringBuilder code = new StringBuilder(length);

        int last = 0;
        boolean hasLast = false;

        // streak = nombre de repeticions consecutives de 'last' (0 = cap repetició)
        int streak = 0;

        for (int i = 0; i < length; i++) {
            final boolean atLimit = hasLast && (streak >= maxConsecutive - 1);

            int digit;
            if (!atLimit) {
                digit = randomDigit();
            } else {
                // Tria uniforme dins del rang excloent 'last' en O(1):
                // escull un índex a [0, range-2] i "salta" el valor 'last'
                int r = rng.nextInt(range - 1);
                int candidate = minDigit + r;
                digit = (candidate >= last) ? candidate + 1 : candidate;
            }

            if (hasLast && digit == last) {
                streak++;
            } else {
                streak = 0;
            }

            code.append(digit);
            last = digit;
            hasLast = true;
        }

        return code.toString();
    }

    private int randomDigit() {
        return minDigit + rng.nextInt(range);
    }
}