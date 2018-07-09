package gate.resources.img.svg;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class has been automatically generated using <a
 * href="http://englishjavadrinker.blogspot.com/search/label/SVGRoundTrip">SVGRoundTrip</a>.
 */
@SuppressWarnings("unused")
public class TwitterJSONIcon implements
		javax.swing.Icon {
		
	private static Color getColor(int red, int green, int blue, int alpha, boolean disabled) {
		
		if (!disabled) return new Color(red, green, blue, alpha);
		
		int gray = (int)(((0.30f * red) + (0.59f * green) + (0.11f * blue))/3f);
		
		gray = Math.min(255, Math.max(0, gray));
		
		//This brightens the image the same as GrayFilter
		int percent = 50;		
		gray = (255 - ((255 - gray) * (100 - percent) / 100));

		return new Color(gray, gray, gray, alpha);
	}
	
	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 * 
	 * @param g
	 *            Graphics context.
	 */
	public static void paint(Graphics2D g, boolean disabled) {
        Shape shape = null;
        Paint paint = null;
        Stroke stroke = null;
        Area clip = null;
         
        float origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite = 
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }
        
	    Shape clip_ = g.getClip();
AffineTransform defaultTransform_ = g.getTransform();
//  is CompositeGraphicsNode
float alpha__0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0 = g.getClip();
AffineTransform defaultTransform__0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
clip = new Area(g.getClip());
clip.intersect(new Area(new Rectangle2D.Double(0.0,0.0,48.0003547668457,48.0)));
g.setClip(clip);
// _0 is CompositeGraphicsNode
float alpha__0_0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_0 = g.getClip();
AffineTransform defaultTransform__0_0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_0 is ShapeNode
paint = getColor(0, 172, 237, 255, disabled);
shape = new GeneralPath();
((GeneralPath)shape).moveTo(43.885242, 8.396486);
((GeneralPath)shape).curveTo(42.346073, 9.079261, 40.69164, 9.54051, 38.955627, 9.74802);
((GeneralPath)shape).curveTo(40.72741, 8.685787, 42.088566, 7.003743, 42.729504, 4.999347);
((GeneralPath)shape).curveTo(41.07089, 5.983137, 39.23405, 6.69729, 37.278812, 7.082187);
((GeneralPath)shape).curveTo(35.713284, 5.4139495, 33.48255, 4.3717966, 31.013771, 4.3717966);
((GeneralPath)shape).curveTo(26.273674, 4.3717966, 22.430393, 8.214705, 22.430393, 12.954802);
((GeneralPath)shape).curveTo(22.430393, 13.627536, 22.506123, 14.282698, 22.652546, 14.910875);
((GeneralPath)shape).curveTo(15.519056, 14.552753, 9.194354, 11.135743, 4.96111, 5.9427648);
((GeneralPath)shape).curveTo(4.222191, 7.210417, 3.798929, 8.684951, 3.798929, 10.258009);
((GeneralPath)shape).curveTo(3.798929, 13.235944, 5.31417, 15.863078, 7.6174045, 17.40225);
((GeneralPath)shape).curveTo(6.2103953, 17.35749, 4.88685, 16.97154, 3.729731, 16.32872);
((GeneralPath)shape).curveTo(3.7288945, 16.36637, 3.7288945, 16.40026, 3.7288945, 16.436659);
((GeneralPath)shape).curveTo(3.7288945, 20.595266, 6.6876264, 24.064198, 10.614083, 24.852985);
((GeneralPath)shape).curveTo(9.893948, 25.049198, 9.135657, 25.15421, 8.352852, 25.15421);
((GeneralPath)shape).curveTo(7.7997293, 25.15421, 7.2620444, 25.100239, 6.7378726, 25.00025);
((GeneralPath)shape).curveTo(7.8301444, 28.410107, 10.999985, 30.891901, 14.755955, 30.960848);
((GeneralPath)shape).curveTo(11.818435, 33.26316, 8.117519, 34.635365, 4.096011, 34.635365);
((GeneralPath)shape).curveTo(3.4031959, 34.635365, 2.7199192, 34.593525, 2.0484824, 34.515293);
((GeneralPath)shape).curveTo(5.84696, 36.950565, 10.358711, 38.3718, 15.205866, 38.3718);
((GeneralPath)shape).curveTo(30.993605, 38.3718, 39.62723, 25.292858, 39.62723, 13.950266);
((GeneralPath)shape).curveTo(39.62723, 13.578338, 39.61883, 13.208083, 39.60213, 12.83992);
((GeneralPath)shape).curveTo(41.27936, 11.629585, 42.734444, 10.118025, 43.885162, 8.396656);
g.setPaint(paint);
g.fill(shape);
origAlpha = alpha__0_0;
g.setTransform(defaultTransform__0_0);
g.setClip(clip__0_0);
float alpha__0_1 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1 = g.getClip();
AffineTransform defaultTransform__0_1 = g.getTransform();
g.transform(new AffineTransform(0.13750000298023224f, 0.0f, 0.0f, 0.13750000298023224f, 23.352754592895508f, 23.226686477661133f));
// _0_1 is CompositeGraphicsNode
float alpha__0_1_0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_0 = g.getClip();
AffineTransform defaultTransform__0_1_0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_0 is ShapeNode
paint = new LinearGradientPaint(new Point2D.Double(-666.1166381835938, 413.044921875), new Point2D.Double(-553.2698974609375, 525.9075927734375), new float[] {0.0f,1.0f}, new Color[] {getColor(0, 0, 0, 255, disabled),getColor(255, 255, 255, 255, disabled)}, MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, new AffineTransform(0.9988399744033813f, 0.0f, 0.0f, 0.9986990094184875f, 689.0077514648438f, -388.84375f));
shape = new GeneralPath();
((GeneralPath)shape).moveTo(79.8646, 119.09957);
((GeneralPath)shape).curveTo(115.26228, 167.35492, 149.90417, 105.63105, 149.85327, 68.51282);
((GeneralPath)shape).curveTo(149.79308, 24.62677, 105.31237, 0.09913, 79.8356, 0.09913);
((GeneralPath)shape).curveTo(38.94318, 0.09913, 0.0, 33.89521, 0.0, 80.13502);
((GeneralPath)shape).curveTo(0.0, 131.53102, 44.64038, 159.99998, 79.8356, 159.99998);
((GeneralPath)shape).curveTo(71.87113, 158.85324, 45.329403, 153.16603, 44.972683, 92.03322);
((GeneralPath)shape).curveTo(44.732815, 50.68662, 58.46025, 34.16771, 79.777954, 41.43417);
((GeneralPath)shape).curveTo(80.25539, 41.61124, 103.29187, 50.69868, 103.29187, 80.384705);
((GeneralPath)shape).curveTo(103.29187, 109.944626, 79.8646, 119.09958, 79.8646, 119.09958);
((GeneralPath)shape).closePath();
g.setPaint(paint);
g.fill(shape);
origAlpha = alpha__0_1_0;
g.setTransform(defaultTransform__0_1_0);
g.setClip(clip__0_1_0);
float alpha__0_1_1 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_1 = g.getClip();
AffineTransform defaultTransform__0_1_1 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_1 is ShapeNode
paint = new LinearGradientPaint(new Point2D.Double(-553.2697143554688, 525.9077758789062), new Point2D.Double(-666.1163940429688, 413.0451965332031), new float[] {0.0f,1.0f}, new Color[] {getColor(0, 0, 0, 255, disabled),getColor(255, 255, 255, 255, disabled)}, MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, new AffineTransform(0.9988399744033813f, 0.0f, 0.0f, 0.9986990094184875f, 689.0077514648438f, -388.84375f));
shape = new GeneralPath();
((GeneralPath)shape).moveTo(79.82327, 41.40081);
((GeneralPath)shape).curveTo(56.43322, 33.33893, 27.78025, 52.61662, 27.78025, 91.22962);
((GeneralPath)shape).curveTo(27.78025, 154.2776, 74.5008, 160.0, 80.16441, 160.0);
((GeneralPath)shape).curveTo(121.05683, 159.99998, 160.0, 126.20391, 160.0, 79.9641);
((GeneralPath)shape).curveTo(160.0, 28.5681, 115.35962, 0.09913, 80.16441, 0.09913);
((GeneralPath)shape).curveTo(89.91252, -1.25087, 132.70529, 10.649039, 132.70529, 69.135925);
((GeneralPath)shape).curveTo(132.70529, 107.2771, 100.75243, 128.0409, 79.96982, 119.169365);
((GeneralPath)shape).curveTo(79.492386, 118.992294, 56.4559, 109.904854, 56.4559, 80.218834);
((GeneralPath)shape).curveTo(56.4559, 50.658913, 79.82328, 41.400803, 79.82328, 41.400803);
((GeneralPath)shape).closePath();
g.setPaint(paint);
g.fill(shape);
origAlpha = alpha__0_1_1;
g.setTransform(defaultTransform__0_1_1);
g.setClip(clip__0_1_1);
origAlpha = alpha__0_1;
g.setTransform(defaultTransform__0_1);
g.setClip(clip__0_1);
origAlpha = alpha__0;
g.setTransform(defaultTransform__0);
g.setClip(clip__0);
g.setTransform(defaultTransform_);
g.setClip(clip_);

	}
	
	public Image getImage() {
		BufferedImage image =
            new BufferedImage(getIconWidth(), getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB);
    	Graphics2D g = image.createGraphics();
    	paintIcon(null, g, 0, 0);
    	g.dispose();
    	return image;
	}

    /**
     * Returns the X of the bounding box of the original SVG image.
     * 
     * @return The X of the bounding box of the original SVG image.
     */
    public static int getOrigX() {
        return 3;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * 
     * @return The Y of the bounding box of the original SVG image.
     */
    public static int getOrigY() {
        return 5;
    }

	/**
	 * Returns the width of the bounding box of the original SVG image.
	 * 
	 * @return The width of the bounding box of the original SVG image.
	 */
	public static int getOrigWidth() {
		return 48;
	}

	/**
	 * Returns the height of the bounding box of the original SVG image.
	 * 
	 * @return The height of the bounding box of the original SVG image.
	 */
	public static int getOrigHeight() {
		return 48;
	}

	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;
	
	/**
	 * Should this icon be drawn in a disabled state
	 */
	boolean disabled = false;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public TwitterJSONIcon() {
        this(getOrigWidth(),getOrigHeight(),false);
	}
	
	public TwitterJSONIcon(boolean disabled) {
        this(getOrigWidth(),getOrigHeight(),disabled);
	}
	
	/**
	 * Creates a new transcoded SVG image with the given dimensions.
	 *
	 * @param size the dimensions of the icon
	 */
	public TwitterJSONIcon(Dimension size) {
		this(size.width, size.height, false);
	}
	
	public TwitterJSONIcon(Dimension size, boolean disabled) {
		this(size.width, size.height, disabled);
	}

	public TwitterJSONIcon(int width, int height) {
		this(width, height, false);
	}
	
	public TwitterJSONIcon(int width, int height, boolean disabled) {
		this.width = width;
		this.height = height;
		this.disabled = disabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics,
	 * int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(x, y);
						
		Area clip = new Area(new Rectangle(0, 0, this.width, this.height));		
		if (g2d.getClip() != null) clip.intersect(new Area(g2d.getClip()));		
		g2d.setClip(clip);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d, disabled);
		g2d.dispose();
	}
}

