
package Paint;

import javax.swing.*;

public class GUI
{

   private final JFrame frame;

   public GUI()
   {
   
      this.frame = new JFrame();
   
      this.frame.add(this.createBottomPanel());
   
   }

   private final JPanel createBottomPanel()
   {
   
      final JButton save = new JButton();
   
      save
         .addActionListener
         (
            actionEvent ->
            {
            
               this.toString();
            
            }
            
         )
         ;
   
      return null;
   
   }

}
