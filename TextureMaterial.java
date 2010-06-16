/*
	Copyright 2008, 2009, 2010  Peter Hofmann

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import java.io.File;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.Serializable;

/**
 * Hält eine Textur als short[][] bereit und kann den Farbwert an
 * gegebener UV-Position zurückgeben.
 */
public class TextureMaterial extends Material implements Serializable
{
	private static final long serialVersionUID = 20100302001L;

	private int w = -1;
	private int h = -1;

	// Eigentlich würde hier auch ein byte[][] reichen. byte's sind in
	// Java jedoch immer SIGNED, was hier natürlich total unangebracht
	// ist. Deshalb nehmen wir einen short, da hier die nötigen Werte
	// reinpassen. Bei einem byte müsste man später mehr casten und
	// rechnen...
	private short[][] pixels = null;

	private RGBColor specular = null;
	private RGBColor transparent = null;

	public TextureMaterial(SceneReader in, String mname) throws Exception
	{
		name = mname;

		String[] tokens = null;
		while ((tokens = in.getNextTokens()) != null)
		{
			switch (tokens.length)
			{
				// Ende?
				case 1:
					if (tokens[0].equals("end"))
					{
						// Sicherstellen, dass auch gültige Werte
						// vorherrschen.
						transparency		= clip(transparency, 0.0, 1.0);
						specularity			= clip(specularity, 0.0, 1.0);
						shininess			= clip(shininess, 0.0, 1.0);
						shininessSharpness	= clip(shininessSharpness, 0.0, 100.0);
						roughness			= clip(roughness, 0.0, 1.0);
						roughRays			= (roughRays < 1 ? 1 : roughRays);
						cloudiness			= clip(cloudiness, 0.0, 1.0);
						cloudyRays			= (cloudyRays < 1 ? 1 : cloudyRays);
						ior					= clip(ior, 0.001, 10.0);

						if (specular == null)
							specular = RGBColor.white();
						if (transparent == null)
							transparent = RGBColor.white();

						dump();
						return;
					}
					break;

				// Zweistellige Felder (Key Value)
				case 2:
					if (tokens[0].equals("transparency"))
						transparency = new Double(tokens[1]);
					if (tokens[0].equals("specularity"))
						specularity = new Double(tokens[1]);
					if (tokens[0].equals("shininess"))
						shininess = new Double(tokens[1]);
					if (tokens[0].equals("shininessSharpness"))
						shininessSharpness = new Double(tokens[1]);
					if (tokens[0].equals("ior"))
						ior = new Double(tokens[1]);
					if (tokens[0].equals("roughness"))
						roughness = new Double(tokens[1]);
					if (tokens[0].equals("roughRays"))
						roughRays = new Integer(tokens[1]);
					if (tokens[0].equals("cloudiness"))
						cloudiness = new Double(tokens[1]);
					if (tokens[0].equals("cloudyRays"))
						cloudyRays = new Integer(tokens[1]);

					if (tokens[0].equals("file"))
					{
						BufferedImage img
							= ImageIO.read(in.getRelativePath(tokens[1]));
						w = img.getWidth();
						h = img.getHeight();
						pixels = new short[h][];
						for (int y = 0; y < h; y++)
						{
							pixels[y] = new short[w * 3];

							int lx = 0;
							for (int x = 0; x < w; x++)
							{
								int argb = img.getRGB(x, y);
								pixels[y][lx++] = (short)((argb & 0x00FF0000) >> 16);
								pixels[y][lx++] = (short)((argb & 0x0000FF00) >>  8);
								pixels[y][lx++] = (short)((argb & 0x000000FF)      );
							}
						}
						img = null;
					}

					break;
				case 4:
					if (tokens[0].equals("specular"))
						specular = new RGBColor(new Double(tokens[1]),
												new Double(tokens[2]),
												new Double(tokens[3]));
					if (tokens[0].equals("transparent"))
						transparent = new RGBColor(new Double(tokens[1]),
												   new Double(tokens[2]),
												   new Double(tokens[3]));
					break;
			}
		}

		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in Materialdefinition von " + mname);
		throw new Exception();
	}

	private double clip(double val, double min, double max)
	{
		double out = val;
		if (out < min)
			out = min;

		if (out > max)
			out = max;

		return out;
	}

	/**
	 * Verwirf p.z und interpretiere die anderen beiden als UV-Koordinaten
	 */
	public RGBColor getDiffuseColor(Vec3 p)
	{
		if (pixels == null)
			return RGBColor.black();

		// Von den Intervallen [0, 1] auf Texturgröße hoch
		double realx = p.x * (double)w;
		double realy = (1.0 - p.y) * (double)h;

		int rw = (int)realx; 
		int rh = (int)realy;

		// Grenzen sicherstellen
		if (rw >= w)
			rw = w - 1;
		else if (rw < 0)
			rw = 0;

		if (rh >= h)
			rh = h - 1;
		else if (rh < 0)
			rh = 0;

		// Glätten, sofern nicht am Texturrand (vorerst vereinfacht)
		if (rw < w - 1 && rh < h - 1)
		{
			// Bilineare Filterung
			RGBColor nn = new RGBColor(
					pixels[rh    ][((rw    ) * 3)    ] / 255.0,
					pixels[rh    ][((rw    ) * 3) + 1] / 255.0,
					pixels[rh    ][((rw    ) * 3) + 2] / 255.0
					);
			RGBColor en = new RGBColor(
					pixels[rh    ][((rw + 1) * 3)    ] / 255.0,
					pixels[rh    ][((rw + 1) * 3) + 1] / 255.0,
					pixels[rh    ][((rw + 1) * 3) + 2] / 255.0
					);
			RGBColor ne = new RGBColor(
					pixels[rh + 1][((rw    ) * 3)    ] / 255.0,
					pixels[rh + 1][((rw    ) * 3) + 1] / 255.0,
					pixels[rh + 1][((rw    ) * 3) + 2] / 255.0
					);
			RGBColor ee = new RGBColor(
					pixels[rh + 1][((rw + 1) * 3)    ] / 255.0,
					pixels[rh + 1][((rw + 1) * 3) + 1] / 255.0,
					pixels[rh + 1][((rw + 1) * 3) + 2] / 255.0
					);

			double x2x = rw + 1 - realx;
			double xx1 = realx - rw;
			double y2y = rh + 1 - realy;
			double yy1 = realy - rh;

			nn.scale(x2x);
			en.scale(xx1);

			ne.scale(x2x);
			ee.scale(xx1);

			// Anlegen neuer Objekte vermeiden, das spart Zeit und schont
			// den Garbage Collector.

			//RGBColor R1 = nn.plus(en);
			//RGBColor R2 = ne.plus(ee);
			nn.add(en);
			ne.add(ee);

			//R1.scale(y2y);
			//R2.scale(yy1);
			nn.scale(y2y);
			ne.scale(yy1);

			//R1.add(R2);
			nn.add(ne);

			return nn;
		}
		else
		{
			// Dort Farbe holen und als RGBColor zurück
			return new RGBColor(
					pixels[rh][(rw * 3)    ] / 255.0,
					pixels[rh][(rw * 3) + 1] / 255.0,
					pixels[rh][(rw * 3) + 2] / 255.0
					);
		}
	}

	public RGBColor getSpecularColor(Vec3 p)
	{
		// Vorerst fest, später vielleicht Glanzmaps o.ä.
		return specular;
	}

	public RGBColor getTransparentColor(Vec3 p)
	{
		return transparent;
	}

	public void dump()
	{
		System.out.println("TextureMaterial " + name + ":");
		System.out.println("transparency: " + transparency);
		System.out.println("specularity: " + specularity);
		System.out.println("shininess: " + shininess);
		System.out.println("shininessSharpness: " + shininessSharpness);
		System.out.println("roughness: " + roughness);
		System.out.println("roughRays: " + roughRays);
		System.out.println("ior: " + ior);
		System.out.println();
	}

	public String toString()
	{
		return name;
	}
}
