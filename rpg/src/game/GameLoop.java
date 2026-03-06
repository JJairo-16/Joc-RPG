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
import models.weapons.WeaponType;
import utils.cache.TextWrapCache;
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

    private static final String HR = " " + Ansi.DARK_GRAY + "────────────────────────────────────────────────────────"
            + Ansi.RESET + "\n";

    private final Character player1;
    private final Character player2;

    private final CombatSystem combatSystem;
    private final Cleaner cls = new Cleaner();
    private final TextWrapCache wrapCache = new TextWrapCache();

    // Cache d'armes (assumim que no canvia durant la partida)
    private final List<Arsenal> entries = Arsenal.getSortedWeapons();

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
        cls.clearAndPrint(() -> generateFFinishText(winner));
    }

    private String generateFFinishText(Winner winner) {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("====================================\n");
        sb.append("           FI DEL COMBAT\n");
        sb.append("====================================\n\n");

        switch (winner) {
            case PLAYER1:
                sb.append("VICTÒRIA!\n");
                sb.append(player1.getName()).append(" ha guanyat el combat!\n");
                sb.append(player2.getName()).append(" ha caigut derrotat.\n");
                break;

            case PLAYER2:
                sb.append("VICTÒRIA!\n");
                sb.append(player2.getName()).append(" ha guanyat el combat!\n");
                sb.append(player1.getName()).append(" ha caigut derrotat.\n");
                break;

            case TIE:
                sb.append("EMPAT!\n");
                sb.append("Tots dos combatents han caigut al mateix temps.\n");
                sb.append("No hi ha vencedor en aquesta batalla.\n");
                break;

            default:
                sb.append("El combat ha finalitzat.\n");
                break;
        }

        sb.append('\n');
        sb.append("====================================\n");

        return sb.toString();
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
                    cls.clearAndPrint(() -> getPlayerInfo(player));
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

    private final StringBuilder playerInfo = new StringBuilder(24_000);

    /**
     * Mostra la informació del jugador: dades bàsiques, raça, estadístiques i arma
     * equipada.
     *
     * ASSUMIM:
     * - Prettier ha estat adaptat a appendTitle(StringBuilder, ...)
     * - CombatSystem ha estat adaptat a appendStatusBars(StringBuilder, ...)
     */
    private String getPlayerInfo(Character player) {
        Statistics stats = player.geStatistics();
        Breed breed = player.getBreed();
        Weapon weapon = player.getWeapon();

        StringBuilder sb = playerInfo;
        sb.setLength(0);
        sb.ensureCapacity(24_000);

        Prettier.appendTitle(sb, "Informació del jugador");
        sb.append('\n');

        appendPlayerCard(sb, player, breed, stats, weapon);

        // Barres visuals (API "append")
        CombatSystem.appendStatusBars(sb, player);
        sb.append('\n');

        if (weapon != null) {
            appendWeaponEquippedCard(sb, weapon);
        } else {
            sb.append(" ").append(Ansi.DARK_GRAY).append("Arma equipada:").append(Ansi.RESET).append(' ')
                    .append(Ansi.BOLD).append("Cap").append(Ansi.RESET).append('\n');
            sb.append(HR);
        }

        return sb.toString();
    }

    private void appendPlayerCard(StringBuilder sb, Character p, Breed breed, Statistics s, Weapon weapon) {
        // Capçalera
        sb.append(' ')
                .append(Ansi.WHITE).append(Ansi.BOLD).append(p.getName()).append(Ansi.RESET)
                .append("  ").append(Ansi.DARK_GRAY).append('·').append(Ansi.RESET).append("  ")
                .append(Ansi.DARK_GRAY).append("Edat: ").append(Ansi.RESET)
                .append(Ansi.BOLD).append(p.getAge()).append(Ansi.RESET)
                .append('\n');

        sb.append(' ')
                .append(Ansi.CYAN).append(Ansi.BOLD).append(breed.getName()).append(Ansi.RESET)
                .append(' ');

        if (weapon == null) {
            sb.append(Ansi.DARK_GRAY).append("(SENSE ARMA)").append(Ansi.RESET);
        } else {
            sb.append(Ansi.GREEN).append(Ansi.BOLD).append("(ARMA EQUIPADA)").append(Ansi.RESET);
        }
        sb.append('\n');

        // Descripció raça (wrap)
        String desc = breed.getDescription() == null ? "" : breed.getDescription().trim();
        if (!desc.isEmpty()) {
            for (String line : wrapCache.get(desc, 78)) {
                sb.append("   ").append(Ansi.DARK_GRAY).append(line).append(Ansi.RESET).append('\n');
            }
        }

        int bonusPct = (int) Math.round(breed.bonus() * 100.0);
        sb.append("   ")
                .append(Ansi.GREEN).append("Bonus: ").append(Ansi.RESET)
                .append(Ansi.BOLD).append('+').append(bonusPct).append('%').append(Ansi.RESET)
                .append(Ansi.DARK_GRAY).append(" a ").append(Ansi.RESET)
                .append(Ansi.BOLD).append(breed.bonusStat().getName()).append(Ansi.RESET)
                .append('\n');

        // Stats en format compacte (2 línies tipus “barra d’stats”)
        sb.append('\n');
        sb.append("   ")
                .append(statChip("Força", s.getStrength())).append("   ")
                .append(statChip("Destresa", s.getDexterity())).append("   ")
                .append(statChip("Constitució", s.getConstitution())).append("   ")
                .append(statChip("Intel·ligència", s.getIntelligence()))
                .append('\n');

        sb.append("   ")
                .append(statChip("Saviesa", s.getWisdom())).append("   ")
                .append(statChip("Carisma", s.getCharisma())).append("   ")
                .append(statChip("Sort", s.getLuck()))
                .append('\n');

        sb.append("   ").append(Ansi.DARK_GRAY)
                .append("────────────────────────────────────────────────────────")
                .append(Ansi.RESET).append('\n');
    }

    private void appendWeaponEquippedCard(StringBuilder sb, Weapon w) {
        WeaponType wt = w.getType();
        String typeName = (wt == null) ? "?" : wt.getName();

        sb.append(' ')
                .append(Ansi.WHITE).append(Ansi.BOLD).append(w.getName()).append(Ansi.RESET)
                .append(' ')
                .append(colorByWeaponType(wt)).append('[').append(typeName).append(']').append(Ansi.RESET)
                .append('\n');

        String desc = w.getDescription() == null ? "" : w.getDescription().trim();
        if (!desc.isEmpty()) {
            for (String line : wrapCache.get(desc, 78)) {
                sb.append("   ").append(Ansi.DARK_GRAY).append(line).append(Ansi.RESET).append('\n');
            }
        }

        sb.append("   ")
                .append(Ansi.GREEN).append("Dany: ").append(Ansi.RESET).append(Ansi.BOLD)
                .append(w.getBaseDamage()).append(Ansi.RESET)
                .append("   ")
                .append(Ansi.YELLOW).append("Crit: ").append(Ansi.RESET).append(Ansi.BOLD)
                .append(roundPer(w.getCriticalProb())).append('%').append(Ansi.RESET)
                .append("   ")
                .append(Ansi.YELLOW).append("Mult: ").append(Ansi.RESET).append(Ansi.BOLD)
                .append('x').append(round2(w.getCriticalDamage())).append(Ansi.RESET)
                .append("   ");

        if (w.getManaPrice() > 0) {
            sb.append(Ansi.BRIGHT_BLUE).append("Mana: ").append(Ansi.RESET).append(Ansi.BOLD)
                    .append(Math.round(w.getManaPrice())).append(Ansi.RESET);
        } else {
            sb.append(Ansi.DARK_GRAY).append("Mana: -").append(Ansi.RESET);
        }

        sb.append('\n');
        sb.append(HR);
    }

    private String statChip(String label, int value) {
        // “chip” curt i consistent
        return Ansi.DARK_GRAY + label + ":" + Ansi.RESET + " " + Ansi.BOLD + value + Ansi.RESET;
    }

    private String colorByWeaponType(WeaponType type) {
        if (type == null)
            return Ansi.WHITE;

        return switch (type) {
            case PHYSICAL -> Ansi.MAGENTA;
            case RANGE -> Ansi.BRIGHT_BLUE;
            case MAGICAL -> Ansi.ORANGE;
            default -> Ansi.WHITE;
        };
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static int roundPer(double n) {
        return (int) Math.round(n * 100.0);
    }
}