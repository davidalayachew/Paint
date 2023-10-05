
package Paint;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main
{

   public static void main(String[] args)
   {
   
      System.out.println("Paint");
   
      javax.swing.SwingUtilities.invokeLater(() -> new GUI(10_000, 10_000));
      //javax.swing.SwingUtilities.invokeLater(() -> new GUI(20, 20));
   
   }

}
