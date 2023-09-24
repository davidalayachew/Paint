
package Paint;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public class GUI
{

   private enum KeyboardColorHotKey
   {
   
      U(Color.RED),
      I(Color.ORANGE),
      O(Color.YELLOW),
      H(Color.GREEN),
      J(Color.BLUE),
      K(Color.MAGENTA),
      B(Color.PINK),
      N(Color.WHITE),
      M(Color.BLACK),
      ;
   
      public final Color color;
   
      KeyboardColorHotKey(final Color color)
      {
      
         this.color = color;
      
      }
   
   }

   private static final ExecutorService THREADER =
      Executors
         .newWorkStealingPool()
         ;

   private static final Dimension CELL_DIMENSIONS  = new Dimension(10, 10);
   private static final KeyStroke UP               = KeyStroke.getKeyStroke(KeyEvent.VK_UP,     0, false);
   private static final KeyStroke DOWN             = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   0, false);
   private static final KeyStroke LEFT             = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   0, false);
   private static final KeyStroke RIGHT            = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  0, false);
   private static final KeyStroke SPACE_PRESS      = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,  0, false);
   private static final KeyStroke SPACE_RELEASE    = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,  0, true);
   private static final Border ORIGINAL_BORDER     = new JButton().getBorder();
   private static final Border SELECTED_BORDER     =
      BorderFactory
         .createCompoundBorder
         (
            BorderFactory
               .createLineBorder
               (
                  Color.BLACK,
                  2
               ),
            BorderFactory
               .createLineBorder
               (
                  Color.WHITE,
                  2
               )
         )
         ;

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

   private static final int MIN_PEN_SIZE = 1;
   private static final int MAX_PEN_SIZE = 10;

   private final JFrame frame;
   private final List<JButton> cells = new ArrayList<>();

   private Color currentColor = Color.BLACK;
   private boolean coloring = false;
   private int numPixelRows = 26;
   private int numPixelColumns = 24;
   private int penSize = 1;

   @SuppressWarnings("this-escape")
   public GUI()
   {
   
      this.frame = new JFrame();
   
      this.frame.setTitle("Paint");
      this.frame.setLocationByPlatform(true);
      this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   
      this.frame.add(this.createMainPanel());
   
      this.frame.pack();
      this.frame.setVisible(true);
   
   }

   private JPanel createMainPanel()
   {
   
      final JPanel mainPanel = new JPanel(new BorderLayout());
   
      mainPanel.add(this.createTopPanel(),         BorderLayout.NORTH);
      mainPanel.add(this.createCenterPanel(),      BorderLayout.CENTER);
      mainPanel.add(this.createBottomPanel(),      BorderLayout.SOUTH);
   
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
         
            final int rowCopy = row;
            final int columnCopy = column;
         
            final class CellConsumer extends SwingWorker<JButton, Object>
            {
            
               public final int indexValue;
               private final Consumer<JButton> idca;
            
               public CellConsumer(final int indexValue, final Consumer<JButton> idca)
               {
               
                  this.indexValue = indexValue;
                  this.idca = idca;
               
               }
            
               @Override
               public JButton doInBackground()
               {
               
                  return cells.get(indexValue);
               
               }
            
               @Override
               protected void done()
               {
               
                  try
                  {
                  
                     this.idca.accept(this.get());
                  
                  }
                  
                  catch (final Exception e)
                  {
                  
                     throw new RuntimeException(e);
                  
                  }
               
               }
            
            }
         
            final Consumer<Consumer<JButton>> gridAction =
               buttonConsumer ->
                  IntStream
                     .iterate
                     (
                        rowCopy,
                        hRow -> hRow < rowCopy + gui.penSize && hRow < gui.numPixelRows,
                        hRow -> hRow + 1
                     )
                     .parallel()
                     .flatMap
                     (
                        hRow ->
                           IntStream
                              .iterate
                              (
                                 columnCopy,
                                 hColumn -> hColumn < columnCopy + gui.penSize && hColumn < gui.numPixelColumns,
                                 hColumn -> hColumn + 1
                              )
                              .parallel()
                              .map(hColumn -> (hRow * gui.numPixelColumns) + hColumn)
                     )
                     .parallel()
                     .mapToObj(someIndex -> new CellConsumer(someIndex, buttonConsumer))
                     .map(THREADER::submit)
                     .forEach(GUI::join)
                     ;
         
            cell
               .addActionListener
               (
                  event ->
                  {
                  
                     gridAction.accept(eachButton -> eachButton.setBackground(gui.currentColor));
                  
                  }
               )
               ;
         
            ADD_KEYBOARD_MOVEMENT:
            {
            
               cell
                  .addFocusListener
                  (
                     new FocusListener()
                     {
                     
                        public void focusGained(final FocusEvent event)
                        {
                        
                           gridAction
                              .accept
                              (
                                 eachButton ->
                                 {
                                 
                                    eachButton.setBorder(SELECTED_BORDER);
                                 
                                    if (gui.coloring)
                                    {
                                    
                                       eachButton.setBackground(gui.currentColor);
                                    
                                    }
                                 
                                 }
                              );
                        
                        }
                     
                        public void focusLost(final FocusEvent event)
                        {
                        
                           gridAction.accept(eachButton -> eachButton.setBorder(ORIGINAL_BORDER));
                        
                        }
                     
                     }
                  )
                  ;
            
               final int currentIndex = (row * this.numPixelColumns) + column;
            
               final BiFunction<JButton, KeyStroke, Action> actionFunction =
                  (yeughh, keyStroke) ->
                     new AbstractAction()
                     {
                     
                        public void actionPerformed(final ActionEvent event)
                        {
                        
                           Objects.requireNonNull(keyStroke);
                        
                           if (keyStroke.getModifiers() == 0)
                           {
                           
                              final JComponent nextCell =
                                 gui.fetchNextCell(keyStroke, currentIndex, totalSize);
                           
                              nextCell.requestFocus();
                           
                              gridAction
                                 .accept
                                 (
                                    eachButton ->
                                    {
                                    
                                       if (gui.coloring)
                                       {
                                       
                                          eachButton.setBackground(gui.currentColor);
                                       
                                       }
                                    
                                    }
                                 );
                           
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
            
               final BiFunction<JButton, KeyStroke, Action> actionFunction =
                  (someButton, keyStroke) ->
                     new AbstractAction()
                     {
                     
                        public void actionPerformed(final ActionEvent event)
                        {
                        
                           if (!gui.coloring && keyStroke == SPACE_PRESS)
                           {
                           
                              System.out.println("PRESSED SPACE");
                              gui.coloring = true;
                              someButton.doClick();
                           
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
   
      final JPanel panel = new JPanel(new GridLayout(NUM_ROWS, NUM_COLUMNS));
   
      panel.setBorder(TITLED_BORDER.apply("Color"));
   
      for (int index = 0; index < NUM_ROWS * NUM_COLUMNS; index++)
      {
      
         final JButton button =
            new JButton("" + KeyboardColorHotKey.values()[index])
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
            
            }
            ;
      
         button.setFont(button.getFont().deriveFont(20.0f));
         button.setOpaque(false);
         button.setBackground(KeyboardColorHotKey.values()[index].color);
      
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

   private JPanel createBottomPanel()
   {
   
      final JPanel panel = new JPanel();
   
      panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
   
      final JButton save = new JButton();
   
      save.setText("SAVE");
   
      enum ImageType
      {
      
         PNG,
         GIF,
         ;
      
      }
   
      final JComboBox<ImageType> imageTypeDropDownMenu = new JComboBox<>(ImageType.values());
   
      save
         .addActionListener
         (
            event ->
            {
            
               final int selectedIndex = imageTypeDropDownMenu.getSelectedIndex();
            
               final ImageType imageType = ImageType.values()[selectedIndex];
            
               final List<Color> pixels =
                  this
                     .cells
                     .stream()
                     .map(JButton::getBackground)
                     .toList()
                     ;
            
               PERFORM_VALIDATIONS:
               {
               
                  final Predicate<Color> isOpaqueOrTransparent =
                     givenColor ->
                        givenColor.getAlpha() == 0
                        ||
                        givenColor.getAlpha() == 255
                        ;
               
                  final boolean canSaveCorrectly =
                     switch (imageType)
                     {
                     
                        case  PNG   -> true;
                        case  GIF   ->
                           pixels
                              .stream()
                              .allMatch(isOpaqueOrTransparent)
                              ;
                     
                     }
                     ;
               
                  if (!canSaveCorrectly)
                  {
                  
                     record Pixel(int row, int column, Color color)
                     {
                     
                        public Pixel
                        {
                        
                           Objects.requireNonNull(color);
                        
                        }
                     
                        public static Pixel of(final int index, final Color color, final GUI gui)
                        {
                        
                           Objects.requireNonNull(color);
                           Objects.requireNonNull(gui);
                        
                           final int maxRows    = gui.numPixelRows;
                           final int maxColumns = gui.numPixelColumns;
                        
                           final int row     = index / maxColumns;
                           final int column  = index % maxColumns;
                        
                           return
                              new
                                 Pixel
                                 (
                                    row,
                                    column,
                                    color
                                 );
                        
                        }
                     
                     }
                  
                     final JPanel listOfBadPixels = new JPanel();
                     listOfBadPixels.setLayout(new BoxLayout(listOfBadPixels, BoxLayout.PAGE_AXIS));
                     listOfBadPixels
                        .add
                        (
                           new
                              JLabel
                              (
                                 """
                                 <html>
                                 Cannot have translucent pixels in a GIF!<br>
                                 Translucent means that the pixel has an alpha value where 0 &lt; alpha &lt; 255!<br>
                                 Here are the list of translucent pixels.<br>
                                 Please remember, the top-left most pixel is row = 0 and column = 0!
                                 </html>
                                 """
                              )
                        )
                        ;
                  
                     listOfBadPixels
                        .add
                        (
                           new
                              JScrollPane
                              (
                                 new
                                    JList<String>
                                    (
                                       IntStream
                                          .range(0, pixels.size())
                                          .mapToObj(eachInt -> Pixel.of(eachInt, pixels.get(eachInt), this))
                                          .filter(eachPixel -> !isOpaqueOrTransparent.test(eachPixel.color()))
                                          .map(eachPixel -> eachPixel + " -- alpha = " + eachPixel.color().getAlpha())
                                          .toArray(String[]::new)
                                    )
                              )
                        )
                        ;
                  
                     JOptionPane
                        .showMessageDialog
                        (
                           this.frame,
                           listOfBadPixels,
                           "Cannot have translucent pixels in a GIF!",
                           JOptionPane.ERROR_MESSAGE
                        );
                  
                     return;
                  
                  }
               
               }
            
               final BufferedImage finalImage =
                  new
                     BufferedImage
                     (
                        this.numPixelColumns,
                        this.numPixelRows,
                        BufferedImage.TYPE_INT_ARGB
                     )
                     ;
            
               for (int row = 0; row < this.numPixelRows; row++)
               {
               
                  for (int column = 0; column < this.numPixelColumns; column++)
                  {
                  
                     final int index = (row * this.numPixelColumns) + column;
                  
                     final Color pixel = pixels.get(index);
                     System.out.println(pixel);
                     System.out.println(pixel.getRGB());
                     finalImage.setRGB(column, row, pixel.getRGB());
                  
                  }
               
               }
            
               try
               {
               
                  final String imageTypeString = imageType.name().toLowerCase();
               
                  ImageIO
                     .write
                     (
                        finalImage,
                        imageTypeString,
                        new
                           File
                           (
                              LocalDateTime
                                 .now()
                                 .format
                                 (
                                    DateTimeFormatter
                                       .ofPattern("yyyyMMdd_HHmmss_SSS")
                                 )
                                 +
                                 "."
                                 +
                                 imageType
                           )
                     )
                     ;
               
               }
               
               catch (final Exception e)
               {
               
                  throw new RuntimeException(e);
               
               }
            
            }
         
         )
         ;
   
      panel.add(save);
      panel.add(imageTypeDropDownMenu);
   
      return panel;
   
   }

   private JComponent fetchNextCell(final KeyStroke keyStroke, final int currentIndex, final int totalSize)
   {
   
      return
         this
            .cells
            .get
            (
               switch (keyStroke.getKeyCode())
               {
               
                  case KeyEvent.VK_UP     -> currentIndex   >= this.numPixelColumns                               ? currentIndex - this.numPixelColumns : currentIndex;
                  case KeyEvent.VK_DOWN   -> currentIndex   <  totalSize - this.numPixelColumns                   ? currentIndex + this.numPixelColumns : currentIndex;
                  case KeyEvent.VK_LEFT   -> currentIndex   %  this.numPixelColumns != 0                          ? currentIndex - 1 : currentIndex;
                  case KeyEvent.VK_RIGHT  -> currentIndex   %  this.numPixelColumns != this.numPixelColumns - 1   ? currentIndex + 1 : currentIndex;
                  default                 -> currentIndex;
               
               }
            )
            ;
   
   }

   private static <T> void join(final Future<T> future)
   {
   
      try
      {
      
         System.out.println(future.get());
      
      }
      
      catch (final Exception e)
      {
      
         throw new RuntimeException(e);
      
      }
   
   }

   private void setKeyBinding(final JButton button, final KeyStroke keyStroke, final BiFunction<JButton, KeyStroke, Action> actionFunction)
   {
   
      Objects.requireNonNull(button);
      Objects.requireNonNull(keyStroke);
      Objects.requireNonNull(actionFunction);
   
      button.getInputMap().put(keyStroke, keyStroke.toString());
      button.getActionMap().put(keyStroke.toString(), actionFunction.apply(button, keyStroke));
   
   }

}
