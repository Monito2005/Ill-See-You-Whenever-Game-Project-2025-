package mainpack1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public GamePanel gp;
    public boolean upPressed, downPressed, leftPressed, rightPressed, oPressed, shiftPressed;

    @Override public void keyTyped(KeyEvent e){}

    @Override
    public void keyPressed(KeyEvent e){
        if(gp == null) return;
        int c = e.getKeyCode();

        if(gp.gameState == GamePanel.GameState.INTRO){
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER || c==KeyEvent.VK_ESCAPE){
                gp.intro.skip(); // was skipToEnd()
            }
            return;
        } else if(gp.gameState == GamePanel.GameState.MENU){
            if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP) gp.mainMenu.move(-1);
            else if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN) gp.mainMenu.move(1);
            else if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER) gp.mainMenu.confirm();
            return;
        } else if(gp.gameState == GamePanel.GameState.CONVO){
            gp.conversationSystem.handleInput(e);
            return;
        } else if(gp.gameState == GamePanel.GameState.PLAY){
            if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP)    upPressed = true;
            if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN)  downPressed = true;
            if(c==KeyEvent.VK_A || c==KeyEvent.VK_LEFT)  leftPressed = true;
            if(c==KeyEvent.VK_D || c==KeyEvent.VK_RIGHT) rightPressed = true;
            if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER) oPressed = true;
            if(c==KeyEvent.VK_SHIFT) shiftPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e){
        int c = e.getKeyCode();
        if(c==KeyEvent.VK_W || c==KeyEvent.VK_UP)    upPressed = false;
        if(c==KeyEvent.VK_S || c==KeyEvent.VK_DOWN)  downPressed = false;
        if(c==KeyEvent.VK_A || c==KeyEvent.VK_LEFT)  leftPressed = false;
        if(c==KeyEvent.VK_D || c==KeyEvent.VK_RIGHT) rightPressed = false;
        if(c==KeyEvent.VK_O || c==KeyEvent.VK_ENTER) oPressed = false;
        if(c==KeyEvent.VK_SHIFT) shiftPressed = false;
    }
}
