import creator.CharacterCreator;
import models.characters.Character;

import game.GameLoop;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        Character p1 = CharacterCreator.createDebugCharacter();
        Character p2 = CharacterCreator.createDebugCharacter();
        GameLoop game = new GameLoop(p1, p2);
        game.init();
    }
}