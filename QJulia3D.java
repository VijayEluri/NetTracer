import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Quaternionen-Julia-Fraktale
 */
public class QJulia3D implements Object3D, RenderingPrimitive
{
	private AABB cachedAABB = null;
	
	private RenderingPrimitive[] prims;
	private Material mat = null;
	
	private double[] c = new double[4];
	private int nmaxSurface = 100;
	private int nmaxNormal  = 8;
	private double epsSurface = 1e-3;
	private double epsNormal  = 1e-2;
	private double deltaTransp  = 1e-2;
	
	public QJulia3D(SceneReader in, List<Material> mats) throws Exception
	{
		String[] tokens = null;
		while ((tokens = in.getNextTokens()) != null)
		{
			switch (tokens.length)
			{
				case 1:
					if (tokens[0].equals("end"))
					{
						prims = new RenderingPrimitive[] {this};
						
						System.out.println("QJulia3D:");
						System.out.println("c: " + c[0] + ", " + c[1] + ", " + c[2] + ", " + c[3]);
						System.out.println("nmaxSurface: " + nmaxSurface);
						System.out.println("nmaxNormal: " + nmaxNormal);
						System.out.println("epsSurface: " + epsSurface);
						System.out.println("epsNormal: " + epsNormal);
						System.out.println("deltaTransp: " + deltaTransp);
						System.out.println();
						return;
					}
					break;
				
				case 2:
					if (tokens[0].equals("mat"))
					{
						mat = Scene.getLoadedMaterial(tokens[1], mats);
						if (mat == null)
						{
							System.err.println("Fehler, Material " + tokens[1] + " nicht gefunden.");
							throw new Exception();
						}
					}
					if (tokens[0].equals("nmaxSurface"))
						nmaxSurface = new Integer(tokens[1]);
					if (tokens[0].equals("nmaxNormal"))
						nmaxNormal = new Integer(tokens[1]);
					if (tokens[0].equals("epsSurface"))
						epsSurface = new Double(tokens[1]);
					if (tokens[0].equals("epsNormal"))
						epsNormal = new Double(tokens[1]);
					if (tokens[0].equals("deltaTransp"))
						deltaTransp = new Double(tokens[1]);
					break;
				
				case 5:
					if (tokens[0].equals("c"))
					{
						c[0] = new Double(tokens[1]);
						c[1] = new Double(tokens[2]);
						c[2] = new Double(tokens[3]);
						c[3] = new Double(tokens[4]);
					}
					break;
			}
		}
		
		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in QJulia3D-Definition.");
		throw new Exception();
	}
	
	/**
	 * Produkt zweier Quaternionen.
	 * TODO: Richtige Quaternionen-Klasse
	 */
	private void quatProd(double[] x, double[] y, double[] res)
	{
		res[0] = x[0] * y[0] - x[1] * y[1] - x[2] * y[2] - x[3] * y[3];
		res[1] = x[0] * y[1] + x[1] * y[0] + x[2] * y[3] - x[3] * y[2];
		res[2] = x[0] * y[2] - x[1] * y[3] + x[2] * y[0] + x[3] * y[1];
		res[3] = x[0] * y[3] + x[1] * y[2] - x[2] * y[1] + x[3] * y[0];
	}
	
	/**
	 * Quadrat eines Quaternions.
	 */
	private void quatSq(double[] z, double[] res)
	{
		res[0] = z[0] * z[0] - z[1] * z[1] - z[2] * z[2] - z[3] * z[3];
		res[1] = 2.0 * z[0] * z[1];
		res[2] = 2.0 * z[0] * z[2];
		res[3] = 2.0 * z[0] * z[3];
	}
	
	/**
	 * Quadrat der Länge eines Quaternions.
	 */
	private double quatSqrLen(double[] z)
	{
		return z[0] * z[0] + z[1] * z[1] + z[2] * z[2] + z[3] * z[3];
	}
	
	/**
	 * Idee nach: http://www.lichtundliebe.info/projects/projekte.html
	 */
	private Vec3 calcNormal(Vec3 p)
	{
		double gradX, gradY, gradZ;
		
		double[] qP = new double[4];
		qP[0] = p.x;
		qP[1] = p.y;
		qP[2] = p.z;
		qP[3] = 0.0;
		
		double[] gx1 = new double[4];
		gx1[0] = qP[0] - epsNormal;
		gx1[1] = qP[1];
		gx1[2] = qP[2];
		gx1[3] = qP[3];
		
		double[] gx2 = new double[4];
		gx2[0] = qP[0] + epsNormal;
		gx2[1] = qP[1];
		gx2[2] = qP[2];
		gx2[3] = qP[3];
		
		double[] gy1 = new double[4];
		gy1[0] = qP[0];
		gy1[1] = qP[1] - epsNormal;
		gy1[2] = qP[2];
		gy1[3] = qP[3];
		
		double[] gy2 = new double[4];
		gy2[0] = qP[0];
		gy2[1] = qP[1] + epsNormal;
		gy2[2] = qP[2];
		gy2[3] = qP[3];
		
		double[] gz1 = new double[4];
		gz1[0] = qP[0];
		gz1[1] = qP[1];
		gz1[2] = qP[2] - epsNormal;
		gz1[3] = qP[3];
		
		double[] gz2 = new double[4];
		gz2[0] = qP[0];
		gz2[1] = qP[1];
		gz2[2] = qP[2] + epsNormal;
		gz2[3] = qP[3];
		
		double[] t = new double[4];
		
		for (int i = 0; i < nmaxNormal; i++)
		{
			quatSq(gx1, t);
			gx1[0] = t[0] + c[0];
			gx1[1] = t[1] + c[1];
			gx1[2] = t[2] + c[2];
			gx1[3] = t[3] + c[3];
			
			quatSq(gx2, t);
			gx2[0] = t[0] + c[0];
			gx2[1] = t[1] + c[1];
			gx2[2] = t[2] + c[2];
			gx2[3] = t[3] + c[3];
			
			quatSq(gy1, t);
			gy1[0] = t[0] + c[0];
			gy1[1] = t[1] + c[1];
			gy1[2] = t[2] + c[2];
			gy1[3] = t[3] + c[3];
			
			quatSq(gy2, t);
			gy2[0] = t[0] + c[0];
			gy2[1] = t[1] + c[1];
			gy2[2] = t[2] + c[2];
			gy2[3] = t[3] + c[3];
			
			quatSq(gz1, t);
			gz1[0] = t[0] + c[0];
			gz1[1] = t[1] + c[1];
			gz1[2] = t[2] + c[2];
			gz1[3] = t[3] + c[3];
			
			quatSq(gz2, t);
			gz2[0] = t[0] + c[0];
			gz2[1] = t[1] + c[1];
			gz2[2] = t[2] + c[2];
			gz2[3] = t[3] + c[3];
		}
		
		gradX = Math.sqrt(quatSqrLen(gx2)) - Math.sqrt(quatSqrLen(gx1));
		gradY = Math.sqrt(quatSqrLen(gy2)) - Math.sqrt(quatSqrLen(gy1));
		gradZ = Math.sqrt(quatSqrLen(gz2)) - Math.sqrt(quatSqrLen(gz1));
		
		return new Vec3(gradX, gradY, gradZ).normalized();
	}
	
	/**
	 * Führe mit diesem Strahl einen Schnitttest mit dir selbst durch
	 */
	public Intersection intersectionTest(Ray r)
	{
		// Eintritts- und Austrittsalpha für die Clipping-Box suchen
		double[] alpha0arr = new double[2];
		if (!cachedAABB.alphaEntryExit(r, alpha0arr))
			return null;
		
		boolean firstTime = true;
		boolean inner     = false;
		double alpha = alpha0arr[0] + 0.01;
		double[] z  = new double[4];
		double[] z2 = new double[4];
		double[] t  = new double[4];
		
		// Wandere am Strahl entlang, aber nur innerhalb der Box
		while (alpha < alpha0arr[1])
		{
			// Hole den aktuellen Punkt ...
			Vec3 hitpoint = r.evaluate(alpha);
			
			z[0] = hitpoint.x;
			z[1] = hitpoint.y;
			z[2] = hitpoint.z;
			z[3] = 0.0;
			
			z2[0] = 1.0;
			z2[1] = 0.0;
			z2[2] = 0.0;
			z2[3] = 0.0;
			
			// ... und betrachte das Verhalten der Folge hier:
			double sqr_abs_z = 0.0;
			for (int n = 0; n < nmaxSurface; n++)
			{
				quatProd(z, z2, t);
				z2[0] = t[0] * 2.0;
				z2[1] = t[1] * 2.0;
				z2[2] = t[2] * 2.0;
				z2[3] = t[3] * 2.0;
				
				quatSq(z, t);
				z[0] = t[0] + c[0];
				z[1] = t[1] + c[1];
				z[2] = t[2] + c[2];
				z[3] = t[3] + c[3];
				
				// Abbruch, falls Divergenz gemeldet wurde.
				sqr_abs_z = quatSqrLen(z);
				if (sqr_abs_z >= 32.0)
				{
					break;
				}
			}
			
			double sqr_abs_z2 = quatSqrLen(z2);
			
			sqr_abs_z = Math.sqrt(sqr_abs_z);
			sqr_abs_z2 = Math.sqrt(sqr_abs_z2);
			
			// Die magische Formel für Unbounding-Volumes. Falls oben die Schleife
			// bei Erkennen von Divergenz abgebrochen wurde (und nur dann), dann
			// liefert diese Formel den Radius einer Kugel, der angibt, wie weit
			// wir noch von der Menge weg sind. So weit können wir also auf dem
			// Strahl auf jeden Fall weiterlaufen.
			double d = (sqr_abs_z / (2.0 * sqr_abs_z2)) * Math.log(sqr_abs_z);
			
			// Wurde direkt im ersten Durchlauf festgestellt, dass dieser Punkt
			// sehr nahe am Rand der Menge ist? Dann ist der Ray *in* der Menge
			// gestartet und somit ein Transmission-Ray.
			if (d < epsSurface && firstTime)
				inner = true;

			// Abhängig davon, ob wir von außen oder innen gegen den Rand der Menge
			// laufen, teste, der geschätzte Radius größer oder kleiner als die
			// gewünschte Genauigkeit ist. Falls ja, berechne hier die Normale und
			// melde einen Schnitt.
			if ((!inner && d < epsSurface) || (inner && d >= epsSurface))
			{
				Vec3 n = calcNormal(hitpoint);
				return new Intersection(this, hitpoint, n.times(inner ? -1.0 : 1.0),
										alpha,
										mat, mat.getDiffuseColor(hitpoint),
										mat.getSpecularColor(hitpoint),
										mat.getTransparentColor(hitpoint));
			}
			
			// Bist du innen, dann wandere eine feste Schrittweite weiter, da hier
			// die Abschätzung nicht funktioniert (d wird sehr, sehr klein sein).
			// Bist du außen, dann nutze die Abschätzung, um einen großen Sprung
			// zu machen.
			alpha += (inner ? deltaTransp : d);

			if (firstTime)
				firstTime = false;
		}
		
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
			cachedAABB = new AABB(new Vec3(0, 0, 0), new Vec3(2, 2, 2));
		}
		
		return cachedAABB;
	}
	
	public RenderingPrimitive[] getRenderingPrimitives()
	{
		return prims;
	}
}
