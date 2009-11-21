/**
 * Ein Punktlicht mit Phong-Shading.
 */
public class PointLight extends Light
{
	public PointLight(SceneReader in) throws Exception
	{
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
						System.out.println("PointLight:");
						System.out.println("color: " + color);
						System.out.println("intensity: " + intensity);
						System.out.println("decayRate: " + decayRate);
						System.out.println("origin: " + origin);
						System.out.println();
						return;
					}
					break;
				
				case 2:
					if (tokens[0].equals("intensity"))
						intensity = new Double(tokens[1]);
					if (tokens[0].equals("decayRate"))
						decayRate = new Double(tokens[1]);
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
		System.err.println("Fehler, unerwartetes Ende in PointLight-Definition.");
		throw new Exception();
	}
	
	public void lighting(Material mat, Vec3 L, Vec3 R, Vec3 N, double[] li)
	{
		// Alle normalisieren, aber Länge von L behalten
		double distance = L.length();
		L.scale(1.0 / distance);
		/* das ist extern schon sichergestellt
		R.normalize();
		N.normalize();
		*/
		
		double diffuse = L.dot(N);
		double specular = L.dot(R);
		
		diffuse = (diffuse < 0.0 ? 0.0 : diffuse);
		specular = (specular < 0.0 ? 0.0 : specular);
		
		
		// Bestimmt, wie scharf die Shiny-Reflektionen sind
		double shinySharp = mat.shininessSharpness * 30.0;
		
		// Bestimmt, wie shiny die Reflektion ist
		double shinyAmount = mat.shininess * 2.0;
		specular = Math.pow(specular, shinySharp);
		
		// Setze diffuse- und shiny-Werte im li-Array
		li[0] = (intensity * diffuse) / (distance * decayRate);
		li[1] = (intensity * specular * shinyAmount) / (distance * decayRate);
	}
}
