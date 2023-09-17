
package Paint;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class GUI
{

   private static final int MIN_PEN_SIZE = 1;
   private static final int MAX_PEN_SIZE = 1;

   private static final Function<String, Border> TITLED_BORDER =
      title ->
         BorderFactory
            .createTitledBorder
            (
               null,
               title,
               TitledBorder.CENTER,
               TitledBorder.TOP
            )
            ;

   private int numPixelRows = 5;
   private int numPixelColumns = 5;
   private Color currentColor = Color.BLACK;
   private int penSize = 1;

   public GUI()
   {
   
      final JFrame frame = new JFrame();
   
      frame.setTitle("Paint");
      frame.setLocationByPlatform(true);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   
      frame.add(this.createMainPanel());
   
      frame.pack();
      frame.setVisible(true);
   
   }

   private JPanel createMainPanel()
   {
   
      final JPanel mainPanel = new JPanel(new BorderLayout());
   
      mainPanel.add(this.createTopPanel(),      BorderLayout.NORTH);
      mainPanel.add(this.createCenterPanel(),   BorderLayout.CENTER);
   
      return mainPanel;
   
   }

   private JPanel createTopPanel()
   {
   
      final JPanel topPanel = new JPanel();
      topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
   
      topPanel.add(this.createPenSizePanel());
      topPanel.add(this.createColorChooserPanel());
   
      return topPanel;
   
   }

   private JPanel createCenterPanel()
   {
   
      final JPanel panel = new JPanel();
      
      //panel.setLayout(new BoxLayout(panel, BoxLayout.));
      
      return panel;
   
   }

   private JPanel createPenSizePanel()
   {
   
      final JPanel panel = new JPanel();
   
      panel.setBorder(TITLED_BORDER.apply("Size"));
   
      final JComboBox<Integer> penSizeDropDownMenu =
         new
            JComboBox<>
            (
               IntStream
                  .rangeClosed
                  (
                     MIN_PEN_SIZE,
                     MAX_PEN_SIZE
                  )
                  .boxed()
                  .toArray(Integer[]::new)
            );
   
      penSizeDropDownMenu
         .addActionListener
         (
            event ->
            {
            
               this.penSize = penSizeDropDownMenu.getItemAt(penSizeDropDownMenu.getSelectedIndex());
            
            }
         );
   
      panel.add(penSizeDropDownMenu);
   
      return panel;
   
   }

   private JPanel createColorChooserPanel()
   {
   
      final int NUM_ROWS = 3;
      final int NUM_COLUMNS = 3;
      final String DIALOG_TITLE = "Choose a color";
      final java.util.List<Character> KEY_LIST =
         java.util.List.of('U', 'I', 'O', 'H', 'J', 'K', 'B', 'N', 'M');
   
      final java.util.List<Color> COLOR_LIST =
         java.util.List.of(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.PINK, Color.WHITE, Color.BLACK);
   
      final JPanel panel = new JPanel(new GridLayout(NUM_ROWS, NUM_COLUMNS));
   
      panel.setBorder(TITLED_BORDER.apply("Color"));
   
      for (int index = 0; index < NUM_ROWS * NUM_COLUMNS; index++)
      {
      
         final JButton button =
            new JButton("" + KEY_LIST.get(index))
            {
            
               private static final int JUMP = 10;
            
               @Override
               protected void paintComponent(final Graphics g)
               {
               
                  drawCheckerBoard(g);
               
                  g.setColor(this.getBackground());
                  g.fillRect(0, 0, this.getWidth(), this.getHeight());
               
                  super.paintComponent(g);
               
               }
            
               private void drawCheckerBoard(final Graphics g)
               {
               
                  for (int row = 0; row < this.getHeight(); row+=JUMP)
                  {
                  
                     for (int column = 0; column < this.getWidth(); column+=JUMP)
                     {
                     
                        g.setColor((row + column) % (JUMP * 2) == 0 || this.isCenterish(row, column) ? Color.white : Color.BLACK);
                        g.fillRect(column, row, JUMP, JUMP);
                     
                     }
                  
                  }
               
               }
            
               private boolean isCenterish(final int row, final int column)
               {
               
                  final int MAX_ROW = this.getHeight();
                  final int MAX_COLUMN = this.getWidth();
               
                  final boolean ROW_IS_CENTERISH =
                     (MAX_ROW / 2) - (JUMP * 2) <= row && (MAX_ROW / 2) + JUMP >= row;
                  final boolean COLUMN_IS_CENTERISH =
                     (MAX_COLUMN / 2) - (JUMP * 2) <= column && (MAX_COLUMN / 2) + JUMP >= column;
               
                  return ROW_IS_CENTERISH && COLUMN_IS_CENTERISH;
               
               }
            
            };
      
         button.setFont(button.getFont().deriveFont(20.0f));
         button.setOpaque(false);
         button.setBackground(COLOR_LIST.get(index));
      
         ON_CLICK:
         {
         
            button
               .addActionListener
               (
                  event ->
                     button
                        .setBackground
                        (
                           JColorChooser
                              .showDialog
                              (
                                 button,
                                 DIALOG_TITLE,
                                 button.getBackground()
                              )
                        )
               );
         
         }
      
         panel.add(button);
      
      }
   
      return panel;
   
   }

}
