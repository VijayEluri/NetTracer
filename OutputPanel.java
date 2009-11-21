import java.io.*;
import java.awt.image.*;
import java.awt.*;
import javax.swing.*;
import javax.imageio.*;

/**
 * Customized Panel, das die Pixel des Ergebnisses darstellt, soweit
 * diese schon berechnet sind. Kann seinen Inhalt auch in eine Datei
 * speichern.
 */
public class OutputPanel extends JPanel
{
	private Scene toRender;
	private int[] px;
	
	public boolean showCriticalPixels = false;
	
	public OutputPanel(Scene toRender)
	{
		super();
		this.toRender = toRender;
		
		this.px = new int[toRender.set.sizeX * toRender.set.sizeY];
	}
	
	/**
	 * Wird von AWT/Swing aufgerufen, wenn ein Repaint angefordert wurde
	 * bzw. unten von exportImage()
	 */
	synchronized public void paint(Graphics g)
	{
		// Male das ganze Bild neu.
		
		// In der Antialiasing-Stufe kann es also vorkommen, dass gerade
		// noch Rays verfolgt werden und die Farbe noch nicht gemittelt
		// wurde. Dann ist dieser Pixel temporär ziemlich hell.
		
		boolean c = showCriticalPixels;
		
		int[] tpos = null;
		if (toRender.tpos == null)
		{
			// Es liegen noch keine Informationen über die Positionen vor
			tpos = new int[1];
			tpos[0] = -1;
		}
		else
		{
			tpos = toRender.tpos.get();
		}
		
		int indicator = 0xFF00FF00;
		int aaover = 0x70FF0000;
		int pindex = 0;
		boolean overlayPos = false;
		for (int y = 0; y < toRender.set.sizeY; y++)
		{
			// Aktuelle Zeile mit Indicator überlagern?
			overlayPos = false;
			for (int i = 0; i < tpos.length; i++)
				if (tpos[i] == y)
					overlayPos = true;
			
			for (int x = 0; x < toRender.set.sizeX; x++)
			{
				if (overlayPos)
					px[pindex++] = indicator;
				else
				{
					// Sollen die kritischen Pixel hervorgehoben werden?
					if (c && toRender.criticalPixels[x][y])
						px[pindex++] = RGBColor.overlayARGB(aaover, toRender.pixels[x][y].toRGB());
					else
						px[pindex++] = toRender.pixels[x][y].toRGB();
				}
			}
		}
		Image img = createImage(
					new MemoryImageSource(
						toRender.set.sizeX, toRender.set.sizeY, px, 0, toRender.set.sizeX));
		((Graphics2D)g).drawImage(img, new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);
	}
	
	/**
	 * Speichere deinen aktuellen Inhalt dort rein
	 */
	public void exportImage(String path)
	{
		File ziel = new File(path);
		
		// Koennen wir denn schreiben?
		if (!(ziel.getParentFile()).canWrite())
		{
			System.err.println("Kann \"" + ziel.getAbsolutePath()
						+ "\" nicht schreiben (Zugriff verweigert)");
			return;
		}
		
		// Aus der Dateinamenserweiterung auf den Bildtyp schliessen
		String bildTyp = ziel.getName();
		bildTyp = bildTyp.substring(bildTyp.lastIndexOf('.') + 1);
		
		// neues BufferedImage direkt aus diesem Panel erstellen
		BufferedImage img = (BufferedImage)createImage(getWidth(), getHeight());

		// Graphics-Objekt davon besorgen
		Graphics g = img.getGraphics();
		
		// da wird nun reingezeichnet
		paint(g);
		
		// Bild speichern
		try
		{
			ImageIO.write(img, bildTyp, ziel);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		System.out.println("Bild exportiert: \"" + path + "\"");
	}
}
