import java.io.*;
import java.util.*;
import java.io.Serializable;

/**
 * Stark vereinfachter OBJ-Reader.
 * 
 * Objekte:
 * - Ein einzelnes Objekt aus der OBJ-File einlesen. Es wird auch voraus-
 *   gesetzt, dass sich nur eines dort drin befindet.
 * - Es wird vorausgesetzt, dass zumindest die v, vn und f Felder ge-
 *   geschrieben worden sind.
 * - Das Objekt muss extern trianguliert worden sein!
 * - Da die Normalen in OBJ-Files stehen, ist "smooth shading" möglich.
 * 
 * Texturen/Materialien:
 * - vt Felder werden gelesen und den Vertices zugeordnet.
 * - Die Texturdatei selbst muss über eine "texmat [name] ... end"-
 *   Defintion in der Szenendatei geladen werden und diesem Objekt hier
 *   zugeordnet werden. Das wirft zwar das OBJ-Konzept über den Haufen,
 *   ist aber konsistenter hier im Programm.
 * - Es wird vorausgesetzt, dass dem Objekt nur eine einzige Texturmap
 *   zugeordnet wurde.
 * - MTL-Files werden ignoriert.
 * 
 * Getestet gegen TriMeshes aus Art of Illusion und UV-Mapped-TriMeshes
 * aus Wings3D.
 */
public class OBJTriMesh3D implements Object3D, Serializable
{
	private Vec3 origin = new Vec3(0.0, 0.0, 0.0);
	private File infile = null;
	private Material mat;
	private RenderingPrimitive[] out;
	private boolean smooth = true;
	
	/**
	 * OBJ aus Scanner laden
	 */
	public OBJTriMesh3D(SceneReader in, List<Material> mats) throws Exception
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
					if (tokens[0].equals("smooth"))
						smooth = (new Integer(tokens[1]) == 1);
						
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
		System.err.println("Fehler, unerwartetes Ende in OBJTriMesh3D-Definition.");
		throw new Exception();
	}
	
	/**
	 * Leicht antiquierte Funktion, um das eigentliche Mesh zu laden
	 */
	private boolean doLoad()
	{
		try
		{
			ArrayList<Vec3> v = new ArrayList<Vec3>();
			ArrayList<Vec3> vn = new ArrayList<Vec3>();
			ArrayList<Vec3> vt = new ArrayList<Vec3>();
			ArrayList<RenderingTriangle> f = new ArrayList<RenderingTriangle>();
			
			Scanner in = new Scanner(infile);
			in.useLocale(java.util.Locale.US); // Punkt statt Komma...
			
			while (in.hasNextLine())
			{
				String line = in.nextLine().trim();
				String[] tokens = line.split(" ");
				
				if (tokens[0].equals("v"))
				{
					// Neuen Vertex einlesen von der Form:
					// 		v 0.83912 -1.14377 -1.11928
					v.add(new Vec3(new Double(tokens[1]),
									new Double(tokens[2]),
									new Double(tokens[3])).plus(origin));
				}
				
				if (tokens[0].equals("vn"))
				{
					// Neue Normale einlesen von der Form:
					// 		vn 0.99454 0.0172 0.10289
					vn.add(new Vec3(new Double(tokens[1]),
									new Double(tokens[2]),
									new Double(tokens[3])).normalized());
				}
				
				if (tokens[0].equals("vt"))
				{
					// Neue Texturkoordinate einlesen von der Form:
					// 		vt 0.33333333 0.66666667
					vt.add(new Vec3(new Double(tokens[1]),
									new Double(tokens[2]),
									0.0));
				}
				
				if (tokens[0].equals("f"))
				{
					// Neues Dreieck einlesen. Es wird angenommen, dass
					// jetzt schon die entsprechenden Vertices und
					// Normalen definiert sind. Sonst knallt's. Form:
					// 		f 611//21799 10911//21799 3904//21799
					// Also: Vertex//Normale, jeweils der Index
					
					if (tokens.length != 4)
					{
						System.err.println("FEHLER! Das ist kein TriMesh!");
						return false;
					}
					
					String p1[] = tokens[1].split("/");
					String p2[] = tokens[2].split("/");
					String p3[] = tokens[3].split("/");
					
					// Beachten, dass die Indizes in OBJ-Files bei 1
					// anfangen..........
					
					Vec3[] verts = new Vec3[] { v.get(new Integer(p1[0]) - 1),
												v.get(new Integer(p2[0]) - 1),
												v.get(new Integer(p3[0]) - 1)
											};
					Vec3[] norms = new Vec3[] { vn.get(new Integer(p1[2]) - 1),
												vn.get(new Integer(p2[2]) - 1),
												vn.get(new Integer(p3[2]) - 1)
											};
					
					Vec3[] tex = null;
					if (!p1[1].equals(""))
					{
						tex = new Vec3[] { vt.get(new Integer(p1[1]) - 1),
										   vt.get(new Integer(p2[1]) - 1),
										   vt.get(new Integer(p3[1]) - 1)
										};
					}
					
					f.add(new RenderingTriangle(verts, norms, tex, mat, smooth));
				}
			}
			
			out = f.toArray(new RenderingPrimitive[0]);
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
		System.out.println("OBJTriMesh3D:");
		System.out.println("mat: " + mat);
		System.out.println("sourcepath: " + infile.getAbsolutePath());
		System.out.println("origin: " + origin);
		System.out.println("triangles: " + out.length);
		System.out.println("smooth: " + smooth);
		System.out.println();
	}
}
