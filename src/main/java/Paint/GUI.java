
package Paint;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public final class GUI
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
   private static final KeyStroke UP               = KeyStroke.getKeyStroke(KeyEvent.VK_UP,     0, false );
   private static final KeyStroke DOWN             = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   0, false );
   private static final KeyStroke LEFT             = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   0, false );
   private static final KeyStroke RIGHT            = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  0, false );
   private static final KeyStroke SPACE_PRESS      = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,  0, false );
   private static final KeyStroke SPACE_RELEASE    = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,  0, true  );

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
   private static final int DEFAULT_IMAGE_PIXEL_ROWS = 26;
   private static final int DEFAULT_IMAGE_PIXEL_COLUMNS = 24;

   private final List<Color> imagePixels =
      Arrays.asList(new Color[DEFAULT_IMAGE_PIXEL_ROWS * DEFAULT_IMAGE_PIXEL_COLUMNS]);
   private final JFrame frame;

   private Color transparencyColor = Color.WHITE;
   private Color cursorColor = Color.BLACK;
   private Point mouseCurrentLocation = new Point(0, 0);
   private boolean coloring = false;
   private int numImagePixelRows = DEFAULT_IMAGE_PIXEL_ROWS;
   private int numImagePixelColumns = DEFAULT_IMAGE_PIXEL_COLUMNS;
   private int penSize = 1;
   private int screenToImagePixelRatio = 10;

   public GUI()
   {
   
      try
      {
      
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      
      }
      
      catch (Exception e)
      {
      
         throw new RuntimeException(e);
      
      }
   
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
      //topPanel.add(this.colorToShowWhenUsingTransparentOrTranslucentPixels());
   
      return topPanel;
   
   }

   private JPanel createCenterPanel()
   {
   
      final GUI gui = this; //useful when trying to differentiate between different this'.
   
      final JPanel mainPanel;
   
      CREATE_MAIN_PANEL:
      {
      
         mainPanel = new JPanel();
         mainPanel.setLayout(new BorderLayout());
      
      }
   
      final JPanel drawingPanel;
   
      final Runnable UPDATE_DRAWING_PANEL_BORDER_TEXT;
      final Runnable UPDATE_DRAWING_PANEL_ZOOM_LEVEL;
      final Runnable REPAINT_DRAWING_PANEL;
   
      CREATE_DRAWING_PANEL:
      {
      
         SET_UP_DRAWING_PANEL:
         {
         
            drawingPanel = new JPanel();
         
            drawingPanel.setLayout(new BoxLayout(drawingPanel, BoxLayout.PAGE_AXIS));
         
            drawingPanel.add(Box.createRigidArea(new Dimension(123, 456)));
         
         }
      
         REPAINT_DRAWING_PANEL =
            () ->
            {
            
               drawingPanel.repaint();
               drawingPanel.revalidate();
            
            }
            ;
      
         UPDATE_DRAWING_PANEL_BORDER_TEXT =
            () ->
               drawingPanel
                  .setBorder
                  (
                     BorderFactory
                        .createCompoundBorder
                        (
                           TITLED_BORDER
                              .apply
                              (
                                 "Drawing Area -- "
                                 + this.numImagePixelRows
                                 + " rows and "
                                 + this.numImagePixelColumns
                                 + " columns"
                              ),
                           BorderFactory.createLineBorder(Color.BLACK, 1)
                        )
                  )
                  ;
      
         UPDATE_DRAWING_PANEL_BORDER_TEXT.run();
      
         UPDATE_DRAWING_PANEL_ZOOM_LEVEL =
            () ->
            {
            
               drawingPanel.removeAll();
            
               final Dimension drawingArea = gui.deriveDrawingAreaDimensions();
            
               final Box.Filler box =
                  new Box.Filler(drawingArea, drawingArea, drawingArea)
                  {
                  
                     @Override
                     protected void paintComponent(final Graphics dontUse)
                     {
                     
                        if (!(dontUse instanceof Graphics2D g))
                        {
                        
                           throw new RuntimeException("Unknown graphics type = " + dontUse);
                        
                        }
                        
                        DRAW_DRAWN_PIXELS:
                        {
                        
                           IntStream
                              .range(0, gui.numImagePixelRows * gui.numImagePixelColumns)
                              .forEach
                              (
                                 eachIndex ->
                                 {
                                 
                                    final Color currentPixel = gui.imagePixels.get(eachIndex);
                                 
                                    final int x = eachIndex % gui.numImagePixelColumns;
                                    final int y = eachIndex / gui.numImagePixelColumns;
                                 
                                    g.setColor(currentPixel == null ? gui.transparencyColor : currentPixel);
                                    g
                                       .fillRect
                                       (
                                          x * gui.screenToImagePixelRatio,
                                          y * gui.screenToImagePixelRatio,
                                          gui.screenToImagePixelRatio,
                                          gui.screenToImagePixelRatio
                                       )
                                       ;
                                 
                                 }
                              )
                              ;
                        
                        }
                     
                        DRAW_CURSOR_PIXELS:
                        {
                        
                           final int screenPixelCursorSize = gui.screenToImagePixelRatio * gui.penSize;
                        
                           g.setColor(gui.cursorColor);
                           g
                              .fillRect
                              (
                                 gui.mouseCurrentLocation.x,
                                 gui.mouseCurrentLocation.y,
                                 screenPixelCursorSize,
                                 screenPixelCursorSize
                              )
                              ;
                        
                        }
                     
                     }
                  
                  }
                  ;
            
               box
                  .addMouseListener
                  (
                     new MouseAdapter()
                     {
                     
                        @Override
                        public void mousePressed(final MouseEvent mouseEvent)
                        {
                        
                           final Point maybeNewPoint = mouseEvent.getPoint();
                        
                           if
                           (
                              maybeNewPoint.x < 0
                              ||
                              maybeNewPoint.x >= drawingArea.width
                              ||
                              maybeNewPoint.y < 0
                              ||
                              maybeNewPoint.y >= drawingArea.height
                           )
                           {
                           
                              return;
                           
                           }
                        
                           final int x = (maybeNewPoint.x - (maybeNewPoint.x % gui.screenToImagePixelRatio)) / gui.screenToImagePixelRatio;
                           final int y = (maybeNewPoint.y - (maybeNewPoint.y % gui.screenToImagePixelRatio)) / gui.screenToImagePixelRatio;
                        
                           final int index = (y * gui.numImagePixelColumns) + x;
                        
                           final int startRow      = index / gui.numImagePixelColumns;
                           final int startColumn   = index % gui.numImagePixelColumns;
                        
                           for
                           (
                              int row = startRow;
                              row < gui.imagePixels.size() / gui.numImagePixelColumns
                                 && row < startRow + gui.penSize;
                              row++
                           )
                           {
                           
                              for
                              (
                                 int column = startColumn;
                                 column < gui.imagePixels.size() / gui.numImagePixelRows
                                    && column < startColumn + gui.penSize;
                                 column++
                              )
                              {
                              
                                 gui.imagePixels.set((row * gui.numImagePixelColumns) + column, gui.cursorColor);
                              
                              }
                           
                           }
                        
                           REPAINT_DRAWING_PANEL.run();
                        
                        }
                     
                     }
                  )
                  ;
            
               box
                  .addMouseMotionListener
                  (
                     new MouseMotionListener()
                     {
                     
                        @Override
                        public void mouseDragged(final MouseEvent mouseEvent)
                        {
                        
                           final Point maybeNewPoint = mouseEvent.getPoint();
                           
                           if
                           (
                              maybeNewPoint.x < 0
                              ||
                              maybeNewPoint.x >= drawingArea.width
                              ||
                              maybeNewPoint.y < 0
                              ||
                              maybeNewPoint.y >= drawingArea.height
                           )
                           {
                           
                              return;
                           
                           }
                        
                           final int x = (maybeNewPoint.x - (maybeNewPoint.x % gui.screenToImagePixelRatio)) / gui.screenToImagePixelRatio;
                           final int y = (maybeNewPoint.y - (maybeNewPoint.y % gui.screenToImagePixelRatio)) / gui.screenToImagePixelRatio;
                        
                           final int index = (y * gui.numImagePixelColumns) + x;
                        
                           final int startRow      = index / gui.numImagePixelColumns;
                           final int startColumn   = index % gui.numImagePixelColumns;
                        
                           for
                           (
                              int row = startRow;
                              row < gui.imagePixels.size() / gui.numImagePixelColumns
                                 && row < startRow + gui.penSize;
                              row++
                           )
                           {
                           
                              for
                              (
                                 int column = startColumn;
                                 column < gui.imagePixels.size() / gui.numImagePixelRows
                                    && column < startColumn + gui.penSize;
                                 column++
                              )
                              {
                              
                                 gui.imagePixels.set((row * gui.numImagePixelColumns) + column, gui.cursorColor);
                              
                              }
                           
                           }
                        
                           REPAINT_DRAWING_PANEL.run();
                        
                        }
                     
                        @Override
                        public void mouseMoved(final MouseEvent mouseEvent)
                        {
                        
                           final Point maybeNewPoint = mouseEvent.getPoint();
                        
                           final int x = maybeNewPoint.x - (maybeNewPoint.x % gui.screenToImagePixelRatio);
                           final int y = maybeNewPoint.y - (maybeNewPoint.y % gui.screenToImagePixelRatio);
                        
                           maybeNewPoint.setLocation(x, y);
                        
                           if (!gui.mouseCurrentLocation.equals(maybeNewPoint))
                           {
                           
                              gui.mouseCurrentLocation = maybeNewPoint;
                           
                              REPAINT_DRAWING_PANEL.run();
                           
                           }
                        
                        }
                     
                     }
                  )
                  ;
            
               drawingPanel.add(Box.createHorizontalGlue());
               drawingPanel.add(box);
               drawingPanel.add(Box.createHorizontalGlue());
            
               REPAINT_DRAWING_PANEL.run();
            
            }
            ;
      
         UPDATE_DRAWING_PANEL_ZOOM_LEVEL.run();
      
      }
   
      final JPanel drawingSettingsPanel;
   
      CREATE_DRAWING_SETTINGS_PANEL:
      {
      
         drawingSettingsPanel = new JPanel();
         drawingSettingsPanel.setLayout(new BoxLayout(drawingSettingsPanel, BoxLayout.LINE_AXIS));
      
         final JComboBox<Integer> screenToImagePixelRatioDropDownMenu =
            new JComboBox<>(IntStream.rangeClosed(10, 30).boxed().toArray(Integer[]::new));
      
         screenToImagePixelRatioDropDownMenu
            .addActionListener
            (
               event ->
               {
               
                  this.screenToImagePixelRatio =
                     screenToImagePixelRatioDropDownMenu
                        .getItemAt
                        (
                           screenToImagePixelRatioDropDownMenu.getSelectedIndex()
                        )
                        ;
               
                  this.mouseCurrentLocation = new Point(0, 0);
               
                  UPDATE_DRAWING_PANEL_ZOOM_LEVEL.run();
               
               }
            );
      
         final JButton transparencyColorChooser;
      
         TRANSPARENCY_COLOR_CHOOSER:
         {
         
            transparencyColorChooser = new JButton();
         
            transparencyColorChooser.setText("Transparency Color");
         
            transparencyColorChooser
               .addActionListener
               (
                  event ->
                  {
                  
                     final Color chosenColor =
                        JColorChooser
                           .showDialog
                           (
                              this.frame,
                              "Choose the Drawing Area's Transparency color!",
                              this.transparencyColor,
                              false
                           )
                           ;
                  
                     if (chosenColor == null)
                     {
                     
                        return;
                     
                     }
                  
                     this.transparencyColor = chosenColor;
                  
                     REPAINT_DRAWING_PANEL.run();
                  
                  }
               )
               ;
         
         }
      
         final JButton cursorColorChooser;
      
         CURSOR_COLOR_CHOOSER:
         {
         
            cursorColorChooser = new JButton();
         
            cursorColorChooser.setText("Cursor Color");
         
            cursorColorChooser
               .addActionListener
               (
                  event ->
                  {
                  
                     final Color chosenColor =
                        JColorChooser
                           .showDialog
                           (
                              this.frame,
                              "Choose the Drawing Area's Cursor color!",
                              this.cursorColor,
                              false
                           )
                           ;
                  
                     if (chosenColor == null)
                     {
                     
                        return;
                     
                     }
                  
                     this.cursorColor = chosenColor;
                  
                     REPAINT_DRAWING_PANEL.run();
                  
                  }
               )
               ;
         
         }
      
         drawingSettingsPanel.add(Box.createHorizontalGlue());
         drawingSettingsPanel.add(screenToImagePixelRatioDropDownMenu);
         drawingSettingsPanel.add(new JLabel("SCREEN pixels = 1 IMAGE pixel"));
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(transparencyColorChooser);
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(cursorColorChooser);
         drawingSettingsPanel.add(Box.createHorizontalGlue());
      
      }
   
      mainPanel.add(drawingSettingsPanel, BorderLayout.NORTH);
      mainPanel.add(drawingPanel, BorderLayout.CENTER);
   
      return mainPanel;
   
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
            
               this.mouseCurrentLocation = new Point(0, 0);
               
               this.frame.repaint();
            
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
                  
                     this.cursorColor = chosenColor;
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
   
      final Color transparentColor = new Color(0, 0, 0, 0);
   
      final List<Color> pixels =
         this
            .imagePixels
            .stream()
            .map(each -> Objects.requireNonNullElse(each, transparentColor))
            .toList()
            ;
   
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
            actionEvent ->
            {
            
               final int selectedIndex = imageTypeDropDownMenu.getSelectedIndex();
            
               final ImageType imageType = ImageType.values()[selectedIndex];
            
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
                        
                           final int maxRows    = gui.numImagePixelRows;
                           final int maxColumns = gui.numImagePixelColumns;
                        
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
            
               // final BufferedImage finalImage =
                  // new
                  //    BufferedImage
                  //    (
                  //       this.numImagePixelColumns,
                  //       this.numImagePixelRows,
                  //       BufferedImage.TYPE_INT_ARGB
                  //    )
                  //    ;
            //
               // for (int row = 0; row < this.numImagePixelRows; row++)
               // {
               //
                  // for (int column = 0; column < this.numImagePixelColumns; column++)
                  // {
                  //
                     // final int index = (row * this.numImagePixelColumns) + column;
                  //
                     // final Color pixel = pixels.get(index);
                     // System.out.println(pixel);
                     // System.out.println(pixel.getRGB());
                     // finalImage.setRGB(column, row, pixel.getRGB());
                  //
                  // }
               //
               // }
            //
               // try
               // {
               //
                  // final String imageTypeString = imageType.name().toLowerCase();
               //
                  // ImageIO
                     // .write
                     // (
                     //    finalImage,
                     //    imageTypeString,
                     //    new
                     //       File
                     //       (
                     //          LocalDateTime
                     //             .now()
                     //             .format
                     //             (
                     //                DateTimeFormatter
                     //                   .ofPattern("yyyyMMdd_HHmmss_SSS")
                     //             )
                     //             +
                     //             "."
                     //             +
                     //             imageType
                     //       )
                     // )
                     // ;
               //
               // }
               //
               // catch (final Exception e)
               // {
               //
                  // throw new RuntimeException(e);
               //
               // }
            //
            }
         )
         ;
   
      // panel.add(save);
      // panel.add(imageTypeDropDownMenu);
   
      return panel;
   
   }

   private Dimension deriveDrawingAreaDimensions()
   {
   
      return
         new Dimension
         (
            this.numImagePixelColumns * this.screenToImagePixelRatio,
            this.numImagePixelRows * this.screenToImagePixelRatio
         )
         ;
   
   }

}
