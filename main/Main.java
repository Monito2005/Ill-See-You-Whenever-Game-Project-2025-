import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Ill See you Whenever");

        GamePannel gamepannel = new GamePannel();
        window.add(gamepannel);

        window.pack();
        
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gamepannel.startGameThread();
        
    }
    
}
