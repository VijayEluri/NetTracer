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


import java.io.Serializable;

/**
 * Ein SphereLight mit Phong-Shading. Speichert sich zusätzliche
 * Informationen, damit man die Position dieses Lichtes jittern kann.
 */
public class SphereLight extends PointLight implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public double radius = 0.3;
	public int numrays = 3;
	public double rayscale = 1.0 / (double)numrays;

	public SphereLight(SceneReader in) throws Exception
	{
		super(null);

		if (in == null)
			return;

		String[] tokens = null;
		while ((tokens = in.getNextTokens()) != null)
		{
			switch (tokens.length)
			{
				case 1:
					if (tokens[0].equals("end"))
					{
						if (color == null)
							color = RGBColor.white();
						if (origin == null)
							origin = new Vec3();
						intensity = (intensity < 0 ? 1.0 : intensity);
						decayRate = (decayRate < 0 ? 0.1 : decayRate);
						radius = (radius < 0 ? 0.0 : radius);
						numrays = (numrays < 1 ? 1 : numrays);
						rayscale = 1.0 / (double)numrays;
						System.out.println("SphereLight:");
						System.out.println("color: " + color);
						System.out.println("intensity: " + intensity);
						System.out.println("decayRate: " + decayRate);
						System.out.println("origin: " + origin);
						System.out.println("radius: " + radius);
						System.out.println("numrays: " + numrays);
						System.out.println();
						return;
					}
					break;

				case 2:
					if (tokens[0].equals("intensity"))
						intensity = new Double(tokens[1]);
					if (tokens[0].equals("decayRate"))
						decayRate = new Double(tokens[1]);
					if (tokens[0].equals("radius"))
						radius = new Double(tokens[1]);
					if (tokens[0].equals("numrays"))
						numrays = new Integer(tokens[1]);
					break;

				case 4:
					if (tokens[0].equals("origin"))
						origin = new Vec3(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											);
					if (tokens[0].equals("color"))
						color = new RGBColor(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											);
						break;
			}
		}

		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in SphereLight-Definition.");
		throw new Exception();
	}
}
