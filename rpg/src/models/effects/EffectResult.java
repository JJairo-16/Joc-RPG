package models.effects;

/**
 * Resultat d'una execució d'efecte en una fase.
 *
 * <p>Permet retornar missatge i indicar si l'efecte ha consumit recursos.</p>
 */
public record EffectResult(
        String message,
        boolean consumedCharge,
        boolean changedState
) {
    public static EffectResult none() {
        return new EffectResult(null, false, false);
    }

    public static EffectResult msg(String message) {
        return new EffectResult(message, false, false);
    }

    public static EffectResult consumed(String message) {
        return new EffectResult(message, true, true);
    }

    public static EffectResult changed(String message) {
        return new EffectResult(message, false, true);
    }
}