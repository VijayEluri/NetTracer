import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Mandelblub-Fraktale.
 * http://www.skytopia.com/project/fractal/2mandelbulb.html
 * https://sourceforge.net/projects/aoi/forums/forum/47784/topic/3464031
 * http://www.fractalforums.com/3d-fractal-generation/true-3d-mandlebrot-type-fractal/msg4109/#msg4109
 * http://www.bugman123.com/Hypercomplex/index.html
 * http://www.fractalforums.com/3d-fractal-generation/true-3d-mandlebrot-type-fractal/msg8073/#msg8073
 * http://www.fractalforums.com/3d-fractal-generation/true-3d-mandlebrot-type-fractal/msg7812/#msg7812
 */
public class Mandelbulb implements Object3D, RenderingPrimitive
{
	private AABB cachedAABB = null;
	private double step = 0.01;
	private int nmax = 10;
	private double normalEps = 1e-8;
	private double bailout = 2;
	private double accuracy = 1e-5;
	private double order = 8;

	private RenderingPrimitive[] prims;
	private Material mat = null;

	public Mandelbulb(SceneReader in, List<Material> mats) throws Exception
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
						return;
					}
					break;

				case 2:
					if (tokens[0].equals("mat"))
					{
						mat = Scene.getLoadedMaterial(tokens[1], mats);
						if (mat == null)
						{
							System.err.println("Fehler, Material "
									+ tokens[1] + " nicht gefunden.");
							throw new Exception();
						}
					}
					if (tokens[0].equals("step"))
						step = new Double(tokens[1]);
					if (tokens[0].equals("nmax"))
						nmax = new Integer(tokens[1]);
					if (tokens[0].equals("normalEps"))
						normalEps = new Double(tokens[1]);
					if (tokens[0].equals("bailout"))
						bailout = new Double(tokens[1]);
					if (tokens[0].equals("accuracy"))
						accuracy = new Double(tokens[1]);
					if (tokens[0].equals("order"))
						order = new Double(tokens[1]);
					break;
			}
		}

		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in "
				+ "Mandelbulb-Definition.");
		throw new Exception();
	}

	private int evalAtPoint(Vec3 hitpoint, double[] outR)
	{
		double zx = 0;
		double zy = 0;
		double zz = 0;

		double cx = 0;
		double cy = 0;
		double cz = 0;

		int n = 0;

		double r = 0;
		double theta = 0;
		double phi = 0;

		double rPow = 0;
		double sinThe, cosThe, sinPhi, cosPhi;
		double zx2, zy2, zz2;

		// Das Mandelbulb selbst funktioniert genauso wie das
		// Mandelbrot, der einzige Unterschied ist eine andere
		// Definition für die Potenzierung und die Addition.
		// Außerdem werden "triplex"-Zahlen verwendet statt den
		// normalen komplexen Zahlen.

		// Wir starten also mit z = 0 und c = hitpoint.
		zx = 0;
		zy = 0;
		zz = 0;

		cx = hitpoint.x;
		cy = hitpoint.y;
		cz = hitpoint.z;

		n = 0;
		r = 0;

		zx2 = zx*zx;
		zy2 = zy*zy;
		zz2 = zz*zz;
		while (n < nmax)
		{
			// Potenzierung
			r = Math.sqrt(zx2 + zy2 + zz2);
			if (r >= bailout)
			{
				n--;
				break;
			}

			theta = Math.atan2(Math.sqrt(zx2 + zy2), zz);
			phi = Math.atan2(zy, zx);

			sinThe = Math.sin(theta * order);
			cosThe = Math.cos(theta * order);
			sinPhi = Math.sin(phi * order);
			cosPhi = Math.cos(phi * order);

			rPow = Math.pow(r, order);

			zx = rPow * sinThe * cosPhi;
			zy = rPow * sinThe * sinPhi;
			zz = rPow * cosThe;

			// Addition
			zx += cx;
			zy += cy;
			zz += cz;

			zx2 = zx*zx;
			zy2 = zy*zy;
			zz2 = zz*zz;

			n++;
		}

		if (outR != null)
			outR[0] = r;
		return n;
	}

	private Vec3 normalAtPoint(Vec3 hitpoint)
	{
		Vec3 temp = null;

		double xr, xl, yr, yl, zr, zl;
		double[] carrier = new double[1];

		temp = new Vec3(hitpoint); temp.x += normalEps;
		evalAtPoint(temp, carrier); xl = carrier[0];
		temp = new Vec3(hitpoint); temp.x -= normalEps;
		evalAtPoint(temp, carrier); xr = carrier[0];

		temp = new Vec3(hitpoint); temp.y += normalEps;
		evalAtPoint(temp, carrier); yl = carrier[0];
		temp = new Vec3(hitpoint); temp.y -= normalEps;
		evalAtPoint(temp, carrier); yr = carrier[0];

		temp = new Vec3(hitpoint); temp.z += normalEps;
		evalAtPoint(temp, carrier); zl = carrier[0];
		temp = new Vec3(hitpoint); temp.z -= normalEps;
		evalAtPoint(temp, carrier); zr = carrier[0];

		Vec3 normal = new Vec3(xl - xr, yl - yr, zl - zr).normalized();
		return normal;
	}

	/**
	 * Führe mit diesem Strahl einen Schnitttest mit dir selbst durch
	 */
	public Intersection intersectionTest(Ray ray)
	{
		// Eintritts- und Austrittsalpha für die Clipping-Box suchen
		double[] alpha0arr = new double[2];
		if (!cachedAABB.alphaEntryExit(ray, alpha0arr))
			return null;

		double alpha = alpha0arr[0] + 0.01;
		double[] carrier = new double[1];
		boolean debug = true;

		if (debug) System.err.println("New ray");

		// Bisektion: Anfangssituation merken!
		boolean sitStart = (evalAtPoint(ray.evaluate(alpha), carrier) == nmax);
		if (debug) System.err.println("\t" + carrier[0]);
		boolean sitNow = false;
		double cstep = step;

		// Wandere am Strahl entlang, aber nur innerhalb der Box
		while (alpha < alpha0arr[1])
		{
			// Hole den aktuellen Punkt ...
			Vec3 hitpoint = ray.evaluate(alpha);

			// Schau dir die Situation an diesem Punkt an.
			sitNow = (evalAtPoint(hitpoint, carrier) == nmax);
			if (debug) System.err.println("\t" + carrier[0]);

			// Hat sie sich verändert? Dann starte Bisektion.
			if (sitNow != sitStart)
			{
				if (debug) System.err.println("\t* Bisection started");

				double a1 = alpha - cstep, a2 = alpha;

				while (cstep > accuracy)
				{
					// Gehe zum Mittelpunkt des aktuellen Stückes und
					// sample dort.
					cstep *= 0.5;
					alpha = a1 + cstep;

					hitpoint = ray.evaluate(alpha);
					sitNow = (evalAtPoint(hitpoint, carrier) == nmax);
					if (debug) System.err.println("\t" + carrier[0]);

					/*
					// Original: bei "a2 = alpha" passiert effektiv nix.
					if (sitNow != sitStart)
					{
						a2 = alpha;
					}
					else
					{
						a1 = alpha;
					}
					*/

					if (sitNow == sitStart)
						a1 = alpha;
				}

				if (debug) System.err.println("\t* Accu reached");

				// Genauigkeit erreicht.
				Vec3 normal = normalAtPoint(hitpoint);
				return new Intersection(this, hitpoint, normal,
										alpha,
										mat, mat.getDiffuseColor(hitpoint),
										mat.getSpecularColor(hitpoint),
										mat.getTransparentColor(hitpoint));
			}

			// Wir sind noch auf derselben Seite, weitermachen.
			alpha += cstep;
		}

		if (debug) System.err.println("\tMISS");

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
			cachedAABB = new AABB(new Vec3(0, 0, 0),
					new Vec3(1.1, 1.1, 1.1));
		}

		return cachedAABB;
	}

	public RenderingPrimitive[] getRenderingPrimitives()
	{
		return prims;
	}
}
