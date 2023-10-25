
package Paint;

import javax.imageio.*;
import javax.imageio.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.colorchooser.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import static Paint.KeyStrokes.*;

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

   private enum DrawingMode
   {
   
      KEYBOARD,
      MOUSE,
      ;
   
   }

   private enum MouseDrawingMode
   {
   
      COLORING,
      ERASING,
      ;
   
   }

   private enum KeyDrawingMode
   {
   
      COLORING,
      ERASING,
      NONE,
      ;
   
   }

   enum ImageType
   {
   
      PNG,
      GIF,
      ;
   
   }

   private static final ExecutorService THREADER =
      Executors
         .newWorkStealingPool()
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

   private static final Color CLEAR = new Color(0, 0, 0, 0);
   private static final Point OFF_SCREEN = new Point(-1, -1);

   public static final int ARBITRARY_VIEW_BUFFER = 200;
   private static final int MIN_PEN_SIZE = 1;
   private static final int MAX_PEN_SIZE = 10;
   private static final int MIN_SCREEN_TO_IMAGE_PIXEL_RATIO = 1;
   private static final int MAX_SCREEN_TO_IMAGE_PIXEL_RATIO = 30;
   private static final int DEFAULT_IMAGE_PIXEL_ROWS = 26;
   private static final int DEFAULT_IMAGE_PIXEL_COLUMNS = 24;

   private final JFrame frame;
   private final JFileChooser fileChooser;
   private final JScrollPane drawingAreaScrollPane = new JScrollPane();

   private BufferedImage image;
   private Color transparencyColor = Color.WHITE;
   private Color cursorColor = Color.BLACK;
   private Color gridLinesColor = Color.GRAY;
   private Point cursorCurrentLocation = new Point(0, 0);
   private Point cursorPreviousLocation = new Point(0, 0);
   private MouseDrawingMode mouseDrawingMode = MouseDrawingMode.COLORING;
   private KeyDrawingMode keyDrawingMode = KeyDrawingMode.NONE;
   private boolean hasGridLines = true;
   private int penSize = 1;
   private int screenToImagePixelRatio = 10;

   public GUI()
   {
   
      this(DEFAULT_IMAGE_PIXEL_ROWS, DEFAULT_IMAGE_PIXEL_COLUMNS);
   
   }

   public GUI(final int numImagePixelRows, final int numImagePixelColumns)
   {
   
      this(new BufferedImage(numImagePixelColumns, numImagePixelRows, BufferedImage.TYPE_INT_ARGB));
   
   }

   public GUI(final BufferedImage image)
   {
   
      Objects.requireNonNull(image);
   
      try
      {
      
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      
      }
      
      catch (Exception e)
      {
      
         throw new RuntimeException(e);
      
      }
   
      INITALIZING_METADATA_INSTANCE_FIELDS:
      {
      
         this.image = image;
      
         this.fileChooser = new JFileChooser();
      
         Arrays
            .stream(ImageType.values())
            .map(ImageType::name)
            .map(name -> new FileNameExtensionFilter(name, name.toLowerCase()))
            .forEach(fileChooser::addChoosableFileFilter)
            ;
      
         fileChooser.setFileHidingEnabled(false);
         fileChooser.setAcceptAllFileFilterUsed(false);
      
      }
   
      this.frame = new JFrame();
   
      this.frame.setTitle("Paint");
      this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   
      this.frame.add(this.createMainPanel());
   
      final JMenuBar menuBar = new JMenuBar();
   
      final JMenu file = new JMenu("File");
   
      final JMenuItem save = this.createSaveMenuItem();
   
      file.add(save);
   
      menuBar.add(file);
   
      this.frame.setJMenuBar(menuBar);
   
      this.frame.pack();
      this.frame.setLocationByPlatform(true);
      this.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      this.frame.setVisible(true);
   
   }

   private Point generateCursorStartingPoint()
   {
   
      final int offset =
         (
            this.screenToImagePixelRatio * this.penSize / 2
         )
         -
         (
            (
               this.penSize % 2
            )
            *
            (
               this.screenToImagePixelRatio / 2
            )
         )
         ;
   
      return new Point(offset, offset);
   
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
   
      final GUI gui = this; //useful when trying to differentiate between different this'.
   
      final JPanel mainPanel;
   
      CREATE_MAIN_PANEL:
      {
      
         mainPanel = new JPanel();
         mainPanel.setLayout(new BorderLayout());
      
      }
   
      final JPanel drawingPanel;
   
      final Runnable UPDATE_DRAWING_PANEL_BORDER_TEXT;
      final Runnable RECREATE_DRAWING_AREA_FRESH;
      final Runnable REPAINT_DRAWING_PANEL;
   
      final BiFunction<Point, Integer, Point> originalToZoomed =
         (original, ratio) ->
            new Point(original.x * ratio, original.y * ratio)
            ;
   
      final BiFunction<Point, Integer, Point> zoomedToOriginal =
         (zoomed, ratio) ->
            new Point
            (
               (zoomed.x - (zoomed.x % ratio)) / ratio,
               (zoomed.y - (zoomed.y % ratio)) / ratio
            )
            ;
   
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
      
         RECREATE_DRAWING_AREA_FRESH =
            () ->
            {
            
               final var thingToFocus = drawingPanel;
            
               drawingPanel.removeAll();
            
               final Dimension drawingArea = gui.deriveDrawingAreaDimensions();
            
               final IntUnaryOperator quantize =
                  num ->
                     num - (num % gui.screenToImagePixelRatio)
                     ;
            
               final Box.Filler box =
                  new Box.Filler(drawingArea, drawingArea, drawingArea)
                  {
                  
                     @Override
                     protected void paintComponent(final Graphics dontUse)
                     {
                     
                        super.paintComponent(dontUse);
                     
                        if (!(dontUse instanceof Graphics2D g))
                        {
                        
                           throw new RuntimeException("Unknown graphics type = " + dontUse);
                        
                        }
                     
                        DRAW_DRAWN_PIXELS:
                        {
                        
                           final Rectangle rectangle  = g.getClipBounds();
                        
                           //We are only drawing a subsection because we may be working with GIGANTIC images.
                           //If we attempt to draw the whole image, performance will drop like a rock.
                           CALCULATE_SUBSECTION_TO_DRAW:
                           {
                           
                              final int x = rectangle.x;
                              final int y = rectangle.y;
                              final int width = Math.min(rectangle.width, drawingArea.width);
                              final int height = Math.min(rectangle.height, drawingArea.height);
                           
                              g.setBackground(gui.transparencyColor);
                              g.clearRect(rectangle.x, rectangle.y, width, height);
                           
                           }
                        
                           DRAW_SUBSECTION_OF_IMAGE:
                           {
                           
                              final int originalImageX;
                              final int zoomedInImageX;
                              final int originalImageY;
                              final int zoomedInImageY;
                           
                              CALCULATE_ORIGINAL_POSITION:
                              {
                              
                                 final int quantizedX = quantize.applyAsInt(rectangle.x);
                                 final int quantizedY = quantize.applyAsInt(rectangle.y);
                              
                                 zoomedInImageX = Math.max(quantizedX, 0);
                                 originalImageX = zoomedInImageX / gui.screenToImagePixelRatio;
                              
                                 zoomedInImageY = Math.max(quantizedY, 0);
                                 originalImageY = zoomedInImageY / gui.screenToImagePixelRatio;
                              
                              }
                           
                              final int zoomedInImageWidth;
                              final int originalImageWidth;
                              final int zoomedInImageHeight;
                              final int originalImageHeight;
                           
                              CALCULATE_ORIGINAL_DIMENSION:
                              {
                              
                                 final int minWidth = Math.min(rectangle.width, drawingArea.width);
                                 final int quantizedMinWidth = quantize.applyAsInt(minWidth);
                                 final int potentialWidth = quantizedMinWidth + gui.screenToImagePixelRatio;
                                 zoomedInImageWidth = Math.min(drawingArea.width, potentialWidth);
                                 originalImageWidth = zoomedInImageWidth / gui.screenToImagePixelRatio;
                              
                                 final int minHeight = Math.min(rectangle.height, drawingArea.height);
                                 final int quantizedMinHeight = quantize.applyAsInt(minHeight);
                                 final int potentialHeight = quantizedMinHeight + gui.screenToImagePixelRatio;
                                 zoomedInImageHeight = Math.min(drawingArea.height, potentialHeight);
                                 originalImageHeight = zoomedInImageHeight / gui.screenToImagePixelRatio;
                              
                              }
                           
                              g.setPaint(gui.cursorColor);
                           
                              // System.out.println(zoomedInImageX + " -- " + zoomedInImageY + " -- " + zoomedInImageWidth + " -- " + zoomedInImageHeight + " ---- " + originalImageX + " -- " + originalImageY + " -- " + originalImageWidth + " -- " + originalImageHeight + " ----- " + rectangle + " - " + drawingArea);
                           
                              g
                                 .drawImage
                                 (
                                    gui.image,
                                    zoomedInImageX,
                                    zoomedInImageY,
                                    zoomedInImageX + zoomedInImageWidth,
                                    zoomedInImageY + zoomedInImageHeight,
                                    originalImageX,
                                    originalImageY,
                                    originalImageX + originalImageWidth,
                                    originalImageY + originalImageHeight,
                                    //gui.transparencyColor,
                                    null
                                 )
                                 ;
                           
                           }
                        
                           DRAW_CURSOR_IF_IN_SUBSECTION:
                           {
                           
                              if (gui.cursorCurrentLocation.getLocation().equals(OFF_SCREEN))
                              {
                              
                                 break DRAW_CURSOR_IF_IN_SUBSECTION;
                              
                              }
                           
                              final int screenPixelCursorSize = gui.screenToImagePixelRatio * gui.penSize;
                           
                              if (rectangle.contains(gui.cursorCurrentLocation))
                              {
                              
                                 g
                                    .setPaint
                                    (
                                       switch (gui.keyDrawingMode)
                                       {
                                       
                                          case  COLORING -> gui.cursorColor;
                                          case  ERASING  -> gui.transparencyColor;
                                          case  NONE     ->
                                                switch (gui.mouseDrawingMode)
                                                {
                                                
                                                   case  COLORING -> gui.cursorColor;
                                                   case  ERASING  -> gui.transparencyColor;
                                                
                                                }
                                                ;
                                       
                                       }
                                    )
                                    ;
                              
                                 g.setStroke(new java.awt.BasicStroke(screenPixelCursorSize));
                              
                                 final int transform = gui.screenToImagePixelRatio / 2;
                              
                                 final int evenPush = ((gui.penSize - 1) % 2) * (gui.screenToImagePixelRatio / 2);
                              
                                 var asd = gui.cursorCurrentLocation.x + transform - evenPush;
                                 var ert = gui.cursorCurrentLocation.y + transform - evenPush;
                              
                                 g
                                    .drawLine
                                    (
                                       asd,
                                       ert,
                                       asd,
                                       ert
                                    )
                                    ;
                              
                              }
                           
                           }
                        
                           DRAW_GRID_LINES:
                           {
                           
                              if (gui.hasGridLines && gui.screenToImagePixelRatio > 1)
                              {
                              
                                 g.setPaint(gui.gridLinesColor);
                                 g.setStroke(new java.awt.BasicStroke(1));
                              
                                 IntStream
                                    .range(rectangle.y, rectangle.y + rectangle.height)
                                    .forEach
                                    (
                                       eachIndex ->
                                       {
                                       
                                          if (eachIndex % gui.screenToImagePixelRatio == 0)
                                          {
                                          
                                             g
                                                .drawLine
                                                (
                                                   rectangle.x,
                                                   eachIndex,
                                                   rectangle.x + rectangle.width,
                                                   eachIndex
                                                )
                                                ;
                                          
                                          }
                                       
                                       }
                                    )
                                    ;
                              
                                 IntStream
                                    .rangeClosed(rectangle.x, rectangle.x + rectangle.width)
                                    .forEach
                                    (
                                       eachIndex ->
                                       {
                                       
                                          if (eachIndex % gui.screenToImagePixelRatio == 0)
                                          {
                                          
                                             g
                                                .drawLine
                                                (
                                                   eachIndex,
                                                   rectangle.y,
                                                   eachIndex,
                                                   rectangle.y + rectangle.height
                                                )
                                                ;
                                          
                                          }
                                       
                                       }
                                    )
                                    ;
                              
                              }
                           
                           }
                        
                        }
                     
                     }
                  
                  }
                  ;
            
               box.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
               record ClickMetaData(DrawingMode drawingMode, boolean dragging) {}
            
               final BiConsumer<Point, ClickMetaData> performClick =
                  (maybeNewPoint, clickMetaData) ->
                  {
                  
                     final int zoomedInImageX = quantize.applyAsInt(maybeNewPoint.x);
                     final int zoomedInImageY = quantize.applyAsInt(maybeNewPoint.y);
                  
                     final int originalImageX = zoomedInImageX / gui.screenToImagePixelRatio;
                     final int originalImageY = zoomedInImageY / gui.screenToImagePixelRatio;
                  
                     gui.cursorPreviousLocation.setLocation(gui.cursorCurrentLocation);
                  
                     gui.cursorCurrentLocation.setLocation(zoomedInImageX, zoomedInImageY);
                  
                     final Graphics2D graphics = gui.image.createGraphics();
                  
                     record GraphicsMetadata(Color someColor, AlphaComposite alphaComposite)
                     {
                     
                        GraphicsMetadata
                        {
                        
                           Objects.requireNonNull(someColor);
                           Objects.requireNonNull(alphaComposite);
                        
                        }
                     
                     
                     
                     }
                  
                     final GraphicsMetadata graphicsMetadata =
                        switch (clickMetaData.drawingMode())
                        {
                        
                           case  MOUSE    ->
                                 switch (gui.mouseDrawingMode)
                                 {
                                 
                                    case COLORING  -> new GraphicsMetadata(gui.cursorColor, AlphaComposite.SrcOver);
                                    case ERASING   -> new GraphicsMetadata(CLEAR, AlphaComposite.SrcIn);
                                 
                                 }
                                 ;
                           case  KEYBOARD ->
                                 switch (gui.keyDrawingMode)
                                 {
                                 
                                    case  COLORING -> new GraphicsMetadata(gui.cursorColor, AlphaComposite.SrcOver);
                                    case  ERASING  -> new GraphicsMetadata(CLEAR, AlphaComposite.SrcIn);
                                    case  NONE     -> throw new IllegalArgumentException();
                                 
                                 }
                                 ;
                        
                        }
                        ;
                  
                     graphics.setPaint(graphicsMetadata.someColor());
                     graphics.setComposite(graphicsMetadata.alphaComposite());
                  
                     graphics.setStroke(new java.awt.BasicStroke(gui.penSize));
                  
                     if
                     (
                        clickMetaData.dragging()
                        && !gui.cursorPreviousLocation.equals(OFF_SCREEN)
                        && !gui.cursorPreviousLocation.equals(gui.cursorCurrentLocation)
                     )
                     {
                     
                        final var first = zoomedToOriginal.apply(gui.cursorPreviousLocation, gui.screenToImagePixelRatio);
                        final var second = zoomedToOriginal.apply(gui.cursorCurrentLocation, gui.screenToImagePixelRatio);
                     
                        graphics
                           .drawLine
                           (
                              first.x,
                              first.y,
                              second.x,
                              second.y
                           )
                           ;
                     
                     }
                     
                     else
                     {
                     
                        graphics.drawLine(originalImageX, originalImageY, originalImageX, originalImageY);
                     
                     }
                  
                     REPAINT_DRAWING_PANEL.run();
                  
                  }
                  ;
            
               final BiConsumer<Point, DrawingMode> performMove =
                  (maybeNewPoint, drawingMode) ->
                  {
                  
                     final int x = maybeNewPoint.x - (maybeNewPoint.x % gui.screenToImagePixelRatio);
                     final int y = maybeNewPoint.y - (maybeNewPoint.y % gui.screenToImagePixelRatio);
                  
                     maybeNewPoint.setLocation(x, y);
                  
                     if (!gui.cursorCurrentLocation.equals(maybeNewPoint))
                     {
                     
                        gui.cursorPreviousLocation.setLocation(gui.cursorCurrentLocation);
                     
                        gui.cursorCurrentLocation.setLocation(maybeNewPoint);
                     
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
                        public void mouseExited(final MouseEvent mouseEvent)
                        {
                        
                           System.out.println("exited");
                           //gui.cursorCurrentLocation.setLocation(OFF_SCREEN);
                        
                           REPAINT_DRAWING_PANEL.run();
                        
                        }
                     
                        @Override
                        public void mousePressed(final MouseEvent mouseEvent)
                        {
                        
                           if (SwingUtilities.isLeftMouseButton(mouseEvent))
                           {
                           
                              performClick.accept(mouseEvent.getPoint(), new ClickMetaData(DrawingMode.MOUSE, false));
                           
                              thingToFocus.requestFocus();
                           
                           }
                        
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
                        
                           print(mouseEvent.getPoint().toString());
                        
                           performClick.accept(mouseEvent.getPoint(), new ClickMetaData(DrawingMode.MOUSE, true));
                        
                        }
                     
                        @Override
                        public void mouseMoved(final MouseEvent mouseEvent)
                        {
                        
                           performMove.accept(mouseEvent.getPoint(), DrawingMode.MOUSE);
                        
                        }
                     
                     }
                  )
                  ;
            
               KEYBOARD_CONTROLS_FOR_DRAWING_AREA:
               {
               
                  final InputMap inputMap = drawingPanel.getInputMap(JComponent.WHEN_FOCUSED);
                  final ActionMap actionMap = drawingPanel.getActionMap();
               
                  final BiConsumer<List<KeyStroke>, Action> keyboardControls =
                     (keyStrokes, action) ->
                     {
                     
                        Objects.requireNonNull(keyStrokes);
                        Objects.requireNonNull(action);
                     
                        keyStrokes
                           .forEach
                           (
                              keyStroke ->
                              {
                              
                                 inputMap.put(keyStroke, keyStroke);
                                 actionMap.put(keyStroke, action);
                              
                              }
                           )
                           ;
                     
                     }
                     ;
               
                  final Consumer<KeyDrawingMode> press =
                     keyDrawingMode ->
                     {
                     
                        gui.keyDrawingMode = keyDrawingMode;
                     
                        switch (keyDrawingMode)
                        {
                        
                           case  NONE        -> REPAINT_DRAWING_PANEL.run();
                           case  COLORING,
                                 ERASING     ->
                           {
                           
                              performClick.accept(gui.cursorCurrentLocation, new ClickMetaData(DrawingMode.KEYBOARD, false));
                           
                           }
                        
                        }
                     
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
                              
                                 press.accept(coloring ? KeyDrawingMode.COLORING : KeyDrawingMode.NONE);
                              
                              }
                           
                           }
                           ;
                     
                     }
                     ;
               
                  final Function<Boolean, Action> eraserAction =
                     erasing ->
                     {
                     
                        return
                           new AbstractAction()
                           {
                           
                              @Override
                              public void actionPerformed(final ActionEvent actionEvent)
                              {
                              
                                 press.accept(erasing ? KeyDrawingMode.ERASING : KeyDrawingMode.NONE);
                              
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
                              
                                 final Point somePoint = new Point(cursorCurrentLocation.x, cursorCurrentLocation.y);
                              
                                 pointModifier.accept(somePoint);
                              
                                 performMove.accept(somePoint, DrawingMode.KEYBOARD);
                              
                                 switch (gui.keyDrawingMode)
                                 {
                                 
                                    case  NONE        -> {}
                                    case  COLORING,
                                          ERASING     -> performClick.accept(somePoint, new ClickMetaData(DrawingMode.KEYBOARD, false));
                                 
                                 
                                 }
                              
                                 REPAINT_DRAWING_PANEL.run();
                              
                              }
                           
                           }
                           ;
                     
                     }
                     ;
               
                  final List<KeyStroke> pressUp          = List.of(NUMPAD_UP,             UP,                  NUMPAD_8,         W  );
                  final List<KeyStroke> pressUpLeft      = List.of(HOME,                                       NUMPAD_7             );
                  final List<KeyStroke> pressLeft        = List.of(NUMPAD_LEFT,           LEFT,                NUMPAD_4,         A  );
                  final List<KeyStroke> pressDownLeft    = List.of(END,                                        NUMPAD_1             );
                  final List<KeyStroke> pressDown        = List.of(NUMPAD_DOWN,           DOWN,                NUMPAD_2,         S  );
                  final List<KeyStroke> pressDownRight   = List.of(PAGE_DOWN,                                  NUMPAD_3             );
                  final List<KeyStroke> pressRight       = List.of(NUMPAD_RIGHT,          RIGHT,               NUMPAD_6,         D  );
                  final List<KeyStroke> pressUpRight     = List.of(PAGE_UP,                                    NUMPAD_9             );
                  final List<KeyStroke> pressPen         = List.of(NUMPAD_CLEAR_PRESS,    SPACE_PRESS,         NUMPAD_5_PRESS       );
                  final List<KeyStroke> releasePen       = List.of(NUMPAD_CLEAR_RELEASE,  SPACE_RELEASE,       NUMPAD_5_RELEASE     );
                  final List<KeyStroke> pressEraser      = List.of(INSERT_PRESS,          BACKSPACE_PRESS,     NUMPAD_0_PRESS       );
                  final List<KeyStroke> releaseEraser    = List.of(INSERT_RELEASE,        BACKSPACE_RELEASE,   NUMPAD_0_RELEASE     );
               
                  final int jumpSize = this.screenToImagePixelRatio;
               
                  keyboardControls.accept(pressUp,          movementAction.apply(p -> p.translate( 0, -jumpSize)));
                  keyboardControls.accept(pressDown,        movementAction.apply(p -> p.translate( 0, +jumpSize)));
                  keyboardControls.accept(pressLeft,        movementAction.apply(p -> p.translate(-jumpSize,  0)));
                  keyboardControls.accept(pressRight,       movementAction.apply(p -> p.translate(+jumpSize,  0)));
                  keyboardControls.accept(pressUpLeft,      movementAction.apply(p -> p.translate(-jumpSize, -jumpSize)));
                  keyboardControls.accept(pressDownLeft,    movementAction.apply(p -> p.translate(-jumpSize, +jumpSize)));
                  keyboardControls.accept(pressUpRight,     movementAction.apply(p -> p.translate(+jumpSize, -jumpSize)));
                  keyboardControls.accept(pressDownRight,   movementAction.apply(p -> p.translate(+jumpSize, +jumpSize)));
                  keyboardControls.accept(pressPen,         colorAction.apply(true));
                  keyboardControls.accept(releasePen,       colorAction.apply(false));
                  keyboardControls.accept(pressEraser,      eraserAction.apply(true));
                  keyboardControls.accept(releaseEraser,    eraserAction.apply(false));
               
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
      
         RECREATE_DRAWING_AREA_FRESH.run();
      
      }
   
      final JPanel drawingSettingsPanel;
   
      CREATE_DRAWING_SETTINGS_PANEL:
      {
      
         drawingSettingsPanel = new JPanel();
         drawingSettingsPanel.setLayout(new BoxLayout(drawingSettingsPanel, BoxLayout.LINE_AXIS));
      
         final JSpinner screenToImagePixelRatioDropDownMenu;
      
         SCREEN_TO_IMAGE_PIXEL_RATIO_DROP_DOWN_MENU:
         {
         
            screenToImagePixelRatioDropDownMenu =
               new JSpinner
               (
                  new SpinnerNumberModel
                  (
                     this.screenToImagePixelRatio,
                     MIN_SCREEN_TO_IMAGE_PIXEL_RATIO,
                     MAX_SCREEN_TO_IMAGE_PIXEL_RATIO,
                     1
                  )
               )
               ;
         
            screenToImagePixelRatioDropDownMenu
               .addChangeListener
               (
                  event ->
                  {
                  
                     if (!(screenToImagePixelRatioDropDownMenu.getValue() instanceof Integer iii))
                     {
                     
                        throw new IllegalStateException();
                     
                     }
                  
                     this.screenToImagePixelRatio = iii;
                  
                     this.cursorCurrentLocation.setLocation(this.generateCursorStartingPoint());
                  
                     RECREATE_DRAWING_AREA_FRESH.run();
                  
                  }
               );
         
         }
      
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
      
         final JComboBox<MouseDrawingMode> mouseDrawingModeDropDownMenu;
      
         MOUSE_DRAWING_MODE_DROP_DOWN_MENU:
         {
         
            mouseDrawingModeDropDownMenu = new JComboBox<>(MouseDrawingMode.values());
            mouseDrawingModeDropDownMenu.setSelectedItem(gui.mouseDrawingMode);
            mouseDrawingModeDropDownMenu
               .addActionListener
               (
                  event ->
                  {
                  
                     this.mouseDrawingMode =
                        mouseDrawingModeDropDownMenu
                           .getItemAt
                           (
                              mouseDrawingModeDropDownMenu.getSelectedIndex()
                           )
                           ;
                  
                     REPAINT_DRAWING_PANEL.run();
                  
                  }
               )
               ;
         
         }
      
         final JButton openImageButton;
      
         OPEN_IMAGE_BUTTON:
         {
         
            openImageButton = new JButton();
         
            openImageButton.setText("Open Image");
         
            openImageButton
               .addActionListener
               (
                  event ->
                  {
                  
                     fileChooser.showOpenDialog(this.frame);
                  
                     if (fileChooser.getSelectedFile() instanceof final File newImageFile)
                     {
                     
                        try
                        {
                        
                           final BufferedImage newImage = ImageIO.read(newImageFile);
                        
                           new GUI(newImage);
                        
                        }
                        
                        catch (final Exception e)
                        {
                        
                           throw new RuntimeException(e);
                        
                        }
                     
                     }
                  
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
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(mouseDrawingModeDropDownMenu);
         drawingSettingsPanel.add(new JLabel("Mouse Drawing Mode"));
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(openImageButton);
         drawingSettingsPanel.add(Box.createHorizontalGlue());
      
      }
   
      CREATE_CENTERED_DRAWING_PANEL:
      {
      
         final JPanel centeredDrawingPanel = new JPanel();
         centeredDrawingPanel.setLayout(new BoxLayout(centeredDrawingPanel, BoxLayout.PAGE_AXIS));
         centeredDrawingPanel.add(Box.createVerticalGlue());
         centeredDrawingPanel.add(drawingPanel);
         centeredDrawingPanel.add(Box.createVerticalGlue());
      
         UPDATE_DRAWING_PANEL_BORDER_TEXT =
            () ->
               this.drawingAreaScrollPane
                  .setBorder
                  (
                     BorderFactory
                        .createCompoundBorder
                        (
                           TITLED_BORDER
                              .apply
                              (
                                 "Drawing Area -- "
                                 + gui.image.getHeight()
                                 + " rows and "
                                 + gui.image.getWidth()
                                 + " columns"
                              ),
                           BorderFactory.createLineBorder(Color.BLACK, 1)
                        )
                  )
                  ;
      
         UPDATE_DRAWING_PANEL_BORDER_TEXT.run();
      
         this.drawingAreaScrollPane.setViewportView(centeredDrawingPanel);
      
      }
   
      mainPanel.add(drawingSettingsPanel, BorderLayout.NORTH);
      mainPanel.add(this.drawingAreaScrollPane, BorderLayout.CENTER);
   
      return mainPanel;
   
   }

   private JPanel createPenSizePanel()
   {
   
      final JPanel panel = new JPanel();
   
      panel.setBorder(TITLED_BORDER.apply("Size"));
   
      final JSpinner penSizeDropDownMenu =
         new JSpinner
         (
            new SpinnerNumberModel
            (
               MIN_PEN_SIZE,
               MIN_PEN_SIZE,
               MAX_PEN_SIZE,
               1
            )
         )
         ;
   
      penSizeDropDownMenu
         .addChangeListener
         (
            event ->
            {
            
               if (!(penSizeDropDownMenu.getValue() instanceof Integer iii))
               {
               
                  throw new IllegalStateException();
               
               }
            
               this.penSize = iii;
            
               this.cursorCurrentLocation.setLocation(this.generateCursorStartingPoint());
            
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

   private JMenuItem createSaveMenuItem()
   {
   
      final GUI gui = this;
   
      final JMenuItem saveMenuItem = new JMenuItem("Save Image");
   
      abstract class ProgressBarTask extends SwingWorker<Void, Integer>
      {
      
         private final JProgressBar progressBar;
         private final String prefix;
      
         public ProgressBarTask(final JProgressBar progressBar, final String prefix)
         {
         
            this.progressBar = Objects.requireNonNull(progressBar);
            this.prefix = Objects.requireNonNull(prefix);
            this.publish(0);
         
         }
      
         @Override
         protected void process(final List<Integer> chunks)
         {
         
            this.progressBar.setStringPainted(true);
         
            for (final Integer each : chunks)
            {
            
               this.progressBar.setValue(each);
            
               final double percentComplete = this.progressBar.getPercentComplete() * 100;
            
               final String string = this.prefix + " --- " + percentComplete + "%";
               this.progressBar.setString(string);
            
            }
         
         }
      
      }
   
      final Color transparentColor = new Color(0, 0, 0, 0);
   
      saveMenuItem
         .addActionListener
         (
            actionEvent ->
            {
            
               final JDialog loadingScreen;
               final JProgressBar validationProgressBar;
               final JProgressBar savingImageProgressBar;
            
               CREATE_LOADING_SCREEN:
               {
               
                  loadingScreen = new JDialog(this.frame, true);
               
                  final JPanel loadingScreenPanel = new JPanel();
                  loadingScreenPanel.setLayout(new BoxLayout(loadingScreenPanel, BoxLayout.PAGE_AXIS));
               
                  validationProgressBar      = new JProgressBar(0, gui.image.getHeight() * gui.image.getWidth());
                  savingImageProgressBar     = new JProgressBar();
               
                  loadingScreenPanel.add(validationProgressBar);
                  loadingScreenPanel.add(savingImageProgressBar);
               
                  loadingScreen.add(loadingScreenPanel);
               
               }
            
               TASKS:
               {
               
                  if (this.fileChooser.showSaveDialog(loadingScreen) != JFileChooser.APPROVE_OPTION)
                  {
                  
                     return;
                  
                  }
               
                  final BufferedImage finalImage =
                     new BufferedImage
                     (
                        gui.image.getWidth(),
                        gui.image.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                     )
                     ;
               
                  final ProgressBarTask saveImageTask =
                     new ProgressBarTask(savingImageProgressBar, "Saving Image")
                     {
                     
                        @Override
                        protected Void doInBackground()
                        {
                        
                           try
                           {
                           
                              if (!(fileChooser.getFileFilter() instanceof final FileNameExtensionFilter filter))
                              {
                              
                                 throw new IllegalArgumentException("BAD TYPE");
                              
                              }
                           
                              final String fileExtension = filter.getExtensions()[0];
                           
                              final String rawFileName = Objects.requireNonNull(fileChooser.getSelectedFile()).getAbsolutePath();
                           
                              final File outputFile =
                                 new File
                                 (
                                    rawFileName.endsWith("." + fileExtension)
                                    ? rawFileName
                                    : rawFileName + "." + fileExtension
                                 )
                                 ;
                           
                              final ImageWriter imageWriter = ImageIO.getImageWritersByFormatName(fileExtension).next();
                           
                              final ProgressBarTask task = this;
                           
                              imageWriter
                                 .addIIOWriteProgressListener
                                 (
                                    new IIOWriteProgressListener()
                                    {
                                    
                                       @Override
                                       public void imageProgress(ImageWriter source, float percentageDone)
                                       {
                                       
                                          publish(Math.round(percentageDone));
                                       
                                       }
                                    
                                       @Override public void imageStarted(ImageWriter source, int imageIndex) {}
                                       @Override public void imageComplete(ImageWriter source) {}
                                       @Override public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {}
                                       @Override public void thumbnailProgress(ImageWriter source, float percentageDone) {}
                                       @Override public void thumbnailComplete(ImageWriter source) {}
                                       @Override public void writeAborted(ImageWriter source) {}
                                    
                                    }
                                 )
                                 ;
                           
                              try (var imageOutputStream = ImageIO.createImageOutputStream(outputFile))
                              {
                              
                                 imageWriter.setOutput(imageOutputStream);
                              
                                 imageWriter.write(finalImage);
                              
                                 JOptionPane.showMessageDialog(loadingScreen, outputFile.getAbsolutePath());
                              
                                 loadingScreen.dispose();
                              
                              }
                           
                           }
                           
                           catch (final Exception e)
                           {
                           
                              throw new RuntimeException(e);
                           
                           }
                        
                           return null;
                        
                        }
                     
                     }
                     ;
               
                  final ProgressBarTask validateImageTask =
                     new ProgressBarTask(validationProgressBar, "Validating Image Pixels")
                     {
                     
                        @Override
                        protected Void doInBackground()
                        {
                        
                           print("start");
                        
                           this.publish(0);
                        
                           if (!(fileChooser.getFileFilter() instanceof final FileNameExtensionFilter filter))
                           {
                           
                              throw new IllegalArgumentException("HOW");
                           
                           }
                        
                           final ImageType imageType = ImageType.valueOf(filter.getExtensions()[0].toUpperCase());
                        
                           final Predicate<Color> isOpaqueOrTransparent =
                              givenColor ->
                                 givenColor.getAlpha() == 0
                                 ||
                                 givenColor.getAlpha() == 255
                                 ;
                        
                           final boolean canSaveCorrectly =
                              switch (imageType)
                              {
                              
                                 case  GIF   ->
                                 {
                                 
                                    final int numImagePixels = gui.image.getHeight() * gui.image.getWidth();
                                 
                                    for (int i = 0; i < numImagePixels; i++)
                                    {
                                    
                                       final Color eachColor = new Color(gui.image.getRGB(i % gui.image.getWidth(), i / gui.image.getWidth()), true);
                                    
                                       if (!isOpaqueOrTransparent.test(eachColor))
                                       {
                                       
                                          yield false;
                                       
                                       }
                                    
                                       if (i % 1_000 == 0)
                                       {
                                       
                                          this.publish(i);
                                       
                                       }
                                    
                                    }
                                 
                                    yield true;
                              
                                 }
                                 case  PNG   -> true;
                              }
                              ;
                        
                           print("canSaveCorrectly = " + canSaveCorrectly);
                        
                           if (!canSaveCorrectly)
                           {
                           
                              record Pixel(int row, int column, Color color)
                              {
                              
                                 public Pixel
                                 {
                                 
                                    Objects.requireNonNull(color);
                                 
                                 }
                              
                                 public static Pixel of(final int index, final int color, final int maxRows, final int maxColumns)
                                 {
                                 
                                    return Pixel.of(index, new Color(color, true), maxRows, maxColumns);
                                 
                                 }
                              
                                 public static Pixel of(final int index, final Color color, final int maxRows, final int maxColumns)
                                 {
                                 
                                    Objects.requireNonNull(color);
                                 
                                    final int row     = index / maxColumns;
                                    final int column  = index % maxColumns;
                                 
                                    return
                                       new Pixel
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
                                    new JLabel
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
                           
                              final int pixelCount = gui.image.getHeight() * gui.image.getWidth();
                           
                              listOfBadPixels
                                 .add
                                 (
                                    new JScrollPane
                                    (
                                       new JList<String>
                                       (
                                          IntStream
                                             .range(0, pixelCount)
                                             .mapToObj(eachInt -> Pixel.of(eachInt, gui.image.getRGB(eachInt % gui.image.getWidth(), eachInt / gui.image.getWidth()), gui.image.getHeight(), gui.image.getWidth()))
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
                                 )
                                 ;
                           
                              return null;
                           
                           }
                           
                           else
                           {
                           
                              this.publish(gui.image.getHeight() * gui.image.getWidth());
                           
                           }
                        
                           print("done");
                        
                           saveImageTask.execute();
                        
                           return null;
                        
                        }
                     
                     }
                     ;
               
                  loadingScreen.setSize(300, 200);
               
                  validateImageTask.execute();
               
                  loadingScreen.setVisible(true);
               
               }
            
            }
         )
         ;
   
      return saveMenuItem;
   
   }

   private static void print(final String text)
   {
   
      System.out.println(LocalDateTime.now() + " -- " + text);
   
   }

   private Dimension deriveDrawingAreaDimensions()
   {
   
      return
         new Dimension
         (
            this.image.getWidth() * this.screenToImagePixelRatio,
            this.image.getHeight() * this.screenToImagePixelRatio
         )
         ;
   
   }

}
