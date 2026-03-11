import creator.CharacterCreator;
import models.characters.Character;
import utils.input.WeaponMenu;
import utils.ui.LoadingDots;
import game.GameLoop;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        try (LoadingDots ld = new LoadingDots()) {
            WeaponMenu.preloadCards();
        }

        Character p1 = CharacterCreator.createNewCharacter();
        Character p2 = CharacterCreator.createNewCharacter();
        GameLoop game = new GameLoop(p1, p2);
        game.init();
    }
}