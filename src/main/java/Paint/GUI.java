
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

public class GUI
{

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
   private final JScrollPane drawingAreaScrollPane = new JScrollPane();

   private BufferedImage image;
   private Color transparencyColor = Color.WHITE;
   private Color cursorColor = Color.BLACK;
   private Color gridLinesColor = Color.GRAY;
   private boolean hasGridLines = true;
   private int penSize = 1;
   private int screenToImagePixelRatio = 10;

   public static void main(final String[] args)
   {
   
      final BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
   
      IntStream
         .range(0, 20)
         .forEach(i -> image.setRGB(i, i, Color.RED.getRGB()))
         ;
   
      new GUI(image);
   
   }

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
      
      }
   
      this.frame = new JFrame();
   
      this.frame.setTitle("Paint");
      this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   
      this.frame.add(this.createMainPanel());
   
      this.frame.pack();
      this.frame.setLocationByPlatform(true);
      this.frame.setVisible(true);
   
   }

   private JPanel createMainPanel()
   {
   
      final JPanel mainPanel = new JPanel(new BorderLayout());
   
      mainPanel.add(this.createCenterPanel(),   BorderLayout.CENTER);
   
      return mainPanel;
   
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
                        
                           final Rectangle rectangle  = gui.drawingAreaScrollPane.getViewport().getViewRect();
                        
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
                           
                              System.out.println(zoomedInImageX + " -- " + zoomedInImageY + " -- " + zoomedInImageWidth + " -- " + zoomedInImageHeight + " ---- " + originalImageX + " -- " + originalImageY + " -- " + originalImageWidth + " -- " + originalImageHeight + " ----- " + rectangle + " - " + drawingArea);
                           
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
                     
                        // gui.drawingAreaScrollPane.repaint();
                        // gui.drawingAreaScrollPane.revalidate();
                     
                     }
                  
                  }
                  ;
            
               box.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
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
                  
                     RECREATE_DRAWING_AREA_FRESH.run();
                  
                  }
               );
         
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
         
         final JButton repaint = new JButton("REPAINT");
         repaint.addActionListener(event -> REPAINT_DRAWING_PANEL.run());
      
         drawingSettingsPanel.add(Box.createHorizontalGlue());
         drawingSettingsPanel.add(screenToImagePixelRatioDropDownMenu);
         drawingSettingsPanel.add(new JLabel("SCREEN pixels = 1 IMAGE pixel"));
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(hasGridLinesCheckBox);
         drawingSettingsPanel.add(Box.createHorizontalStrut(10));
         drawingSettingsPanel.add(repaint);
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
