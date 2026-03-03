package models.weapons;

import java.util.Random;
import models.characters.Statistics;

/**
 * Representa el comportament d'una acció d'atac.
 *
 * <p>És una interfície funcional que permet definir habilitats o
 * efectes d'arma mitjançant expressions lambda o referències a mètode.</p>
 *
 * <p>El mètode {@code execute} calcula el resultat complet de l'atac
 * (dany, missatge i objectiu) a partir de l'arma, les estadístiques
 * del personatge i un generador aleatori.</p>
 */
@FunctionalInterface
public interface Attack {

    AttackResult execute(Weapon weapon, Statistics stats, Random rng);
}