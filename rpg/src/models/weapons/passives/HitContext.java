package models.weapons.passives;

import models.characters.Character;
import models.characters.Result;
import models.weapons.AttackResult;

/**
 * Context immutable d'un impacte:
 * qui ataca/defensa, què pretenia fer l'atac i quin va ser el resultat real.
 */
public record HitContext(
        Character attacker,
        Character defender,
        AttackResult attackResult, // el que “intentava” fer l'atac
        Result defenderResult,     // el que realment rep el defensor (dmg + missatge)
        double damageDealt         // dany real aplicat (després de DODGE/DEFEND)
) {}