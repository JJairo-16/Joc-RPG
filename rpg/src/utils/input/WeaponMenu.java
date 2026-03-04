package utils.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import models.characters.Statistics;
import models.weapons.Arsenal;
import models.weapons.WeaponType;
import utils.cache.TextWrapCache;
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
    private static final TextWrapCache WRAP_CACHE = new TextWrapCache();

    // Separador reutilitzable per evitar concatenacions repetides
    private static final String CARD_SEPARATOR =
            "   " + Ansi.DARK_GRAY + "────────────────────────────────────────────────────────" + Ansi.RESET + "\n";

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

        // Caché local per construir la UI amb un sol print per repintat
        final StringBuilder sb = new StringBuilder(32_768);

        while (true) {
            cls.clear();

            sb.setLength(0);
            appendTitle(sb, title);
            appendWeapons(sb, weapons);
            sb.append('\n');
            sb.append("Seleccioni una arma, si us plau: ");

            // Un sol write a consola per la UI
            System.out.print(sb.toString());

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

        boolean dirty = true;
        List<FilteredItem> filtered = List.of();

        final StringBuilder sb = new StringBuilder(64_000);

        while (true) {
            cls.clear();

            if (dirty) {
                filtered = buildFilteredItems(weapons, stats, onlyEquippable, typeFilter);
                dirty = false;
            }

            sb.setLength(0);
            appendTitle(sb, title);
            appendFilterBar(sb, onlyEquippable, typeFilter);
            sb.append('\n');
            appendWeaponsFiltered(sb, weapons, filtered, stats);
            sb.append('\n');
            sb.append("Opció (número), [F] equipables, [T] tipus: ");

            System.out.print(sb.toString());

            String in = getInput();
            if (in == null) {
                Menu.pause();
                continue;
            }

            if (in.equalsIgnoreCase("f")) {
                onlyEquippable = !onlyEquippable;
                dirty = true;
                continue;
            }
            if (in.equalsIgnoreCase("t")) {
                typeFilter = typeFilter.next();
                dirty = true;
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
            if (idxInFiltered < 0 || idxInFiltered >= filtered.size()) {
                Prettier.warn("L'opció introduïda ha d'estar entre 1 i %d.", filtered.size() + 1);
                Menu.pause();
                continue;
            }

            return filtered.get(idxInFiltered).index;
        }
    }

    /**
     * Variant còmoda del menú amb filtres: retorna directament l'{@link Arsenal} (o
     * {@code null} si es cancel·la).
     */
    public static Arsenal chooseWeaponEntryWithFilters(List<Arsenal> weapons, String title, Statistics stats) {
        int idx = chooseWeaponWithFilters(weapons, title, stats);
        if (weapons == null) return null;
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

        boolean dirty = true;
        List<FilteredItem> filtered = List.of();

        final StringBuilder sb = new StringBuilder(64_000);

        while (true) {
            cls.clear();

            if (dirty) {
                filtered = buildFilteredItems(weapons, stats, onlyEquippable, typeFilter);
                dirty = false;
            }

            sb.setLength(0);
            appendTitle(sb, title);
            appendFilterBar(sb, onlyEquippable, typeFilter);
            sb.append('\n');
            appendWeaponsFiltered(sb, weapons, filtered, stats);
            sb.append('\n');
            sb.append("Opció (número), [F] equipables, [T] tipus: ");

            System.out.print(sb.toString());

            String in = getInput();
            if (in == null) {
                Menu.pause();
                continue;
            }

            if (in.equalsIgnoreCase("f")) {
                onlyEquippable = !onlyEquippable;
                state.setOnlyEquippable(onlyEquippable);
                dirty = true;
                continue;
            }
            if (in.equalsIgnoreCase("t")) {
                typeFilter = typeFilter.next();
                state.setTypeFilter(typeFilter);
                dirty = true;
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
            if (idxInFiltered < 0 || idxInFiltered >= filtered.size()) {
                Prettier.warn("L'opció introduïda ha d'estar entre 1 i %d.", filtered.size() + 1);
                Menu.pause();
                continue;
            }

            state.setOnlyEquippable(onlyEquippable);
            state.setTypeFilter(typeFilter);

            return filtered.get(idxInFiltered).index;
        }
    }

    public static Arsenal chooseWeaponEntryWithFilters(
            List<Arsenal> weapons, String title, Statistics stats, FilterState state) {
        int idx = chooseWeaponWithFilters(weapons, title, stats, state);
        return (idx < 0) ? null : weapons.get(idx);
    }

    /**
     * Element filtrat: guarda l'índex original i si és equipable (per evitar cridar
     * canEquip(stats) més d'un cop per repintat).
     */
    private static final class FilteredItem {
        final int index; // índex dins la llista original
        final boolean equippable; // resultat de canEquip(stats) si stats != null

        FilteredItem(int index, boolean equippable) {
            this.index = index;
            this.equippable = equippable;
        }
    }

    /**
     * Construeix la llista d'elements filtrats (índexos originals) i, si cal,
     * calcula també l'etiqueta d'equipable una sola vegada.
     */
    private static List<FilteredItem> buildFilteredItems(
            List<Arsenal> weapons,
            Statistics stats,
            boolean onlyEquippable,
            TypeFilter typeFilter) {

        List<FilteredItem> out = new ArrayList<>();

        for (int i = 0; i < weapons.size(); i++) {
            Arsenal w = weapons.get(i);
            WeaponType wt = w.getType();

            if (!typeFilter.matches(wt)) {
                continue;
            }

            boolean equippable = false;
            if (stats != null && wt != null) {
                equippable = wt.canEquip(stats);
            }

            if (onlyEquippable && !equippable) {
                continue;
            }

            out.add(new FilteredItem(i, equippable));
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

    private static void appendTitle(StringBuilder sb, String title) {
        // No podem garantir que Prettier tingui versió "toString", així que fem un títol senzill.
        // Si vols exactament el mateix estil, pots afegir un Prettier.appendTitle(sb, title).
        sb.append(Ansi.BOLD).append(title).append(Ansi.RESET).append("\n\n");
    }

    private static void appendFilterBar(StringBuilder sb, boolean onlyEquippable, TypeFilter typeFilter) {
        String eq = onlyEquippable ? (Ansi.GREEN + "ON" + Ansi.RESET) : (Ansi.DARK_GRAY + "OFF" + Ansi.RESET);
        String type = Ansi.BOLD + typeFilter.getLabel() + Ansi.RESET;

        sb.append(Ansi.DARK_GRAY)
          .append("Filtres:")
          .append(Ansi.RESET)
          .append("  [F] Equipables: ")
          .append(eq)
          .append("   [T] Tipus: ")
          .append(type)
          .append('\n');
    }

    private static void appendWeapons(StringBuilder sb, List<Arsenal> weapons) {
        sb.append(Ansi.DARK_GRAY).append(Ansi.BOLD).append("1.").append(Ansi.RESET)
          .append(' ')
          .append(Ansi.DARK_GRAY).append("Cancelar").append(Ansi.RESET)
          .append('\n');

        int num = 2;
        for (int i = 0; i < weapons.size(); i++) {
            Arsenal w = weapons.get(i);
            appendWeaponCard(sb, num++, w, null, false);
        }
    }

    private static void appendWeaponsFiltered(
            StringBuilder sb,
            List<Arsenal> weapons,
            List<FilteredItem> filtered,
            Statistics stats) {

        sb.append(Ansi.DARK_GRAY).append(Ansi.BOLD).append("1.").append(Ansi.RESET)
          .append(' ')
          .append(Ansi.DARK_GRAY).append("Cancelar").append(Ansi.RESET)
          .append('\n');

        if (filtered.isEmpty()) {
            sb.append(Ansi.DARK_GRAY)
              .append("  (No hi ha armes amb aquests filtres. Prem 'f' o 't' per canviar.)")
              .append(Ansi.RESET)
              .append('\n');
            return;
        }

        int num = 2;
        for (FilteredItem it : filtered) {
            Arsenal w = weapons.get(it.index);
            appendWeaponCard(sb, num++, w, stats, it.equippable);
        }
    }

    private static void appendWeaponCard(StringBuilder sb, int optionNumber, Arsenal w, Statistics stats, boolean equippable) {
        String num = Ansi.CYAN + Ansi.BOLD + optionNumber + "." + Ansi.RESET;
        String name = Ansi.WHITE + Ansi.BOLD + w.getName() + Ansi.RESET;

        WeaponType wt = w.getType();
        String typeName = (wt == null) ? "?" : wt.getName();
        String type = colorByType(wt) + "[" + typeName + "]" + Ansi.RESET;

        String equipTag = "";
        if (stats != null) {
            equipTag = equippable
                    ? (" " + Ansi.GREEN + Ansi.BOLD + "(EQUIPABLE)" + Ansi.RESET)
                    : (" " + Ansi.DARK_GRAY + "(NO EQUIPABLE)" + Ansi.RESET);
        }

        sb.append(num).append(' ').append(name).append(' ').append(type).append(equipTag).append('\n');

        String desc = w.getDescription();
        if (desc != null) {
            desc = desc.trim();
            if (!desc.isEmpty()) {
                for (String line : WRAP_CACHE.get(desc, 78)) {
                    sb.append("   ").append(Ansi.DARK_GRAY).append(line).append(Ansi.RESET).append('\n');
                }
            }
        }

        sb.append("   ")
          .append(Ansi.GREEN).append("Dany: ").append(Ansi.RESET).append(Ansi.BOLD).append(w.getBaseDamage()).append(Ansi.RESET)
          .append("   ")
          .append(Ansi.YELLOW).append("Crit: ").append(Ansi.RESET).append(Ansi.BOLD)
          .append(roundPer(w.getCriticalProb())).append("%").append(Ansi.RESET)
          .append("   ")
          .append(Ansi.YELLOW).append("Mult: ").append(Ansi.RESET).append(Ansi.BOLD).append("x")
          .append(round2(w.getCriticalDamage())).append(Ansi.RESET)
          .append("   ");

        if (w.getManaPrice() > 0) {
            sb.append(Ansi.BRIGHT_BLUE).append("Mana: ").append(Ansi.RESET).append(Ansi.BOLD)
              .append(Math.round(w.getManaPrice()))
              .append(Ansi.RESET);
        } else {
            sb.append(Ansi.DARK_GRAY).append("Mana: -").append(Ansi.RESET);
        }
        sb.append('\n');

        sb.append(CARD_SEPARATOR);
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
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

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static int roundPer(double n) {
        return (int) Math.round(n * 100.0);
    }
}