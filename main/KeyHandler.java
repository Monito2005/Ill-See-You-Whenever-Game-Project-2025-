import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener{

    GamePannel gp;

    public KeyHandler(GamePannel gp){
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Arrow keys
        if(code == KeyEvent.VK_UP) gp.up = true;
        if(code == KeyEvent.VK_DOWN) gp.down = true;
        if(code == KeyEvent.VK_LEFT) gp.left = true;
        if(code == KeyEvent.VK_RIGHT) gp.right = true;

        // WASD keys
        if(code == KeyEvent.VK_W) gp.up = true;
        if(code == KeyEvent.VK_S) gp.down = true;
        if(code == KeyEvent.VK_A) gp.left = true;
        if(code == KeyEvent.VK_D) gp.right = true;

        // Fullscreen toggle (F11)
        if(code == KeyEvent.VK_F11) gp.toggleFullscreen();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        // Arrow keys
        if(code == KeyEvent.VK_UP) gp.up = false;
        if(code == KeyEvent.VK_DOWN) gp.down = false;
        if(code == KeyEvent.VK_LEFT) gp.left = false;
        if(code == KeyEvent.VK_RIGHT) gp.right = false;

        // WASD keys
        if(code == KeyEvent.VK_W) gp.up = false;
        if(code == KeyEvent.VK_S) gp.down = false;
        if(code == KeyEvent.VK_A) gp.left = false;
        if(code == KeyEvent.VK_D) gp.right = false;
    }
    
}
