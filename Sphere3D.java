import java.util.List;
import java.io.Serializable;

/**
 * Eine perfekte Kugel.
 * 
 * Ist gleichzeitig Object3D und Primitiv, da sie ja kein Mesh enthält
 */
public class Sphere3D implements Object3D, RenderingPrimitive, Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private Material mat;
	private double radius = 1.0;
	private double radius2;
	private Vec3 origin = new Vec3();

	private RenderingPrimitive[] prims;

	private AABB cachedAABB = null;

	/**
	 * Kugel aus Scanner laden
	 */
	public Sphere3D(SceneReader in, List<Material> mats) throws Exception
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
						// Vorarbeit
						prims = new RenderingPrimitive[] {this};
						radius2 = radius*radius;

						System.out.println("Sphere3D:");
						System.out.println("mat: " + mat);
						System.out.println("radius: " + radius);
						System.out.println("origin: " + origin);
						System.out.println();
						return;
					}
					break;

				case 2:
					if (tokens[0].equals("radius"))
						radius = new Double(tokens[1]);

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

				case 4:
					if (tokens[0].equals("origin"))
						origin = new Vec3(
											new Double(tokens[1]),
											new Double(tokens[2]),
											new Double(tokens[3])
											);
					break;
			}
		}

		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in Sphere3D-Definition.");
		throw new Exception();
	}

	/**
	 * Schnitttest durchführen
	 */
	public Intersection intersectionTest(Ray r)
	{
		// Wo ist der Punkt auf dem Strahl, der am nächsten an mir liegt?
		double alpha = -r.direction.dot(r.origin.minus(this.origin));
		Vec3 q = r.evaluate(alpha);

		// Abstand zum Kugelmittelpunkt?
		q.subtract(this.origin);
		double distToCenter2 = q.lengthSquared();

		if (distToCenter2 > radius2)
			return null;

		// Über Pythagoras zu den beiden Schnittpunkten
		double a = Math.sqrt(radius2 - distToCenter2);

		// Welcher von beiden liegt näher am Ray-Ursprung?
		double dist = 0.0;
		if (alpha >= a)
		{
			dist = alpha - a;
		}
		else if (alpha + a > 0)
		{
			dist = alpha + a;
		}
		else
		{
			return null;
		}

		q = r.evaluate(dist);

		// Normale an diesem Punkt
		Vec3 n = q.minus(origin);
		n.normalize();

		Vec3 colpos = q;
		if (mat instanceof TextureMaterial)
		{
			// Zitat von Math.atan2():
			//   Returns the angle theta from the conversion of
			//   rectangular coordinates (x, y) to polar coordinates
			//   (r, theta). This method computes the phase theta by
			//   computing an arc tangent of y/x in the range of -pi
			//   to pi.

			// Wenn man von oben auf die Kugel schaut, geben die X- und
			// Z-Anteile der Normale gerade den Längengrad an. Stellt man
			// sich die Textur der Kugel aufgeschnitten vor und betrachtet
			// alles vorerst nur von oben, dann gäbe der Winkel zwischen
			// nulltem Längengrad und dem aktuell durch die Normale
			// definierten Längengrad an, wie weit man auf der Texturmap
			// in X-Richtung laufen muss. Genau das wird hier für die X-
			// Koordinate von "colpos" berechnet (und dann ins Intervall
			// 0 bis 1 gebracht).

			// Der Y-Wert von "colpos" berechnet sich einfach aus dem
			// Arkussinus des Y-Wertes der Normale. Das Ergebnis davon
			// liegt in -pi/2 bis +pi/2 und wird dann auch wieder ins
			// Intervall 0 bis 1 gebracht. (Man denke sich hier ein
			// Dreieck innerhalb der Kugel mit "Höhe" Y.)

			// Siehe auch: http://www.cse.msu.edu/~cse872/tutorial4.html

			colpos = new Vec3(Math.atan2(n.x, n.z) / (2.0 * Math.PI) + 0.5,
							  Math.asin(n.y) / Math.PI + 0.5,
							  0.0);
		}

		return new Intersection(this, q, n, dist, mat,
								mat.getDiffuseColor(colpos),
								mat.getSpecularColor(colpos),
								mat.getTransparentColor(colpos));
	}

	public AABB getAABB()
	{
		// AABB wird gecached, da sonst der Tree-Bau Jahrhunderte benötigt
		if (cachedAABB == null)
		{
			cachedAABB = new AABB(new Vec3(origin), new Vec3(radius, radius, radius));
		}

		return cachedAABB;
	}

	public RenderingPrimitive[] getRenderingPrimitives()
	{
		return prims;
	}
}
