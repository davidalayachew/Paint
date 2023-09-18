
package Paint;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class GUI
{

   private static final int MIN_PEN_SIZE = 1;
   private static final int MAX_PEN_SIZE = 1;

   private static final Dimension CELL_DIMENSIONS  = new Dimension(10, 10);
   private static final KeyStroke UP               = KeyStroke.getKeyStroke(KeyEvent.VK_UP,     0, true);
   private static final KeyStroke DOWN             = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   0, true);
   private static final KeyStroke LEFT             = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   0, true);
   private static final KeyStroke RIGHT            = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  0, true);
   private static final KeyStroke SPACE_PRESS      = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,  0, false);
   private static final KeyStroke SPACE_RELEASE    = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,  0, true);
   private static final Border ORIGINAL_BORDER     = new JButton().getBorder();

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

   private final List<JComponent> cells = new ArrayList<>();

   private int numPixelRows = 5;
   private int numPixelColumns = 5;
   private Color currentColor = Color.BLACK;
   private int penSize = 1;
   private boolean coloring = false;

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
   
      panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
   
      final GUI gui = this;
      final int totalSize = this.numPixelRows * this.numPixelColumns;
   
      for (int row = 0; row < this.numPixelRows; row++)
      {
      
         final JPanel rowPanel = new JPanel();
      
         rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.LINE_AXIS));
      
         for (int column = 0; column < this.numPixelColumns; column++)
         {
         
            final JButton cell = new JButton();
         
            cell.setPreferredSize(CELL_DIMENSIONS);
            cell.setMaximumSize(CELL_DIMENSIONS);
            cell.setMinimumSize(CELL_DIMENSIONS);
            cell.setBackground(Color.WHITE);
            cell.setBorder(ORIGINAL_BORDER);
            cell.setRolloverEnabled(false);
            cell.addActionListener(event -> cell.setBackground(this.currentColor));
         
            final int currentIndex = (row * this.numPixelColumns) + column;
         
            ADD_KEYBOARD_MOVEMENT:
            {
            
               cell
                  .addFocusListener
                  (
                     new
                        FocusListener()
                     {
                     
                        public void focusGained(final FocusEvent event)
                        {
                        
                           cell
                              .setBorder
                              (
                                 BorderFactory
                                    .createCompoundBorder
                                    (
                                       BorderFactory
                                          .createLineBorder
                                          (
                                             Color.BLACK,
                                             1
                                          ),
                                       BorderFactory
                                          .createLineBorder
                                          (
                                             Color.WHITE,
                                             1
                                          )
                                    )
                              );
                        
                           if (gui.coloring)
                           {
                           
                              cell.doClick();
                           
                           }
                        
                        }
                     
                        public void focusLost(final FocusEvent event)
                        {
                        
                           cell.setBorder(ORIGINAL_BORDER);
                        
                        }
                     
                     }
                  )
                  ;
            
               final Function<KeyStroke, Action> actionFunction =
                  keyStroke ->
                  new AbstractAction()
                  {
                  
                     public void actionPerformed(final ActionEvent event)
                     {
                     
                        Objects.requireNonNull(keyStroke);
                     
                        if (keyStroke.getModifiers() == 0)
                        {
                        
                           gui
                              .cells
                              .get
                              (
                                 switch (keyStroke.getKeyCode())
                                 {
                                 
                                    case KeyEvent.VK_UP     -> currentIndex >= gui.numPixelColumns                            ? currentIndex - gui.numPixelColumns : currentIndex;
                                    case KeyEvent.VK_DOWN   -> currentIndex < totalSize - gui.numPixelColumns                 ? currentIndex + gui.numPixelColumns : currentIndex;
                                    case KeyEvent.VK_LEFT   -> currentIndex % gui.numPixelColumns != 0                        ? currentIndex - 1 : currentIndex;
                                    case KeyEvent.VK_RIGHT  -> currentIndex % gui.numPixelColumns != gui.numPixelColumns - 1  ? currentIndex + 1 : currentIndex;
                                    default                 -> currentIndex;
                                 
                                 }
                              )
                              .requestFocus();
                        
                        }
                     
                     }
                  
                  }
                  ;
            
               this.setKeyBinding(cell, UP,     actionFunction);
               this.setKeyBinding(cell, DOWN,   actionFunction);
               this.setKeyBinding(cell, LEFT,   actionFunction);
               this.setKeyBinding(cell, RIGHT,  actionFunction);
            
            }
         
            ADD_KEYBOARD_COLORING:
            {
            
               final Function<KeyStroke, Action> actionFunction =
                  keyStroke ->
                     new AbstractAction()
                     {
                     
                        public void actionPerformed(final ActionEvent event)
                        {
                        
                           if (!gui.coloring && keyStroke == SPACE_PRESS)
                           {
                           
                              System.out.println("PRESSED SPACE");
                              gui.coloring = true;
                           
                           }
                           
                           else if (keyStroke == SPACE_RELEASE)
                           {
                           
                              System.out.println("RELEASED SPACE");
                              gui.coloring = false;
                           
                           }
                        
                        }
                     
                     }
                  ;
            
               this.setKeyBinding(cell, SPACE_PRESS, actionFunction);
               this.setKeyBinding(cell, SPACE_RELEASE, actionFunction);
            
            }
         
            this.cells.add(cell);
         
            rowPanel.add(cell);
         
         }
      
         panel.add(rowPanel);
      
      }
   
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
      final List<Character> KEY_LIST = List.of('U', 'I', 'O', 'H', 'J', 'K', 'B', 'N', 'M');
   
      final List<Color> COLOR_LIST =
         List.of(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.PINK, Color.WHITE, Color.BLACK);
   
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
                  {
                  
                     final Color chosenColor =
                        JColorChooser
                           .showDialog
                           (
                              button,
                              DIALOG_TITLE,
                              button.getBackground()
                           );
                  
                     this.currentColor = chosenColor;
                     button.setBackground(chosenColor);
                  
                  }
               );
         
         
         }
      
         panel.add(button);
      
      }
   
      return panel;
   
   }

   private void setKeyBinding(final JComponent component, final KeyStroke keyStroke, final Function<KeyStroke, Action> actionFunction)
   {
   
      Objects.requireNonNull(component);
      Objects.requireNonNull(keyStroke);
      Objects.requireNonNull(actionFunction);
   
      component.getInputMap().put(keyStroke, keyStroke.toString());
      component.getActionMap().put(keyStroke.toString(), actionFunction.apply(keyStroke));
   
   }

}
