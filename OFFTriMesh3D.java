import java.io.*;
import java.util.*;
import java.io.Serializable;

/**
 * Einfacher OFF-Reader, kann nur TriMeshes verarbeiten
 */
public class OFFTriMesh3D implements Object3D, Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private Vec3 origin = new Vec3(0.0, 0.0, 0.0);
	private File infile = null;
	private Material mat;
	private RenderingPrimitive[] out;

	/**
	 * OFF aus Scanner laden
	 */
	public OFFTriMesh3D(SceneReader in, List<Material> mats) throws Exception
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
						if (!doLoad())
							throw new Exception();
						dump();
						return;
					}
					break;

				case 2:
					if (tokens[0].equals("file"))
						infile = in.getRelativePath(tokens[1]);

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
		System.err.println("Fehler, unerwartetes Ende in External3D-Definition.");
		throw new Exception();
	}

	/**
	 * Leicht antiquierte Funktion, um das eigentliche Mesh zu laden
	 */
	private boolean doLoad()
	{
		try
		{
			Scanner scan = new Scanner(infile);
			scan.useLocale(java.util.Locale.US); // Punkt statt Komma...

			scan.next();

			int numVertices = scan.nextInt();
			int numFaces = scan.nextInt();
			int dummy = scan.nextInt();

			Vec3[] verts = new Vec3[numVertices];

			for (int i = 0; i < numVertices; i++)
			{
				verts[i] = new Vec3(scan.nextDouble(), scan.nextDouble(), scan.nextDouble());
			}

			out = new RenderingTriangle[numFaces];

			for (int i = 0; i < numFaces; i++)
			{
				Vec3[] theVerts = new Vec3[3];

				// vertnum, immer 3 hier
				dummy = scan.nextInt();
				if (dummy != 3)
				{
					System.err.println("FEHLER! Das ist kein TriMesh!");
				}

				for (int k = 0; k < 3; k++)
				{
					theVerts[k] = verts[scan.nextInt()].plus(origin);
				}

				out[i] = new RenderingTriangle(theVerts, null, null, mat, false);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return true;
	}

	public RenderingPrimitive[] getRenderingPrimitives()
	{
		return out;
	}

	private void dump()
	{
		System.out.println("OFFTriMesh3D:");
		System.out.println("mat: " + mat);
		System.out.println("sourcepath: " + infile.getAbsolutePath());
		System.out.println("origin: " + origin);
		System.out.println("triangles: " + out.length);
		System.out.println();
	}
}
