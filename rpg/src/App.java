import creator.CharacterCreator;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        CharacterCreator.createNewCharacter();
    }
}