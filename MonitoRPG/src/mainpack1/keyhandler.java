package mainpack1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class keyhandler implements  KeyListener{

public boolean upPressed, downPressed, leftPressed, rightPressed;

    
    @Override
    public void keyTyped(KeyEvent e) {
        // not used but required by the interface
    }

    @Override
    public void keyPressed(KeyEvent e) {
        
        int code = e.getKeyCode();

        if(code== KeyEvent.VK_W){
        upPressed = true;
        }
    if(code== KeyEvent.VK_S){
        downPressed = true;
        }
    if(code== KeyEvent.VK_A){
         leftPressed = true;
        }
    if(code== KeyEvent.VK_D){
         rightPressed = true;
        }


        // handled above by setting the appropriate flags
    }

    @Override
    public void keyReleased(KeyEvent e) {
     
    int code = e.getKeyCode();

 if(code== KeyEvent.VK_W){
        upPressed = false;
        }
    if(code== KeyEvent.VK_S){
        downPressed = false;
        }
    if(code== KeyEvent.VK_A){
         leftPressed = false;
        }
    if(code== KeyEvent.VK_D){
         rightPressed = false;
        }

        // handled above by clearing the appropriate flags
    }

}
