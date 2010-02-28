import java.io.Serializable;

/**
 * Ein einheitliches Material
 */
public class UniformMaterial extends Material implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private RGBColor diffuse = null;
	private RGBColor specular = null;
	private RGBColor transparent = null;
	
	public UniformMaterial(SceneReader in, String mname) throws Exception
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
						
						if (diffuse == null)
							diffuse = RGBColor.white();
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
					break;
				
				case 4:
					if (tokens[0].equals("diffuse"))
						diffuse = new RGBColor(new Double(tokens[1]),
											   new Double(tokens[2]),
											   new Double(tokens[3]));
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
	
	public RGBColor getDiffuseColor(Vec3 p)
	{
		return diffuse;
	}
	
	public RGBColor getSpecularColor(Vec3 p)
	{
		return specular;
	}
	
	public RGBColor getTransparentColor(Vec3 p)
	{
		return transparent;
	}
	
	/**
	 * Debug-Krimskrams
	 */
	public void dump()
	{
		System.out.println("UniformMaterial " + name + ":");
		System.out.println("diffuse: " + diffuse.toString());
		System.out.println("specular: " + specular.toString());
		System.out.println("transparent: " + transparent.toString());
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
