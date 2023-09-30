
package Paint;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.colorchooser.*;
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
      Arrays
         .asList(new Color[DEFAULT_IMAGE_PIXEL_ROWS * DEFAULT_IMAGE_PIXEL_COLUMNS]);
   private final JFrame frame;

   private Color transparencyColor = Color.WHITE;
   private Color cursorColor = Color.BLACK;
   private Color gridLinesColor = Color.GRAY;
   private Point mouseCurrentLocation = new Point(0, 0);
   private boolean coloring = false;
   private boolean hasGridLines = true;
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
   
      final Color transparentColor = new Color(0, 0, 0, 0);
   
      final List<Color> pixels =
         this
            .imagePixels
            .stream()
            .map(each -> Objects.requireNonNullElse(each, transparentColor))
            .toList()
            ;
   
      final int maxRows    = this.numImagePixelRows;
      final int maxColumns = this.numImagePixelColumns;
   
      mainPanel.add(this.createTopPanel(),                                 BorderLayout.NORTH);
      mainPanel.add(this.createCenterPanel(this.mouseCurrentLocation, this.screenToImagePixelRatio),                              BorderLayout.CENTER);
      mainPanel.add(this.createBottomPanel(pixels, maxRows, maxColumns),   BorderLayout.SOUTH);
   
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

   private JPanel createCenterPanel(final Point mouseCurrentLocation, final int jumpDistance)
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
            
               final var thingToFocus = drawingPanel;
            
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
                        
                           DRAW_IMAGE:
                           {
                           
                              IntStream
                                 .range(0, gui.numImagePixelRows * gui.numImagePixelColumns)
                                 .forEach
                                 (
                                    eachIndex ->
                                    {
                                    
                                       final Color currentPixel = gui.imagePixels.get(eachIndex);
                                    
                                       final int imagePixelX = eachIndex % gui.numImagePixelColumns;
                                       final int imagePixelY = eachIndex / gui.numImagePixelColumns;
                                    
                                       final int screenPixelX = imagePixelX * gui.screenToImagePixelRatio;
                                       final int screenPixelY = imagePixelY * gui.screenToImagePixelRatio;
                                    
                                       final int screenPixelCursorSize = gui.screenToImagePixelRatio * gui.penSize;
                                    
                                       g.setColor(currentPixel == null ? gui.transparencyColor : currentPixel);
                                       g
                                          .fillRect
                                          (
                                             screenPixelX,
                                             screenPixelY,
                                             gui.screenToImagePixelRatio,
                                             gui.screenToImagePixelRatio
                                          )
                                          ;
                                    
                                    }
                                 )
                                 ;
                           
                           }
                        
                           DRAW_CURSOR:
                           {
                           
                              IntStream
                                 .range(0, gui.numImagePixelRows * gui.numImagePixelColumns)
                                 .forEach
                                 (
                                 
                                 
                                    eachIndex ->
                                    {
                                    
                                       final int imagePixelX = eachIndex % gui.numImagePixelColumns;
                                       final int imagePixelY = eachIndex / gui.numImagePixelColumns;
                                    
                                       final int screenPixelX = imagePixelX * gui.screenToImagePixelRatio;
                                       final int screenPixelY = imagePixelY * gui.screenToImagePixelRatio;
                                    
                                       final int screenPixelCursorSize = gui.screenToImagePixelRatio * gui.penSize;
                                    
                                       g.setColor(gui.cursorColor);
                                    
                                       if (new Point(screenPixelX, screenPixelY).equals(gui.mouseCurrentLocation))
                                       {
                                       
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
                                 )
                                 ;
                           
                           }
                        
                           DRAW_GRID_LINES:
                           {
                           
                              if (gui.hasGridLines)
                              {
                              
                                 IntStream
                                    .range(0, gui.numImagePixelRows * gui.numImagePixelColumns)
                                    .forEach
                                    (
                                       eachIndex ->
                                       {
                                       
                                          final int imagePixelX = eachIndex % gui.numImagePixelColumns;
                                          final int imagePixelY = eachIndex / gui.numImagePixelColumns;
                                       
                                          final int screenPixelX = imagePixelX * gui.screenToImagePixelRatio;
                                          final int screenPixelY = imagePixelY * gui.screenToImagePixelRatio;
                                       
                                          g.setColor(gui.gridLinesColor);
                                          g
                                             .drawRect
                                             (
                                                screenPixelX,
                                                screenPixelY,
                                                gui.screenToImagePixelRatio - 1,
                                                gui.screenToImagePixelRatio - 1
                                             )
                                             ;
                                       
                                       }
                                    )
                                    ;
                              
                              }
                           
                           }
                        
                        }
                     
                     }
                  
                  }
                  ;
            
               final Consumer<Point> performClick =
                  maybeNewPoint ->
                  {
                  
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
                  
                     gui.mouseCurrentLocation.setLocation(maybeNewPoint);
                  
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
                  ;
            
               final Consumer<Point> performMove =
                  maybeNewPoint ->
                  {
                  
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
                  
                     final int x = maybeNewPoint.x - (maybeNewPoint.x % gui.screenToImagePixelRatio);
                     final int y = maybeNewPoint.y - (maybeNewPoint.y % gui.screenToImagePixelRatio);
                  
                     maybeNewPoint.setLocation(x, y);
                  
                     if (!gui.mouseCurrentLocation.equals(maybeNewPoint))
                     {
                     
                        gui.mouseCurrentLocation.setLocation(maybeNewPoint);
                     
                        REPAINT_DRAWING_PANEL.run();
                     
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
                        
                           performClick.accept(mouseEvent.getPoint());
                        
                           thingToFocus.requestFocus();
                        
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
                        
                           performClick.accept(mouseEvent.getPoint());
                        
                        }
                     
                        @Override
                        public void mouseMoved(final MouseEvent mouseEvent)
                        {
                        
                           final Point maybeNewPoint = mouseEvent.getPoint();
                        
                           performMove.accept(maybeNewPoint);
                        
                        }
                     
                     }
                  )
                  ;
            
               KEYBOARD_CONTROLS_FOR_DRAWING_AREA:
               {
               
                  final InputMap inputMap = drawingPanel.getInputMap(JComponent.WHEN_FOCUSED);
                  final ActionMap actionMap = drawingPanel.getActionMap();
               
                  final BiConsumer<KeyStroke, Action> keyboardControls =
                     (keyStroke, action) ->
                     {
                     
                        Objects.requireNonNull(keyStroke);
                        Objects.requireNonNull(action);
                     
                        inputMap.put(keyStroke, keyStroke);
                        actionMap.put(keyStroke, action);
                     
                     }
                     ;
               
                  final Function<Boolean, Action> colorAction =
                     coloring ->
                     {
                     
                        return
                           new AbstractAction()
                           {
                           
                              @Override
                              public void actionPerformed(final ActionEvent actionEvent)
                              {
                              
                                 gui.coloring = coloring;
                                 
                                 performClick.accept(gui.mouseCurrentLocation);
                              
                              }
                           
                           }
                           ;
                     
                     }
                        ;
               
                  final Function<Consumer<Point>, Action> movementAction =
                     pointModifier ->
                     {
                     
                        return
                           new AbstractAction()
                           {
                           
                              @Override
                              public void actionPerformed(final ActionEvent actionEvent)
                              {
                              
                                 final Point somePoint = new Point(mouseCurrentLocation.x, mouseCurrentLocation.y);
                              
                                 pointModifier.accept(somePoint);
                              
                                 performMove.accept(somePoint);
                              
                                 if (coloring)
                                 {
                                 
                                    performClick.accept(somePoint);
                                 
                                 }
                              
                                 REPAINT_DRAWING_PANEL.run();
                              
                              }
                           
                           }
                           ;
                     
                     }
                     ;
               
                  keyboardControls.accept(UP,            movementAction.apply(p -> p.translate( 0, -jumpDistance)));
                  keyboardControls.accept(DOWN,          movementAction.apply(p -> p.translate( 0, +jumpDistance)));
                  keyboardControls.accept(LEFT,          movementAction.apply(p -> p.translate(-jumpDistance,  0)));
                  keyboardControls.accept(RIGHT,         movementAction.apply(p -> p.translate(+jumpDistance,  0)));
                  keyboardControls.accept(SPACE_PRESS,   colorAction.apply(true));
                  keyboardControls.accept(SPACE_RELEASE, colorAction.apply(false));
               
               }
            
               drawingPanel
                  .addMouseListener
                  (
                     new MouseAdapter()
                     {
                     
                        @Override
                        public void mouseClicked(final MouseEvent mouseEvent)
                        {
                        
                           thingToFocus.requestFocus();
                        
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
               
                  this.mouseCurrentLocation.setLocation(new Point(0, 0));
               
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
      
         final JCheckBox hasGridLinesCheckBox;
      
         HAS_GRID_LINES_CHECK_BOX:
         {
         
            hasGridLinesCheckBox = new JCheckBox();
            hasGridLinesCheckBox.setText("Activate Grid Lines");
            hasGridLinesCheckBox.setSelected(true);
            hasGridLinesCheckBox
               .addActionListener
               (
                  event ->
                  {
                  
                     this.hasGridLines = hasGridLinesCheckBox.isSelected();
                  
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
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(hasGridLinesCheckBox);
         drawingSettingsPanel.add(Box.createHorizontalGlue());
      
      }
   
      mainPanel.add(drawingSettingsPanel, BorderLayout.NORTH);
      mainPanel.add(new JScrollPane(drawingPanel), BorderLayout.CENTER);
   
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
            )
            ;
   
      penSizeDropDownMenu
         .addActionListener
         (
            event ->
            {
            
               this.penSize =
                  penSizeDropDownMenu
                     .getItemAt(penSizeDropDownMenu.getSelectedIndex());
            
               this.mouseCurrentLocation.setLocation(new Point(0, 0));
            
               this.frame.repaint();
            
            }
         );
   
      panel.add(penSizeDropDownMenu);
   
      return panel;
   
   }

   private JPanel createColorChooserPanel()
   {
   
      final String DIALOG_TITLE = "Choose a color";
   
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
   
      panel.setBorder(TITLED_BORDER.apply("Color"));
   
      final JColorChooser colorChooser = new JColorChooser();
      colorChooser.setPreviewPanel(new JPanel());
      colorChooser
         .getSelectionModel()
         .addChangeListener
         (
            event ->
            {
            
               final Color chosenColor = colorChooser.getSelectionModel().getSelectedColor();
            
               this.cursorColor = chosenColor == null ? this.transparencyColor : chosenColor;
            
               this.frame.repaint();
            
            }
         )
         ;
   
      panel.add(colorChooser);
   
      return panel;
   
   }

   private JPanel createBottomPanel(final List<Color> pixels, final int maxRows, final int maxColumns)
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
                     
                        public static Pixel of(final int index, final Color color, final int maxRows, final int maxColumns)
                        {
                        
                           Objects.requireNonNull(color);
                        
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
                                          .mapToObj(eachInt -> Pixel.of(eachInt, pixels.get(eachInt), maxRows, maxColumns))
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
                           null,
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
