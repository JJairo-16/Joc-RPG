package game;

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
import utils.input.WeaponMenu;
import utils.ui.Cleaner;
import utils.ui.Prettier;

/**
 * Controla el bucle principal del combat per torns entre dos jugadors.
 */
public class GameLoop {

    private static final List<String> OPTIONS = List.of(
            "Canviar arma",
            "Atacar",
            "Defensar-se",
            "Esquivar",
            "Veure informació"
    );

    private final Character player1;
    private final Character player2;

    private final CombatSystem combatSystem;
    private final Cleaner cls = new Cleaner();

    public GameLoop(Character player1, Character player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.combatSystem = new CombatSystem(player1, player2);
    }

    /**
     * Inicia el combat i el manté en execució fins que hi hagi un vencedor (o empat).
     */
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

    /**
     * Mostra el resultat final del combat.
     */
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

    /**
     * Gestiona el torn d'un jugador fins que triï una acció vàlida.
     */
    private Action playTurn(Character player) {
        Action action = null;

        do {
            cls.clear();
            int option = Menu.getOption(OPTIONS, "Accions de " + player.getName());

            switch (option) {
                case 1:
                    changeWeapon(player);
                    break;

                case 2:
                    action = Action.ATTACK;
                    break;

                case 3:
                    action = Action.DEFEND;
                    break;

                case 4:
                    action = Action.DODGE;
                    break;

                case 5:
                    cls.clear();
                    showPlayerInfo(player);
                    System.out.println();
                    Menu.pause();
                    break;

                default:
                    break;
            }
        } while (action == null);

        return action;
    }

    /**
     * Permet al jugador equipar una arma de l'arsenal (amb filtres) si compleix requisits.
     */
    private void changeWeapon(Character player) {
        Weapon weapon = null;
        boolean loop = true;

        Statistics stats = player.geStatistics();
        boolean equipped = false;

        // Llista d'armes reals
        List<Arsenal> entries = Arsenal.getSortedWeapons();
        WeaponMenu.FilterState filters = new WeaponMenu.FilterState();

        do {
            cls.clear();

            Arsenal selected = WeaponMenu.chooseWeaponEntryWithFilters(
                    entries,
                    "Armes de l'arsenal",
                    stats,
                    filters
            );

            if (selected == null) {
                loop = false; // Cancel·lar
                continue;
            }

            weapon = selected.create();

            if (weapon.canEquip(stats)) {
                loop = false;
                equipped = true;
            } else {
                Prettier.warn("No compleixes els requisits per equipar aquesta arma.");
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

    /**
     * Mostra la informació del jugador: dades bàsiques, raça, estadístiques i arma equipada.
     */
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

        // Barres visuals (mantinc la teva API)
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