import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Mandelblub-Fraktale.
 * http://www.skytopia.com/project/fractal/2mandelbulb.html
 */
public class Mandelbulb implements Object3D, RenderingPrimitive
{
	private AABB cachedAABB = null;
	private double step = 0.01;

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
					break;
			}
		}

		// Unerwartetes Ende
		System.err.println("Fehler, unerwartetes Ende in "
				+ "Mandelbulb-Definition.");
		throw new Exception();
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

		double alpha = alpha0arr[0] + 0.01;

		// Wandere am Strahl entlang, aber nur innerhalb der Box
		while (alpha < alpha0arr[1])
		{
			// Hole den aktuellen Punkt ...
			Vec3 hitpoint = r.evaluate(alpha);

			alpha += step;
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
