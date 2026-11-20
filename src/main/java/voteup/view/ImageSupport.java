package voteup.view;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Loads post images from HTTP(S) URLs, {@code file:} URIs, local paths, or
 * classpath resources ({@code classpath:...}).
 */
final class ImageSupport {

    static final int MAX_WIDTH = 520;
    static final int MAX_HEIGHT = 320;

    private ImageSupport() {
    }

    /**
     * Builds a label showing the image, or an error hint if it cannot load.
     *
     * @param imageRef path / URL / classpath ref
     * @return label with scaled icon or muted failure text
     */
    static JLabel imageLabel(String imageRef) {
        if (imageRef == null || imageRef.isBlank()) {
            return null;
        }
        String ref = imageRef.trim();
        try {
            BufferedImage raw = readImage(ref);
            if (raw == null) {
                return failureLabel(ref, "not found");
            }
            Image scaled = scaleToFit(raw, MAX_WIDTH, MAX_HEIGHT);
            JLabel label = new JLabel(new ImageIcon(scaled));
            label.setToolTipText(ref);
            label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            return label;
        } catch (Exception ex) {
            return failureLabel(ref, ex.getMessage());
        }
    }

    private static JLabel failureLabel(String ref, String reason) {
        JLabel label = new JLabel("<html><i>[image failed: " + escape(reason) + "]</i><br/>"
                + escape(ref) + "</html>");
        label.setForeground(ViewSupport.MUTED);
        label.setFont(ViewSupport.BODY);
        label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        return label;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static BufferedImage readImage(String ref) throws Exception {
        String trimmed = ref.trim();
        if (trimmed.regionMatches(true, 0, "classpath:", 0, "classpath:".length())) {
            String resource = trimmed.substring("classpath:".length()).trim();
            if (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            try (InputStream in = ImageSupport.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    return null;
                }
                return ImageIO.read(in);
            }
        }
        if (trimmed.regionMatches(true, 0, "http://", 0, 7)
                || trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            return ImageIO.read(URI.create(trimmed).toURL());
        }
        if (trimmed.regionMatches(true, 0, "file:", 0, 5)) {
            return ImageIO.read(URI.create(trimmed).toURL());
        }
        File file = new File(trimmed);
        if (file.isFile()) {
            return ImageIO.read(file);
        }
        // Fall back to classpath without prefix (e.g. voteup/demo/sample.png)
        try (InputStream in = ImageSupport.class.getClassLoader().getResourceAsStream(trimmed)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        }
        return null;
    }

    static Image scaleToFit(BufferedImage source, int maxW, int maxH) {
        int w = source.getWidth();
        int h = source.getHeight();
        double scale = Math.min(1.0, Math.min((double) maxW / w, (double) maxH / h));
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        return source.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
    }
}
