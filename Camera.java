/**
 * Eine Kamera. Hat Ort, Blickrichtung, FOV und Auflösung, dient auch
 * als RayCaster.
 */
public class Camera
{
	public double fov = toRad(60);
	public int pxX, pxY;
	public Vec3 origin = new Vec3();
	public Vec3 viewdir = new Vec3(0.0, 0.0, -1.0);
	public Vec3 viewdirInv = new Vec3(0.0, 0.0, 1.0);
	public Vec3 updir = new Vec3(0.0, 1.0, 0.0);
	public Vec3 rdir = viewdir.cross(updir);
	
	public double focalDistance = 0.0;
	public double dofAmount = 0.25;
	public int dofRays = 3;
	public double dofLambda = 0.0;
	
	private Vec3 planeVec;
	
	/**
	 * Kamera aus Scanner laden
	 */
	public Camera(SceneReader in) throws Exception
	{
		String[] tokens = null;
		while ((tokens = in.getNextTokens()) != null)
		{
			switch (tokens.length)
			{
				// Ende?
				case 1:
					if (tokens[0].equals("end"))
					{
						// rdir berechnen
						rdir = viewdir.cross(updir);
						
						viewdirInv = viewdir.times(-1.0);
						
						// DOF-Wert vorberechnen - wo befindet sich
						// die focalPlane?
						if (focalDistance != 0.0)
						{
							dofLambda = origin.plus(viewdir
										.times(focalDistance))
										.dot(viewdirInv);
						}
						
						dump();
						return;
					}
					break;
				
				case 2:
					if (tokens[0].equals("fov"))
						fov = toRad(new Double(tokens[1]));
					if (tokens[0].equals("focalDistance"))
						focalDistance = new Double(tokens[1]);
					if (tokens[0].equals("dofAmount"))
						dofAmount = new Double(tokens[1]);
					if (tokens[0].equals("dofRays"))
						dofRays = new Integer(tokens[1]);
					break;
				
				case 4:
					if (tokens[0].equals("origin"))
						origin = new Vec3(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											);
					if (tokens[0].equals("viewdir"))
						viewdir = new Vec3(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											);
					if (tokens[0].equals("updir"))
						updir = new Vec3(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											);
					if (tokens[0].equals("lookat"))
						calcViewDirections(new Vec3(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											));
					break;
			}
		}
		
		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in Kameradefinition.");
		throw new Exception();
	}
	
	private void dump()
	{
		System.out.println("Kamera:");
		System.out.println("fov: " + toDeg(fov));
		System.out.println("focalDistance: " + focalDistance);
		System.out.println("dofAmount: " + dofAmount);
		System.out.println("dofRays: " + dofRays);
		System.out.println("origin: " + origin);
		System.out.println("viewdir: " + viewdir);
		System.out.println("updir: " + updir);
		System.out.println("rdir: " + rdir);
		System.out.println();
	}
	
	/**
	 * Berechnet bei gegebenem Zielpunkt und bereits voreingestelltem
	 * Up-Vektor die Vektoren updir, viewdir und rdir neu, sodass die
	 * Kamera an den Zielpunkt schaut. Der Up-Vektor ist dazu nötig,
	 * um den "Kippwinkel" der Kamera zu setzen.
	 */
	private void calcViewDirections(Vec3 worldpoint)
	{
		updir.normalize();
		
		viewdir = worldpoint.minus(origin);
		viewdir.normalize();
		
		rdir = viewdir.cross(updir);
		rdir.normalize();
		
		updir = rdir.cross(viewdir);
		updir.normalize();
	}
	
	/**
	 * Pixelanzahl ist erstmal unabhängig von der sonstigen Definition
	 * der Kamera und kann auch für mehrere Rendervorgänge geändert
	 * werden, ohne gleich eine ganz neue Kamera zu erzeugen.
	 */
	public void setResolution(int pxX, int pxY)
	{
		this.pxX = pxX;
		this.pxY = pxY;
		
		// Vektor vom Kameraursprung zum Mittelpunkt der Bildebene
		planeVec = viewdir.times((0.5 * pxY) / Math.tan(0.5 * fov));
	}
	
	/**
	 * Gib einen Ray raus, der dieser Position im Raum entspricht.
	 * Sind double-Werte, da beim Antialising nicht unbedingt diskrete
	 * Pixel angefordert werden.
	 */
	public Ray castRay(double x, double y)
	{
		// Vereinfachtes RayCasting, da der out-Vektor nachher sowieso
		// normalisiert wird. Daher sind ein paar Sachen egal, vorallem
		// ist es nicht notwendig zu definieren, wieviele Längeneinheiten
		// ein Pixel umfassen soll.
		
		// Prinzipiell: Gehe auf dem Screen die angeforderte Höhe / Weite
		// nach oben / rechts.
		double usteps = -y + 0.5 * pxY;
		double rsteps = x - 0.5 * pxX;
		
		Vec3 out = new Vec3(planeVec);
		out.add(updir.times(usteps));
		out.add(rdir.times(rsteps));
		
		return new Ray(origin, out);
	}
	
	public double toRad(double deg)
	{
		return (Math.PI / 180.0) * deg;
	}
	
	public double toDeg(double rad)
	{
		return (180.0 / Math.PI) * rad;
	}
}
