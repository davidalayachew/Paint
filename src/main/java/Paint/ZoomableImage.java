
package Paint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

import static java.awt.Color.*;

public record ZoomableImage(BufferedImage originalImage, int screenToImagePixelRatio, BufferedImage zoomedInImage)
{

   public ZoomableImage
   {
   
      Objects.requireNonNull(originalImage);
      Objects.requireNonNull(zoomedInImage);
   
   }

   public static ZoomableImage of(final BufferedImage originalImage, final int ratio)
   {
   
      Objects.requireNonNull(originalImage);
   
      final int originalImageWidth     = originalImage.getWidth();
      final int originalImageHeight    = originalImage.getHeight();
   
      final int zoomedInImageWidth     = originalImageWidth * ratio;
      final int zoomedInImageHeight    = originalImageHeight * ratio;
   
      final BufferedImage zoomedInImage =
         new BufferedImage
         (
            zoomedInImageWidth,
            zoomedInImageHeight,
            BufferedImage.TYPE_INT_ARGB
         )
         ;
   
      rowLoop:
      for (int row = 0; row < originalImageHeight; row++)
      {
      
         columnLoop:
         for (int column = 0; column < originalImageWidth; column++)
         {
         
            final int rgba = originalImage.getRGB(column, row);
         
            final int[] zoomedInPixels = new int[ratio * ratio];
         
            Arrays.fill(zoomedInPixels, rgba);
         
            zoomedInImage
               .setRGB
               (
                  column * ratio,
                  row * ratio,
                  ratio,
                  ratio,
                  zoomedInPixels,
                  0,
                  1
               )
               ;
         
         }
      
      }
   
      return new ZoomableImage(originalImage, ratio, zoomedInImage);
   
   }

   public ZoomableImage resample(final int newRatio)
   {
   
      return ZoomableImage.of(this.originalImage, newRatio);
   
   }

   public void setRGB(final Point point, final int width, final int height, final Color color)
   {
   
      Objects.requireNonNull(point);
   
      this.setRGB(point.x, point.y, width, height, color);
   
   }

   public void setRGB(final int column, final int row, final int width, final int height, final Color color)
   {
   
      Objects.requireNonNull(color);
   
      final int ratio = this.screenToImagePixelRatio;
   
      final int rgba = color.getRGB();
   
      final int[] originalPixels = new int[width * height];
   
      Arrays.fill(originalPixels, rgba);
   
      System.out.println(column + " -- " + row);
   
      this
         .originalImage
         .setRGB
         (
            column,
            row,
            Math.min(width, this.originalImage.getWidth() - column),
            Math.min(height, this.originalImage.getHeight() - row),
            originalPixels,
            0,
            1
         )
         ;
   
      final int[] zoomedInPixels = new int[ratio * width * ratio * height];
   
      Arrays.fill(zoomedInPixels, rgba);
   
      this
         .zoomedInImage
         .setRGB
         (
            column   * ratio,
            row      * ratio,
            Math.min(width * ratio, (this.originalImage.getWidth() * ratio) - (column * ratio)),
            Math.min(height * ratio, (this.originalImage.getHeight() * ratio) - (row * ratio)),
            zoomedInPixels,
            0,
            1
         )
         ;
   
   }

   public BufferedImage getSubimage(final Rectangle rectangle, final Dimension drawingArea)
   {
   
      Objects.requireNonNull(rectangle);
      Objects.requireNonNull(drawingArea);
      
      final int x = rectangle.x;
      final int y = rectangle.y;
      final int width = Math.min(rectangle.width, drawingArea.width);
      final int height = Math.min(rectangle.height, drawingArea.height);
   
      System.out.println(drawingArea);
   
      return this.zoomedInImage.getSubimage(x, y, width, height);
   
   }

   public static void main(final String[] args)
   {
   
      final BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
   
      final ZoomableImage zoomableImage = ZoomableImage.of(image, 4);
   
      zoomableImage.setRGB(0, 0, 10, 10, RED);
      zoomableImage.setRGB(0, 10, 10, 10, ORANGE);
      zoomableImage.setRGB(10, 0, 10, 10, YELLOW);
      zoomableImage.setRGB(10, 10, 10, 10, GREEN);
   
      zoomableImage.setRGB(0, 0, 10, 10, BLUE);
      zoomableImage.setRGB(0, 10, 10, 10, MAGENTA);
      zoomableImage.setRGB(10, 0, 10, 10, WHITE);
      zoomableImage.setRGB(10, 10, 10, 10, BLACK);
   
   }

}
