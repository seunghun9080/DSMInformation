package org.thinkinggms.utils;

import com.google.gson.JsonArray;
import org.thinkinggms.DSMInformation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

public class ImageUtils {
    public static BufferedImage renderTimeTable(JsonArray timeTable, int grade, int classNum) {
        URL url = DSMInformation.class.getClassLoader().getResource("timeTable.png");
        if (url == null) return null;
        try {
            BufferedImage image = ImageIO.read(url);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Cafe24 Ssurround BOLD", Font.BOLD, 80));
            g.drawString(grade + "-" + classNum, 84, 118);
            g.setFont(new Font("Cafe24 Ssurround BOLD", Font.BOLD, 26));
            for (int i = 0; i < timeTable.size(); i++) {
                for (int j = 0; j < timeTable.get(i).getAsJsonArray().size(); j++) {
                    var o = timeTable.get(i).getAsJsonArray().get(j).getAsJsonObject();
                    if (o.get("edited").getAsBoolean()) {
                        g.setColor(Color.RED);
                        g.drawString(o.get("subject").getAsString() + "(" + o.get("teacher").getAsString() + ")", 267 + (j * 189), 350 + (i * 107));
                        g.setFont(new Font("Cafe24 Ssurround BOLD", Font.BOLD, 15));
                        g.drawString("변경", 267 + (j * 189), 371 + (i * 107));
                        g.setFont(new Font("Cafe24 Ssurround BOLD", Font.BOLD, 26));
                    } else {
                        g.setColor(Color.BLACK);
                        g.drawString(o.get("subject").getAsString() + "(" + o.get("teacher").getAsString() + ")", 267 + (j * 189), 350 + (i * 107));
                    }
                }
            }
            return image;
        } catch (IOException e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    public static InputStream bufferedImageToInputStream(BufferedImage buffImage) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(buffImage, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }
}
