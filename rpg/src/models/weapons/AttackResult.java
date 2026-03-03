package models.weapons;

/**
 * Representa el resultat d'una acció d'atac.
 *
 * @param damage quantitat de dany generat per l'atac
 * @param message missatge descriptiu associat a l'acció
 * @param target objectiu real del dany ({@link Target})
 *
 * <p>És un record immutable que encapsula tota la informació necessària
 * perquè el sistema de combat resolgui l'aplicació del dany i mostri
 * el missatge corresponent.</p>
 */
public record AttackResult(double damage, String message, Target target) {

    /**
     * Crea un resultat d'atac assumint que l'objectiu és l'enemic.
     */
    public AttackResult(double damage, String message) {
        this(damage, message, Target.ENEMY);
    }
}