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
	private double firststep = 0.01;
	private int nmax = 10;
	private double normalEps = 1e-8;
	private double bailout = 2;
	private double accuracy = 1e-5;
	private int cascadeLevel = 3;
	private boolean debug = false;

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
					if (tokens[0].equals("debug"))
						debug = true;

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
					if (tokens[0].equals("firststep"))
						firststep = new Double(tokens[1]);
					if (tokens[0].equals("nmax"))
						nmax = new Integer(tokens[1]);
					if (tokens[0].equals("normalEps"))
						normalEps = new Double(tokens[1]);
					if (tokens[0].equals("bailout"))
						bailout = new Double(tokens[1]);
					if (tokens[0].equals("accuracy"))
						accuracy = new Double(tokens[1]);
					if (tokens[0].equals("cascadeLevel"))
						cascadeLevel = new Integer(tokens[1]);
					break;
			}
		}

		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in "
				+ "Mandelbulb-Definition.");
		throw new Exception();
	}

	private double evalAtPoint(Vec3 hitpoint)
	{
		/*
		if (hitpoint.y > 0.0 && hitpoint.x > 0.0)
			return bailout + 1;
		*/

		double zx = 0;
		double zy = 0;
		double zz = 0;

		double cx = 0;
		double cy = 0;
		double cz = 0;

		double r = 0;
		double theta = 0;
		double phi = 0;

		double rPow = 0;
		double sinThe, cosThe, sinPhi, cosPhi;
		double sinThe2, cosThe2, sinPhi2, cosPhi2;
		double zx2, zy2, zz2;
		double planeXY;
		double minEps = 1e-14, rEpsed;

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

		r = 0;
		for (int n = 0; n < nmax; n++)
		{
			// Potenzen vorberechnen
			zx2 = zx * zx;
			zy2 = zy * zy;
			zz2 = zz * zz;

			// Testen, ob die Folge divergieren wird.
			r = Math.sqrt(zx2 + zy2 + zz2);
			if (r >= bailout)
			{
				if (debug) System.err.println("\tr = " + r);
				return r;
			}

			// Neu: Cascade - Selbst hergeleitet anhand der
			// "Doppelwinkelfunktionen" -- die Idee kam aber von Deltor!
			// Berechne über Trigonometrie direkt Sinus und Cosinus der
			// Winkel anstatt die Winkel als solche zu bestimmen.
			// -------------------------------------------------

			// Die Epsilons hier sind wichtig, damit keine Div durch 0.
			planeXY = Math.sqrt(zx2 + zy2) + minEps;
			rEpsed = r + minEps;

			// Sinus/Cosinus über Dreiecke.
			// Phi ist der horizontale Winkel, Theta der vertikale.
			sinPhi = zy / planeXY;
			cosPhi = zx / planeXY;
			sinThe = planeXY / rEpsed;
			cosThe = zz / rEpsed;

			// Bestimme "rekursiv" über Doppelwinkelfunktionen die
			// multiplizierten Winkel. Dabei kann man auch gleich das r
			// mitpotenzieren.
			rPow = r;
			for (int cascade = 0; cascade < cascadeLevel; cascade++)
			{
				sinPhi2 = 2.0 * sinPhi * cosPhi;
				cosPhi2 = 2.0 * cosPhi * cosPhi - 1.0;

				sinThe2 = 2.0 * sinThe * cosThe;
				cosThe2 = 2.0 * cosThe * cosThe - 1.0;

				sinPhi = sinPhi2;
				cosPhi = cosPhi2;
				sinThe = sinThe2;
				cosThe = cosThe2;

				rPow *= rPow;
			}
			// -------------------------------------------------

			// Neue Werte setzen:
			zx = rPow * sinThe * cosPhi  +  cx;
			zy = rPow * sinThe * sinPhi  +  cy;
			zz = rPow * cosThe           +  cz;
		}

		if (debug) System.err.println("\tr = " + r);
		return r;
	}

	private Vec3 normalAtPoint(Vec3 hitpoint)
	{
		double xr, xl, yr, yl, zr, zl;

		xl = evalAtPoint(new Vec3(
			hitpoint.x + normalEps,
			hitpoint.y,
			hitpoint.z));
		xr = evalAtPoint(new Vec3(
			hitpoint.x - normalEps,
			hitpoint.y,
			hitpoint.z));

		yl = evalAtPoint(new Vec3(
			hitpoint.x,
			hitpoint.y + normalEps,
			hitpoint.z));
		yr = evalAtPoint(new Vec3(
			hitpoint.x,
			hitpoint.y - normalEps,
			hitpoint.z));

		zl = evalAtPoint(new Vec3(
			hitpoint.x,
			hitpoint.y,
			hitpoint.z + normalEps));
		zr = evalAtPoint(new Vec3(
			hitpoint.x,
			hitpoint.y,
			hitpoint.z - normalEps));

		return new Vec3(xl - xr, yl - yr, zl - zr).normalized();
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

		double alpha = alpha0arr[0] + firststep;

		if (debug) System.err.println("New ray");

		// Bisektion: Anfangssituation merken!
		boolean sitStart = (evalAtPoint(ray.evaluate(alpha)) < bailout);
		if (debug) System.err.println("\t" + "sitStart = " + sitStart);
		boolean sitNow = false;
		double cstep = step;

		// Wandere am Strahl entlang, aber nur innerhalb der Box
		while (alpha < alpha0arr[1])
		{
			// Hole den aktuellen Punkt ...
			Vec3 hitpoint = ray.evaluate(alpha);

			// Schau dir die Situation an diesem Punkt an.
			sitNow = (evalAtPoint(hitpoint) < bailout);

			// Hat sie sich verändert? Dann starte Bisektion.
			if (sitNow != sitStart)
			{
				if (debug) System.err.println("\t* Bisecting. "
						+ "sitNow = " + sitNow);

				double a1 = alpha - cstep, a2 = alpha;

				while (cstep > accuracy)
				{
					// Gehe zum Mittelpunkt des aktuellen Stückes und
					// sample dort.
					cstep *= 0.5;
					alpha = a1 + cstep;

					hitpoint = ray.evaluate(alpha);
					sitNow = (evalAtPoint(hitpoint) < bailout);

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
