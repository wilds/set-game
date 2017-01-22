package com.tom_e_white.set_game;

import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import com.tom_e_white.set_game.image.ImageProcessingPipeline;
import com.tom_e_white.set_game.image.Shape;
import georegression.metric.Intersection2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Count the number of shapes on a card.
 */
public class FindCardNumberFeatures {

  public int scan(String filename, boolean debug) throws IOException {
    // Based on code from http://boofcv.org/index.php?title=Example_Binary_Image

    BufferedImage image = UtilImageIO.loadImage(filename);

    ListDisplayPanel panel = debug ? new ListDisplayPanel() : null;

    List<Shape> shapes = ImageProcessingPipeline.fromBufferedImage(image, panel)
            .gray()
            .medianBlur(3) // this is fairly critical
            .edges()
            .dilate()
            .contours()
            .polygons(0.05, 0.05)
            .getExternalShapes();

    int expectedWidth = 40 * 3; // 40mm
    int expectedHeight = 20 * 3; // 20mm
    int tolerancePct = 40;
    List<Shape> filtered = shapes.stream()
            .filter(shape -> {
              RectangleLength2D_F32 b = shape.getBoundingBox();
//              System.out.println(Math.abs(b.getWidth() - expectedWidth) / expectedWidth);
//              System.out.println(Math.abs(b.getHeight() - expectedHeight) / expectedHeight);
              return Math.abs(b.getWidth() - expectedWidth) / expectedWidth <= tolerancePct / 100.0
                      && Math.abs(b.getHeight() - expectedHeight) / expectedHeight <= tolerancePct / 100.0;
            })
            .collect(Collectors.toList());
    List<Shape> nonOverlapping = new ArrayList<>(filtered);
    for (int i = 0; i < filtered.size(); i++) {
      for (int j = 0; j < i; j++) {
        RectangleLength2D_F32 rect1 = shapes.get(i).getBoundingBox();
        RectangleLength2D_F32 rect2 = shapes.get(j).getBoundingBox();
        if (Intersection2D_F32.intersection(rect1, rect2) != null) {
          nonOverlapping.remove(shapes.get(j));
        }
      }
    }

    if (debug) {
      System.out.println(shapes);
      System.out.println(filtered);
      System.out.println(nonOverlapping);
      ShowImages.showWindow(panel, "Binary Operations", true);
    }
    return nonOverlapping.size();

  }

  public static void main(String[] args) throws IOException {
    new FindCardNumberFeatures().scan(args[0], true);
  }
}
