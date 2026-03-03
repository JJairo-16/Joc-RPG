package game;

import java.util.ArrayList;
import java.util.List;

import combat.Action;
import combat.CombatSystem;
import combat.Winner;

import models.characters.Breed;
import models.characters.Character;
import models.characters.Statistics;
import models.weapons.Arsenal;
import models.weapons.Weapon;

import utils.input.Menu;
import utils.ui.Cleaner;
import utils.ui.Prettier;

public class GameLoop {
    private final Character player1;
    private final Character player2;

    private final CombatSystem combatSystem;
    private final Cleaner cls = new Cleaner();

    public GameLoop(Character player1, Character player2) {
        this.player1 = player1;
        this.player2 = player2;

        this.combatSystem = new CombatSystem(player1, player2);

        List<String> weaponsList = new ArrayList<>(Arsenal.getNamesList());
        weaponsList.addFirst("Cancelar");
        weapons = List.copyOf(weaponsList);
    }

    public void init() {
        cls.clear();

        Winner winner;
        do {
            Action action1 = playTurn(player1);
            Action action2 = playTurn(player2);

            cls.clear();
            winner = combatSystem.play(action1, action2);
            Menu.pause();

        } while (winner == Winner.NONE);

        finish(winner);
    }

    private void finish(Winner winner) {
        cls.clear();

        System.out.println("====================================");
        System.out.println("           FI DEL COMBAT");
        System.out.println("====================================");
        System.out.println();

        switch (winner) {
            case PLAYER1:
                System.out.println("🏆 VICTÒRIA!");
                System.out.printf("%s ha guanyat el combat!%n", player1.getName());
                System.out.printf("%s ha caigut derrotat.%n", player2.getName());
                break;

            case PLAYER2:
                System.out.println("🏆 VICTÒRIA!");
                System.out.printf("%s ha guanyat el combat!%n", player2.getName());
                System.out.printf("%s ha caigut derrotat.%n", player1.getName());
                break;

            case TIE:
                System.out.println("⚔ EMPAT!");
                System.out.println("Tots dos combatents han caigut al mateix temps.");
                System.out.println("No hi ha vencedor en aquesta batalla.");
                break;

            default:
                System.out.println("El combat ha finalitzat.");
                break;
        }

        System.out.println();
        System.out.println("====================================");
    }

    private final List<String> options = List.of(
            "Canviar arma",
            "Atacar",
            "Defensar-se",
            "Esquivar",
            "Veure informació",
            "Continuar");

    private Action playTurn(Character player) {
        Action selected = null;
        boolean loop = true;

        do {
            cls.clear();
            int option = Menu.getOption(options, "Accions de " + player.getName());

            switch (option) {
                case 1:
                    changeWeapon(player);
                    break;

                case 2:
                    selected = Action.ATTACK;
                    break;

                case 3:
                    selected = Action.DEFEND;
                    break;

                case 4:
                    selected = Action.DODGE;
                    break;

                case 5:
                    cls.clear();
                    showPlayerInfo(player);
                    break;

                case 6:
                    if (selected != null)
                        loop = false;
                    else
                        Prettier.warn("Seleccioni una acció a realitzar, si us pau.");
                    break;

                default:
                    break;
            }

            if (loop) {
                System.out.println();
                Menu.pause();
            }
        } while (loop);

        return selected;
    }

    private final List<String> weapons;

    private void changeWeapon(Character player) {
        Weapon weapon = null;
        boolean loop = true;

        Statistics stats = player.geStatistics();
        boolean equipped = false;

        do {
            cls.clear();

            int option = Menu.getOption(weapons, "Armes de l'arsenal");
            if (option == 1) {
                loop = false;
                continue;
            }

            weapon = Arsenal.getWeaponByIdx(option - 2);

            if (weapon.canEquip(stats)) {
                loop = false;
                equipped = true;
            } else {
                Prettier.warn("No compleixes els requisits per equipar aquesta arma.");
            }

            if (loop) {
                System.out.println();
                Menu.pause();
            }

        } while (loop);

        if (!equipped) {
            Prettier.info("No s'ha realitzat cap canvi d'arma.");
            return;
        }

        player.setWeapon(weapon);
        Prettier.info("S'ha equipat l'arma %s.", weapon.getName());
    }

    private void showPlayerInfo(Character player) {
        Statistics stats = player.geStatistics();
        Breed breed = player.getBreed();
        Weapon weapon = player.getWeapon();

        StringBuilder sb = new StringBuilder();

        Prettier.printTitle("Informació del jugador");
        sb.append("\n");

        // Nom i edat
        sb.append("Nom: ").append(player.getName()).append("\n");
        sb.append("Edat: ").append(player.getAge()).append("\n\n");

        // Raça + descripció + bonus
        int bonus = (int) Math.round(breed.bonus() * 100.0);
        sb.append("Raça: ").append(breed.getName()).append("\n");
        sb.append("Descripció: ").append(breed.getDescription()).append("\n");
        sb.append("Bonus racial: +").append(bonus).append("% a ")
                .append(breed.bonusStat().getName())
                .append("\n\n");

        // Estadístiques
        sb.append("Estadístiques:\n");
        sb.append("  Força: ").append(stats.getStrength()).append("\n");
        sb.append("  Destresa: ").append(stats.getDexterity()).append("\n");
        sb.append("  Constitució: ").append(stats.getConstitution()).append("\n");
        sb.append("  Intel·ligència: ").append(stats.getIntelligence()).append("\n");
        sb.append("  Saviesa: ").append(stats.getWisdom()).append("\n");
        sb.append("  Carisma: ").append(stats.getCharisma()).append("\n");
        sb.append("  Sort: ").append(stats.getLuck()).append("\n\n");

        System.out.print(sb);

        // Barras visuales (mantengo tu API)
        CombatSystem.printStatusBars(player);
        System.out.println();

        // Arma equipada
        if (weapon != null) {
            StringBuilder weaponInfo = new StringBuilder();

            weaponInfo.append("Arma equipada: ").append(weapon.getName()).append("\n");
            weaponInfo.append("Descripció: ").append(weapon.getDescription()).append("\n\n");

            weaponInfo.append("Estadístiques de l'arma:\n");
            weaponInfo.append("  Tipus: ").append(weapon.getType()).append("\n");
            weaponInfo.append("  Dany base: ").append(weapon.getBaseDamage()).append("\n");
            weaponInfo.append(String.format("  Probabilitat de crític: %.2f%%%n", weapon.getCriticalProb() * 100));
            weaponInfo.append(String.format("  Multiplicador crític: x%.2f%n", weapon.getCriticalDamage()));
            weaponInfo.append(String.format("  Cost de mana: %.2f%n", weapon.getManaPrice()));

            System.out.println(weaponInfo);
        } else {
            System.out.println("Arma equipada: Cap\n");
        }
    }
}
