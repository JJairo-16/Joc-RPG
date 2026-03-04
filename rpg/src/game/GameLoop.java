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
import utils.ui.Ansi;
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
            "Veure informació");

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
     * Inicia el combat i el manté en execució fins que hi hagi un vencedor (o
     * empat).
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
                System.out.println("VICTÒRIA!");
                System.out.printf("%s ha guanyat el combat!%n", player1.getName());
                System.out.printf("%s ha caigut derrotat.%n", player2.getName());
                break;

            case PLAYER2:
                System.out.println("VICTÒRIA!");
                System.out.printf("%s ha guanyat el combat!%n", player2.getName());
                System.out.printf("%s ha caigut derrotat.%n", player1.getName());
                break;

            case TIE:
                System.out.println("EMPAT!");
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
     * Permet al jugador equipar una arma de l'arsenal (amb filtres) si compleix
     * requisits.
     */
    private void changeWeapon(Character player) {
        Weapon weapon = null;
        boolean loop = true;

        Statistics stats = player.geStatistics();
        boolean equipped = false;

        // Llista d'armes reals
        List<Arsenal> entries = Arsenal.getSortedWeapons();
        WeaponMenu.FilterState filters = new WeaponMenu.FilterState();
        filters.setOnlyEquippable(true);

        do {
            cls.clear();

            Arsenal selected = WeaponMenu.chooseWeaponEntryWithFilters(
                    entries,
                    "Armes de l'arsenal",
                    stats,
                    filters);

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
     * Mostra la informació del jugador: dades bàsiques, raça, estadístiques i arma
     * equipada.
     */
    private void showPlayerInfo(Character player) {
        Statistics stats = player.geStatistics();
        Breed breed = player.getBreed();
        Weapon weapon = player.getWeapon();

        Prettier.printTitle("Informació del jugador");
        System.out.println();

        printPlayerCard(player, breed, stats, weapon);

        // Barres visuals (mantinc la teva API)
        CombatSystem.printStatusBars(player);
        System.out.println();

        if (weapon != null) {
            printWeaponEquippedCard(weapon, stats);
        } else {
            System.out.println(
                    " " + utils.ui.Ansi.DARK_GRAY + "Arma equipada:" + utils.ui.Ansi.RESET + " "
                            + utils.ui.Ansi.BOLD + "Cap" + utils.ui.Ansi.RESET);
            System.out.println(" " + utils.ui.Ansi.DARK_GRAY
                    + "────────────────────────────────────────────────────────" + utils.ui.Ansi.RESET);
        }
    }

    private void printPlayerCard(Character p, Breed breed, Statistics s, Weapon weapon) {
        String name = Ansi.WHITE + Ansi.BOLD + p.getName() + Ansi.RESET;
        String age = Ansi.DARK_GRAY + "Edat: " + Ansi.RESET + Ansi.BOLD + p.getAge() + Ansi.RESET;

        String breedName = Ansi.CYAN + Ansi.BOLD + breed.getName() + Ansi.RESET;
        int bonusPct = (int) Math.round(breed.bonus() * 100.0);
        String bonus = Ansi.GREEN + "Bonus: " + Ansi.RESET
                + Ansi.BOLD + "+" + bonusPct + "%" + Ansi.RESET
                + Ansi.DARK_GRAY + " a " + Ansi.RESET
                + Ansi.BOLD + breed.bonusStat().getName() + Ansi.RESET;

        String weaponTag = (weapon == null)
                ? (Ansi.DARK_GRAY + "(SENSE ARMA)" + Ansi.RESET)
                : (Ansi.GREEN + Ansi.BOLD + "(ARMA EQUIPADA)" + Ansi.RESET);

        // Capçalera
        System.out.printf(" %s  %s  %s%n", name, Ansi.DARK_GRAY + "·" + Ansi.RESET, age);
        System.out.printf(" %s %s%n", breedName, weaponTag);

        // Descripció raça (wrap com al WeaponMenu)
        String desc = breed.getDescription() == null ? "" : breed.getDescription().trim();
        if (!desc.isEmpty()) {
            for (String line : wrap(desc, 78)) {
                System.out.println("   " + Ansi.DARK_GRAY + line + Ansi.RESET);
            }
        }

        System.out.println("   " + bonus);

        // Stats en format compacte (2 línies tipus “barra d’stats”)
        System.out.println();
        System.out.printf("   %s   %s   %s   %s%n",
                statChip("Força", s.getStrength()),
                statChip("Destresa", s.getDexterity()),
                statChip("Constitució", s.getConstitution()),
                statChip("Intel·ligència", s.getIntelligence()));

        System.out.printf("   %s   %s   %s%n",
                statChip("Saviesa", s.getWisdom()),
                statChip("Carisma", s.getCharisma()),
                statChip("Sort", s.getLuck()));

        System.out.println("   " + Ansi.DARK_GRAY
                + "────────────────────────────────────────────────────────" + Ansi.RESET);
    }

    private void printWeaponEquippedCard(Weapon w, Statistics stats) {
        String name = Ansi.WHITE + Ansi.BOLD + w.getName() + Ansi.RESET;
        String type = colorByWeaponType(w.getType()) + "[" + w.getType().getName() + "]" + Ansi.RESET;

        // “Equipable” aquí normalment sempre serà true perquè ja la tens equipada,
        // però ho deixo coherent amb el WeaponMenu.
        String equipTag = (stats != null && w.canEquip(stats))
                ? (" " + Ansi.GREEN + Ansi.BOLD + "(EQUIPABLE)" + Ansi.RESET)
                : (" " + Ansi.DARK_GRAY + "(NO EQUIPABLE)" + Ansi.RESET);

        System.out.printf(" %s %s%s%n", name, type, equipTag);

        String desc = w.getDescription() == null ? "" : w.getDescription().trim();
        if (!desc.isEmpty()) {
            for (String line : wrap(desc, 78)) {
                System.out.println("   " + Ansi.DARK_GRAY + line + Ansi.RESET);
            }
        }

        String dmg = Ansi.GREEN + "Dany: " + Ansi.RESET + Ansi.BOLD + w.getBaseDamage() + Ansi.RESET;
        String crit = Ansi.YELLOW + "Crit: " + Ansi.RESET + Ansi.BOLD
                + String.format("%.0f%%", w.getCriticalProb() * 100.0) + Ansi.RESET;
        String mult = Ansi.YELLOW + "Mult: " + Ansi.RESET + Ansi.BOLD
                + String.format("x%.2f", w.getCriticalDamage()) + Ansi.RESET;

        String mana = (w.getManaPrice() > 0)
                ? (Ansi.BRIGHT_BLUE + "Mana: " + Ansi.RESET + Ansi.BOLD + String.format("%.0f", w.getManaPrice())
                        + Ansi.RESET)
                : (Ansi.DARK_GRAY + "Mana: -" + Ansi.RESET);

        System.out.printf("   %s   %s   %s   %s%n", dmg, crit, mult, mana);

        System.out.println("   " + Ansi.DARK_GRAY
                + "────────────────────────────────────────────────────────" + Ansi.RESET);
    }

    private String statChip(String label, int value) {
        // “chip” curt i consistent
        return Ansi.DARK_GRAY + label + ":" + Ansi.RESET + " " + Ansi.BOLD + value + Ansi.RESET;
    }

    private String colorByWeaponType(models.weapons.WeaponType type) {
        if (type == null)
            return Ansi.WHITE;

        return switch (type) {
            case PHYSICAL -> Ansi.MAGENTA;
            case RANGE -> Ansi.BRIGHT_BLUE;
            case MAGICAL -> Ansi.ORANGE;
            default -> Ansi.WHITE;
        };
    }

    /**
     * Mateix wrap simple que al WeaponMenu per no trencar la UI.
     */
    private static java.util.List<String> wrap(String text, int maxWidth) {
        if (text == null)
            return java.util.List.of();
        text = text.trim();
        if (text.isEmpty())
            return java.util.List.of();
        if (text.length() <= maxWidth)
            return java.util.List.of(text);

        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.isEmpty()) {
                line.append(word);
                continue;
            }

            if (line.length() + 1 + word.length() <= maxWidth) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty())
            lines.add(line.toString());
        return lines;
    }
}