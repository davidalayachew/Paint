
package Paint;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.*;

public class Main
{

   private static final int SIZE = 1000;

   public static void main(String[] args)
   {
   
      System.out.println("Paint");
   
      javax.swing.SwingUtilities.invokeLater(() -> new GUI(10_000, 10_000));
      //javax.swing.SwingUtilities.invokeLater(() -> new GUI(20, 20));
   
   }

}
