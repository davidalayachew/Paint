
package Paint;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.*;

public class Main
{

   private static final int SIZE = 1000;

   public static void main(String[] args) throws Exception
   {
   
      System.out.println("Paint");
   
      javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new DarkMetalHighContrast());
      UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
      // UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
      // UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
   
      javax.swing.SwingUtilities.invokeLater(() -> new GUI());
      // javax.swing.SwingUtilities.invokeLater(() -> new GUI(5_000, 5_000));
      //javax.swing.SwingUtilities.invokeLater(() -> new GUI(20, 20));
   
   }

}
