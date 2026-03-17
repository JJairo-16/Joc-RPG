package models.breeds;

import models.characters.Character;
import models.characters.Stat;
import models.characters.Statistics;
import models.weapons.AttackResult;
import models.weapons.Weapon;
import models.weapons.WeaponType;

public class Orc extends Character {
    private final Statistics modStats;
    private static final double ATTACK_PHISICAL_BONUS = 1.2;

    public Orc(String name, int age, int[] stats) {
        super(name, age, stats, Breed.ORC);
        
        int[] tmp = applyBreed(stats, Breed.ORC);
        tmp[Stat.STRENGTH.ordinal()] *= ATTACK_PHISICAL_BONUS;
        this.modStats = new Statistics(tmp);
    }


    @Override
    public boolean setWeapon(Weapon w) {
        if (!w.canEquip(stats)) {
            return false;
        }

        if (w.getType() == WeaponType.MAGICAL) {
            return false;
        }

        this.weapon = w;
        return true;
    }

    @Override
    public AttackResult attack() {
        if (weapon == null)
            return attackUnarmed(modStats);

        Statistics statsToUse = isPhisicalWeapon(weapon) ? modStats : stats;
        return weapon.attack(statsToUse, rng);
    }

    private AttackResult attackUnarmed(Statistics stats) {
        return new AttackResult(
                    WeaponType.PHYSICAL.getBasicDamage(15, stats),
                    "ataca amb les mans desnudes.");
    }

    private boolean isPhisicalWeapon(Weapon w) {
        if (w == null)
            return true;

        return w.getType() == WeaponType.PHYSICAL;
    }
}
