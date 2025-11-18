package mainpack1;

import javax.swing.JFrame;

public class mainclass {
 public static void main(String[] args) {
   JFrame window = new JFrame();
   window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   window.setResizable(false);
   window.setTitle("Ill See You Whenever");

gamepannel gamepannel = new gamepannel();
window.add(gamepannel);
window.pack();

   window.setLocationRelativeTo(null);
   window.setVisible(true);

   gamepannel.startGameThread();
   


 }
}
