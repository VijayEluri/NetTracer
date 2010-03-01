import java.io.Serializable;

/**
 * Ein einfaches Dreieck
 */
public class RenderingTriangle implements RenderingPrimitive, Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private Material mat;
	private Vec3[] vertices = null;
	private Vec3[] normals = null;
	private Vec3[] tex = null;

	private boolean smooth;

	// Fest vorberechnete Werte
	private Vec3 n;
	private Vec3 uBeta;
	private Vec3 uGamma;
	private double d;

	private AABB cachedAABB = null;

	/**
	 * Erstellt ein neues Dreieck mit diesen Eckpunkten und Material,
	 * sowie den gegebenen Vertex-Normalen.
	 */
	public RenderingTriangle(Vec3[] vertices, Vec3[] normals, Vec3[] tex,
							Material mat, boolean smooth)
	{
		this.vertices = vertices;
		this.normals = normals;
		this.mat = mat;
		this.smooth = smooth;
		this.tex = tex;

		// Wenn keine Normalen übergeben wurden, dann forciere flat shading
		if (normals == null)
			smooth = false;

		// Vorarbeit, die einmal erledigt werden kann
		Vec3 e1 = vertices[1].minus(vertices[0]);
		Vec3 e2 = vertices[2].minus(vertices[0]);

		// Ebenenparameter
		n = e1.cross(e2);
		n.normalize();
		d = n.dot(vertices[0]);

		// Baryzentrische Vorarbeit, siehe auch:
		// http://www.blackpawn.com/texts/pointinpoly/default.html
		double a = e1.dot(e1);
		double b = e1.dot(e2);
		double c = e2.dot(e2);

		double D = a * c - b*b;
		double A = a / D;
		double B = b / D;
		double C = c / D;

		uBeta  = e1.times(C).minus(e2.times(B));
		uGamma = e2.times(A).minus(e1.times(B));
	}

	/**
	 * Führt den konrekten Schnitttest mit diesem Strahl durch
	 */
	public Intersection intersectionTest(Ray r)
	{
		// Schnitttest Ray -> Ebene
		double c = r.direction.dot(n);
		if (c == 0.0)
			return null;

		// Wie weit hat es der Ray von seinem Ursprung zum Schnittpunkt?
		double alpha1 = (d - r.origin.dot(n)) / c;
		if (alpha1 <= 0.0)
			return null;

		Vec3 q = r.evaluate(alpha1);
		Vec3 q2 = q.minus(vertices[0]);

		// Schnittpunkt q mit Ebene gefunden, liegt der im Dreieck?
		double beta = uBeta.dot(q2);
		if (beta < 0.0)
			return null;

		double gamma = uGamma.dot(q2);
		if (gamma < 0.0)
			return null;

		double alpha = 1 - beta - gamma;
		if (alpha < 0.0)
			return null;

		// Texturiert?
		Vec3 colpos = q;
		if (tex != null && mat instanceof TextureMaterial)
		{
			// Schnappe dir über die baryzentrischen Koordinaten die
			// interpolierte Position auf der Textur.

			// Das Anlegen neuer Objekte nach Möglichkeit vermeiden.
			// Einmal muss es aber neu angelegt werden, da sonst q
			// überschrieben würde.

			colpos = tex[0].times(alpha);
			/*
			colpos.add(tex[1].times(beta));
			colpos.add(tex[2].times(gamma));
			*/

			colpos.x += tex[1].x * beta;
			colpos.y += tex[1].y * beta;
			colpos.z += tex[1].z * beta;

			colpos.x += tex[2].x * gamma;
			colpos.y += tex[2].y * gamma;
			colpos.z += tex[2].z * gamma;
		}

		// Smooth oder flat shading?
		if (smooth)
		{
			// Interpoliere ganz einfach mit den baryzentrischen
			// Koordinaten die Normalen an den Eckpunkten.

			Vec3 smoothed = normals[0].times(alpha);

			/*
			smoothed.add(normals[1].times(beta));
			smoothed.add(normals[2].times(gamma));
			smoothed.normalize();
			*/

			smoothed.x += normals[1].x * beta;
			smoothed.y += normals[1].y * beta;
			smoothed.z += normals[1].z * beta;

			smoothed.x += normals[2].x * gamma;
			smoothed.y += normals[2].y * gamma;
			smoothed.z += normals[2].z * gamma;

			smoothed.normalize();

			return new Intersection(this, q, smoothed, alpha1,
									mat, mat.getDiffuseColor(colpos),
									mat.getSpecularColor(colpos),
									mat.getTransparentColor(colpos));
		}
		else
			return new Intersection(this, q, new Vec3(n), alpha1,
									mat, mat.getDiffuseColor(colpos),
									mat.getSpecularColor(colpos),
									mat.getTransparentColor(colpos));
	}

	public AABB getAABB()
	{
		// AABB wird gecached, da sonst der Tree-Bau Jahrhunderte benötigt
		if (cachedAABB == null)
		{
			// TODO: Kosmetik statt +/- 1e100
			Vec3 min = new Vec3(1e100, 1e100, 1e100);
			Vec3 max = new Vec3(-1e100, -1e100, -1e100);

			for (int i = 0; i < 3; i++)
			{
				for (int a = 0; a < 3; a++)
				{
					if (vertices[i].getAxis(a) < min.getAxis(a))
						min.setAxis(a, vertices[i].getAxis(a));
					if (vertices[i].getAxis(a) > max.getAxis(a))
						max.setAxis(a, vertices[i].getAxis(a));
				}
			}

			Vec3 center = max.plus(min).times(0.5);
			Vec3 radii = max.minus(min).times(0.5);

			cachedAABB = new AABB(center, radii);
		}

		return cachedAABB;
	}
}
