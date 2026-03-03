package utils.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import models.characters.Statistics;
import models.weapons.Arsenal;
import models.weapons.WeaponType;
import utils.ui.Ansi;
import utils.ui.Cleaner;
import utils.ui.Prettier;

/**
 * Menú especialitzat per mostrar i seleccionar armes amb una aparença "card
 * style".
 *
 * <p>
 * Retorna un índex 0-based sobre la llista d'armes (o -1 si es cancel·la).
 * </p>
 */
public final class WeaponMenu {

    private static final Cleaner cls = new Cleaner();
    private static final Scanner scanner = new Scanner(System.in);

    private WeaponMenu() {
    }

    /**
     * Mostra un menú d'armes amb una opció inicial de "Cancelar".
     *
     * IMPORTANT:
     * Aquesta funció assumeix que la llista ja està en l'ordre de visualització.
     * Recomanat: passar {@link Arsenal#getSortedWeapons()}.
     *
     * @param weapons llista d'armes (Arsenal) a mostrar
     * @param title   títol del menú
     * @return índex 0-based de la llista (0..n-1) o -1 si "Cancelar"
     */
    public static int chooseWeapon(List<Arsenal> weapons, String title) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        while (true) {
            cls.clear();
            Prettier.printTitle(title);

            printWeapons(weapons);
            System.out.println();

            System.out.print("Seleccioni una arma, si us plau: ");
            int option = getInteger();
            if (option == -1) {
                Menu.pause();
                continue;
            }

            // 1 = Cancelar
            if (option == 1) {
                return -1;
            }

            int idx = option - 2; // 2.. => 0..
            if (idx >= 0 && idx < weapons.size()) {
                return idx;
            }

            Prettier.warn("L'opció introduïda ha de estar entre 1 i %d. Si us plau, torni a intentar-ho.",
                    weapons.size() + 1);
            Menu.pause();
        }
    }

    /**
     * Variant còmoda: retorna directament l'{@link Arsenal} escollit (o {@code null}
     * si es cancel·la).
     *
     * IMPORTANT:
     * Aquesta funció assumeix que la llista ja està en l'ordre de visualització.
     * Recomanat: passar {@link Arsenal#getSortedWeapons()}.
     */
    public static Arsenal chooseWeaponEntry(List<Arsenal> weapons, String title) {
        if (weapons == null || weapons.isEmpty()) {
            throw new IllegalArgumentException();
        }

        int idx = chooseWeapon(weapons, title);
        return (idx < 0) ? null : weapons.get(idx);
    }

    /**
     * Menú d'armes amb filtres:
     * <ul>
     * <li>[F] mostrar només equipables (ON/OFF)</li>
     * <li>[T] filtrar per tipus (Tots, Rang, Físic, Màgic)</li>
     * </ul>
     *
     * Entrada:
     * <ul>
     * <li>número: seleccionar</li>
     * <li>f: alternar equipables</li>
     * <li>t: canviar tipus</li>
     * </ul>
     *
     * @return índex 0-based sobre la llista ORIGINAL (weapons), o -1 si Cancelar.
     */
    public static int chooseWeaponWithFilters(List<Arsenal> weapons, String title, Statistics stats) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        boolean onlyEquippable = false;
        TypeFilter typeFilter = TypeFilter.ALL;

        while (true) {
            cls.clear();
            Prettier.printTitle(title);

            printFilterBar(onlyEquippable, typeFilter);
            System.out.println();

            List<Integer> filteredIdx = buildFilteredIndexes(weapons, stats, onlyEquippable, typeFilter);

            printWeaponsFiltered(weapons, filteredIdx, stats);
            System.out.println();

            System.out.print("Opció (número), [F] equipables, [T] tipus: ");
            String in = getInput();
            if (in == null) {
                Menu.pause();
                continue;
            }

            if (in.equalsIgnoreCase("f")) {
                onlyEquippable = !onlyEquippable;
                continue;
            }
            if (in.equalsIgnoreCase("t")) {
                typeFilter = typeFilter.next();
                continue;
            }

            Integer opt = tryParseInt(in);
            if (opt == null) {
                Prettier.warn("Introdueix un número, o bé 'f' / 't'.");
                Menu.pause();
                continue;
            }

            // 1 = Cancelar
            if (opt == 1) {
                return -1;
            }

            int idxInFiltered = opt - 2;
            if (idxInFiltered < 0 || idxInFiltered >= filteredIdx.size()) {
                Prettier.warn("L'opció introduïda ha d'estar entre 1 i %d.", filteredIdx.size() + 1);
                Menu.pause();
                continue;
            }

            return filteredIdx.get(idxInFiltered);
        }
    }

    /**
     * Variant còmoda del menú amb filtres: retorna directament l'{@link Arsenal} (o
     * {@code null} si es cancel·la).
     */
    public static Arsenal chooseWeaponEntryWithFilters(List<Arsenal> weapons, String title, Statistics stats) {
        int idx = chooseWeaponWithFilters(weapons, title, stats);
        return (idx < 0) ? null : weapons.get(idx);
    }

    public static int chooseWeaponWithFilters(List<Arsenal> weapons, String title, Statistics stats, FilterState state) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        // Si no et passen estat, en creem un de local (sense “memòria”)
        if (state == null) {
            state = new FilterState();
        }

        boolean onlyEquippable = state.isOnlyEquippable();
        TypeFilter typeFilter = state.getTypeFilter();

        while (true) {
            cls.clear();
            Prettier.printTitle(title);

            printFilterBar(onlyEquippable, typeFilter);
            System.out.println();

            List<Integer> filteredIdx = buildFilteredIndexes(weapons, stats, onlyEquippable, typeFilter);

            printWeaponsFiltered(weapons, filteredIdx, stats);
            System.out.println();

            System.out.print("Opció (número), [F] equipables, [T] tipus: ");
            String in = getInput();
            if (in == null) {
                Menu.pause();
                continue;
            }

            if (in.equalsIgnoreCase("f")) {
                onlyEquippable = !onlyEquippable;
                state.setOnlyEquippable(onlyEquippable);
                continue;
            }
            if (in.equalsIgnoreCase("t")) {
                typeFilter = typeFilter.next();
                state.setTypeFilter(typeFilter);
                continue;
            }

            Integer opt = tryParseInt(in);
            if (opt == null) {
                Prettier.warn("Introdueix un número, o bé 'f' / 't'.");
                Menu.pause();
                continue;
            }

            if (opt == 1) {
                state.setOnlyEquippable(onlyEquippable);
                state.setTypeFilter(typeFilter);
                return -1;
            }

            int idxInFiltered = opt - 2;
            if (idxInFiltered < 0 || idxInFiltered >= filteredIdx.size()) {
                Prettier.warn("L'opció introduïda ha d'estar entre 1 i %d.", filteredIdx.size() + 1);
                Menu.pause();
                continue;
            }

            state.setOnlyEquippable(onlyEquippable);
            state.setTypeFilter(typeFilter);

            return filteredIdx.get(idxInFiltered);
        }
    }

    public static Arsenal chooseWeaponEntryWithFilters(
            List<Arsenal> weapons, String title, Statistics stats, FilterState state) {
        int idx = chooseWeaponWithFilters(weapons, title, stats, state);
        return (idx < 0) ? null : weapons.get(idx);
    }

    private static List<Integer> buildFilteredIndexes(
            List<Arsenal> weapons,
            Statistics stats,
            boolean onlyEquippable,
            TypeFilter typeFilter) {

        List<Integer> out = new ArrayList<>();

        for (int i = 0; i < weapons.size(); i++) {
            Arsenal w = weapons.get(i);

            if (!typeFilter.matches(w.getType())) {
                continue;
            }

            if (onlyEquippable) {
                if (stats == null) {
                    continue;
                }
                if (!w.getType().canEquip(stats)) {
                    continue;
                }
            }

            out.add(i);
        }

        return out;
    }

    private static String colorByType(WeaponType type) {
        if (type == null) {
            return Ansi.WHITE;
        }

        return switch (type) {
            case PHYSICAL -> Ansi.MAGENTA;
            case RANGE -> Ansi.BRIGHT_BLUE;
            case MAGICAL -> Ansi.ORANGE;
            default -> Ansi.WHITE;
        };
    }

    private static int getInteger() {
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            Prettier.warn("L'opció introduïda no pot estar en blanc. Si us plau, torni a intentar-ho.");
            return -1;
        }

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Prettier.warn("L'opció introduïda ha de ser un nombre enter positiu. Si us plau, torni a intentar-ho.");
        } catch (Exception e) {
            Prettier.warn("Ha hagut un error. Si us plau, torni a intentar-ho.");
        }

        return -1;
    }

    private static String getInput() {
        try {
            String s = scanner.nextLine();
            if (s == null) {
                return null;
            }

            s = s.trim();
            if (s.isEmpty()) {
                Prettier.warn("L'entrada no pot estar en blanc.");
                return null;
            }

            return s;
        } catch (Exception e) {
            Prettier.warn("Ha hagut un error. Si us plau, torni a intentar-ho.");
            return null;
        }
    }

    private static void printFilterBar(boolean onlyEquippable, TypeFilter typeFilter) {
        String eq = onlyEquippable ? (Ansi.GREEN + "ON" + Ansi.RESET) : (Ansi.DARK_GRAY + "OFF" + Ansi.RESET);
        String type = Ansi.BOLD + typeFilter.getLabel() + Ansi.RESET;

        System.out.printf("%sFiltres:%s  [F] Equipables: %s   [T] Tipus: %s%n",
                Ansi.DARK_GRAY, Ansi.RESET, eq, type);
    }

    private static void printWeapons(List<Arsenal> weapons) {
        System.out.printf("%s%s1.%s %sCancelar%s%n",
                Ansi.DARK_GRAY, Ansi.BOLD, Ansi.RESET,
                Ansi.DARK_GRAY, Ansi.RESET);

        int num = 2;
        for (Arsenal w : weapons) {
            printWeaponCard(num++, w, null);
        }
    }

    private static void printWeaponsFiltered(List<Arsenal> weapons, List<Integer> filteredIdx, Statistics stats) {
        System.out.printf("%s%s1.%s %sCancelar%s%n",
                Ansi.DARK_GRAY, Ansi.BOLD, Ansi.RESET,
                Ansi.DARK_GRAY, Ansi.RESET);

        if (filteredIdx.isEmpty()) {
            System.out.println(Ansi.DARK_GRAY
                    + "  (No hi ha armes amb aquests filtres. Prem 'f' o 't' per canviar.)"
                    + Ansi.RESET);
            return;
        }

        int num = 2;
        for (int originalIndex : filteredIdx) {
            Arsenal w = weapons.get(originalIndex);
            printWeaponCard(num++, w, stats);
        }
    }

    private static void printWeaponCard(int optionNumber, Arsenal w, Statistics stats) {
        String num = Ansi.CYAN + Ansi.BOLD + optionNumber + "." + Ansi.RESET;
        String name = Ansi.WHITE + Ansi.BOLD + w.getName() + Ansi.RESET;
        String type = colorByType(w.getType()) + "[" + w.getType().getName() + "]" + Ansi.RESET;

        String equipTag = "";
        if (stats != null) {
            boolean can = w.getType().canEquip(stats);
            equipTag = can
                    ? (" " + Ansi.GREEN + Ansi.BOLD + "(EQUIPABLE)" + Ansi.RESET)
                    : (" " + Ansi.DARK_GRAY + "(NO EQUIPABLE)" + Ansi.RESET);
        }

        System.out.printf("%s %s %s%s%n", num, name, type, equipTag);

        String desc = w.getDescription() == null ? "" : w.getDescription().trim();
        if (!desc.isEmpty()) {
            for (String line : wrap(desc, 78)) {
                System.out.println("   " + Ansi.DARK_GRAY + line + Ansi.RESET);
            }
        }

        String dmg = Ansi.GREEN + "Dany: " + Ansi.RESET + Ansi.BOLD + w.getBaseDamage() + Ansi.RESET;
        String crit = Ansi.YELLOW + "Crit: " + Ansi.RESET + Ansi.BOLD
                + String.format("%.0f%%", w.getCriticalProb() * 100.0) + Ansi.RESET;
        String mult = Ansi.YELLOW + "Mult: " + Ansi.RESET + Ansi.BOLD + String.format("x%.2f", w.getCriticalDamage())
                + Ansi.RESET;

        String mana;
        if (w.getManaPrice() > 0) {
            mana = Ansi.BRIGHT_BLUE + "Mana: " + Ansi.RESET + Ansi.BOLD + String.format("%.0f", w.getManaPrice())
                    + Ansi.RESET;
        } else {
            mana = Ansi.DARK_GRAY + "Mana: -" + Ansi.RESET;
        }

        System.out.printf("   %s   %s   %s   %s%n", dmg, crit, mult, mana);

        System.out.println(
                "   " + Ansi.DARK_GRAY + "────────────────────────────────────────────────────────" + Ansi.RESET);
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Wrap simple per no trencar la UI en terminals estrets.
     */
    private static List<String> wrap(String text, int maxWidth) {
        if (text.length() <= maxWidth) {
            return List.of(text);
        }

        ArrayList<String> lines = new ArrayList<>();
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

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    private enum TypeFilter {
        ALL("Tots"),
        RANGE("Rang"),
        PHYSICAL("Físic"),
        MAGICAL("Màgic");

        private final String label;

        TypeFilter(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean matches(WeaponType t) {
            if (this == ALL) {
                return true;
            }
            if (t == null) {
                return false;
            }

            return switch (this) {
                case RANGE -> t == WeaponType.RANGE;
                case PHYSICAL -> t == WeaponType.PHYSICAL;
                case MAGICAL -> t == WeaponType.MAGICAL;
                default -> true;
            };
        }

        public TypeFilter next() {
            return switch (this) {
                case ALL -> RANGE;
                case RANGE -> PHYSICAL;
                case PHYSICAL -> MAGICAL;
                case MAGICAL -> ALL;
            };
        }
    }

    public static final class FilterState {

        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;

        public FilterState() {
        }

        public FilterState(boolean onlyEquippable, TypeFilter typeFilter) {
            this.onlyEquippable = onlyEquippable;
            this.typeFilter = (typeFilter == null) ? TypeFilter.ALL : typeFilter;
        }

        public boolean isOnlyEquippable() {
            return onlyEquippable;
        }

        public void setOnlyEquippable(boolean onlyEquippable) {
            this.onlyEquippable = onlyEquippable;
        }

        public TypeFilter getTypeFilter() {
            return typeFilter;
        }

        public void setTypeFilter(TypeFilter typeFilter) {
            this.typeFilter = (typeFilter == null) ? TypeFilter.ALL : typeFilter;
        }
    }
}