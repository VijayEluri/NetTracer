import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.Serializable;

/**
 * Metaballs / Blobs
 * 
 * http://www.geisswerks.com/ryan/BLOBS/blobs.html
 * http://graphics.stanford.edu/~jtalton/UCSC/CMPS160/Spring06/lectures/Raytracing%20and%20Implicit%20Surfaces.pdf
 * http://www.softcomputing.net/~ijcism/special_issue_1/paper1.pdf
 */
public class Blob3D implements Object3D, RenderingPrimitive, Serializable
{
	private AABB cachedAABB = null;
	
	private RenderingPrimitive[] prims;
	private Material mat = null;
	
	private double accuracy = 1e-8, startstep = 0.2, normaldelta = 0.01;
	private final double threshold = 1.0;
	
	private Vec3[] charges = null;
	private Double[] weights = null;
	
	public Blob3D(SceneReader in, List<Material> mats) throws Exception
	{
		LinkedList<Vec3> c = new LinkedList<Vec3>();
		LinkedList<Double> w = new LinkedList<Double>();
		
		String[] tokens = null;
		while ((tokens = in.getNextTokens()) != null)
		{
			switch (tokens.length)
			{
				case 1:
					if (tokens[0].equals("end"))
					{
						prims = new RenderingPrimitive[] {this};
						charges = c.toArray(new Vec3[0]);
						weights = w.toArray(new Double[0]);
						
						System.out.println("Blob3D:");
						System.out.println("mat: " + mat);
						System.out.println("accuracy: " + accuracy);
						System.out.println("startstep: " + startstep);
						System.out.println("normaldelta: " + normaldelta);
						//System.out.println("origin: " + origin);
						System.out.println("Charges: " + charges.length);
						System.out.println();
						return;
					}
					break;
				
				case 2:
					if (tokens[0].equals("accuracy"))
						accuracy = new Double(tokens[1]);
					if (tokens[0].equals("startstep"))
						startstep = new Double(tokens[1]);
					if (tokens[0].equals("normaldelta"))
						normaldelta = new Double(tokens[1]);
						
					if (tokens[0].equals("mat"))
					{
						mat = Scene.getLoadedMaterial(tokens[1], mats);
						if (mat == null)
						{
							System.err.println("Fehler, Material " + tokens[1] + " nicht gefunden.");
							throw new Exception();
						}
					}
						
					break;
				
				case 6:
					if (tokens[0].equals("c"))
						w.add(new Double(tokens[1]));
						c.add(new Vec3(
											new Double(tokens[3]),
											new Double(tokens[4]),
											new Double(tokens[5])
											));
					break;
			}
		}
		
		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in Blob3D-Definition.");
		throw new Exception();
	}
	
	/**
	 * Die Blub-Summenfunktion.
	 */
	private double F(double x, double y, double z)
	{
		double sum = 0.0, d1, d2, d3;
		for (int i = 0; i < charges.length; i++)
		{
			Vec3 c = charges[i];
			double w = weights[i];
			
			d1 = (c.x - x);
			d2 = (c.y - y);
			d3 = (c.z - z);
			sum += (w*w) / (d1*d1 + d2*d2 + d3*d3);
		}
		return sum;
	}
	
	/**
	 * Approximiert die Normale an diesem Punkt über den Gradienten
	 */
	private Vec3 calcNormal(Vec3 p)
	{
		double h = normaldelta;
		return new Vec3(F(p.x - h, p.y, p.z) - F(p.x + h, p.y, p.z),
						F(p.x, p.y - h, p.z) - F(p.x, p.y + h, p.z),
						F(p.x, p.y, p.z - h) - F(p.x, p.y, p.z + h)).normalized();
	}
	
	/**
	 * Führe mit diesem Strahl einen Schnitttest mit dir selbst durch
	 */
	public Intersection intersectionTest(Ray r)
	{
		// Wenn der Strahl meine AABB nicht schneidet, dann mich auch nicht.
		// Suche hier auch gleich den eventuellen Wert von Alpha für den
		// Eintritt in die AABB sowie Austritt
		double[] alpha0arr = new double[2];
		if (!cachedAABB.alphaEntryExit(r, alpha0arr))
			return null;
		
		// Ausgangssituation feststellen, denn wir könnten ja auch
		// theoretisch *im* Blob mit dem Ray starten.
		boolean start = (F(r.origin.x, r.origin.y, r.origin.z) >= threshold);
		
		double alpha = alpha0arr[0], step = startstep;
		double x, y, z;
		boolean sit;
		
		// Schiebe den Ray-Parameter maximal so weit bis er die AABB
		// wieder verlässt. Schaue dabei, ob sich zwischendrin die
		// Situation ändert.
		while (alpha < alpha0arr[1])
		{
			alpha += step;
			
			x = r.origin.x + alpha * r.direction.x;
			y = r.origin.y + alpha * r.direction.y;
			z = r.origin.z + alpha * r.direction.z;
			
			sit = (F(x, y, z) >= threshold);
			
			if (sit == !start)
			{
				// Situation hat sich geändert, jetzt Bisektion bis die
				// gewünschte Genauigkeit erreicht ist.
				double a1 = alpha - step, a2 = alpha;
				
				while (step > accuracy)
				{
					// Gehe zum Mittelpunkt des aktuellen Stückes und
					// sample dort.
					step *= 0.5;
					
					alpha = a1 + step;
					
					x = r.origin.x + alpha * r.direction.x;
					y = r.origin.y + alpha * r.direction.y;
					z = r.origin.z + alpha * r.direction.z;
					
					sit = (F(x, y, z) >= threshold);
					
					if (sit == !start)
					{
						// Wir sind wieder "innerhalb" und müssen noch weiter
						// zurück
						a2 = alpha;
					}
					else
					{
						// "Außerhalb", also gehe wieder ein Stück in Richtung
						// Oberfläche
						a1 = alpha;
					}
				}
				
				// Genauigkeit erreicht, berechne Normale und gib zurück
				Vec3 p = new Vec3(x, y, z);
				Vec3 n = calcNormal(p);
				return new Intersection(this, p, n, alpha,
										mat, mat.getDiffuseColor(p),
										mat.getSpecularColor(p),
										mat.getTransparentColor(p));
			}
		}
		
		// Kein Schnitt gefunden
		return null;
	}
	
	/**
	 * Gib mir deine AABB
	 */
	public AABB getAABB()
	{
		// AABB wird gecached, da sonst der Tree-Bau Jahrhunderte benötigt
		if (cachedAABB == null)
		{
			// Zu diesem Zeitpunkt haben wir noch Zeit, also:
			// - Denke dir um jede Charge eine volle Sphere mit Radius
			//   weight[i] (ein paar Prozent mehr).
			// - Erstelle eine AABB an dieser Stelle, die diese Sphere
			//   umfasst.
			// - Das für jede einzelne Charge. Dann eine große AABB,
			//   die diese kleinen AABB's umfasst.
			
			ArrayList<AABB> a = new ArrayList<AABB>();
			
			double epsilon = 1.5;
			for (int i = 0; i < charges.length; i++)
			{
				double r = weights[i] * epsilon;
				a.add(new AABB(new Vec3(charges[i]), new Vec3(r, r, r)));
			}
			
			cachedAABB = new AABB(a);
		}
		
		return cachedAABB;
	}
	
	public RenderingPrimitive[] getRenderingPrimitives()
	{
		return prims;
	}
}
