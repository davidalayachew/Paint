
package Paint;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

public class DarkMetalHighContrast
   extends DefaultMetalTheme
{

   private static final ColorUIResource BLACK =       color(255, 255, 255);
   private static final ColorUIResource WHITE =       color(0, 0, 0);
   private static final ColorUIResource DARK_GRAY =   color(80, 80, 80);
   private static final ColorUIResource DARK_BLUE =   color(80, 90, 255);
   private static final ColorUIResource RED =         color(255, 0, 0);

   private static ColorUIResource color(final int r, final int g, final int b)
   {
   
      return new ColorUIResource(r, g, b);
   
   }

   @Override
   protected ColorUIResource getBlack()
   {
   
      return BLACK;
   
   }

   @Override
   protected ColorUIResource getWhite()
   {
   
      return WHITE;
   
   }

   @Override
   protected ColorUIResource getPrimary1()
   {
   
      return BLACK;
   
   }

   @Override
   protected ColorUIResource getPrimary2()
   {
   
      return WHITE;
   
   }

   @Override
   protected ColorUIResource getPrimary3()
   {
   
      return DARK_BLUE;
   
   }

   @Override
   protected ColorUIResource getSecondary1()
   {
   
      return BLACK;
   
   }

   @Override
   protected ColorUIResource getSecondary2()
   {
   
      return DARK_GRAY;
   
   }

   @Override
   protected ColorUIResource getSecondary3()
   {
   
      return WHITE;
   
   }

   @Override
   public ColorUIResource getFocusColor()
   {
   
      return RED;
   
   }

   @Override
   public ColorUIResource getMenuSelectedBackground()
   {
   
      return DARK_BLUE;
   
   }

   @Override
   public ColorUIResource getPrimaryControlShadow()
   {
   
      return WHITE;
   
   }

}
