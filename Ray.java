import java.io.Serializable;

/**
 * Ein einzelner Ray.
 */
public class Ray implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public static final double corrEps = 1e-7;

	public Vec3 origin;
	public Vec3 direction;

	public Vec3 reciDir;

	/**
	 * Erzeugt einen neuen Ray an dieser Position mit dieser Richtung
	 */
	public Ray(Vec3 origin, Vec3 direction)
	{
		this.origin = new Vec3(origin);
		this.direction = direction;
		this.direction.normalize();

		// Um ein kleines Epsilon schubsen - das ist nötig, um
		// Rundungsfehler auszugleichen
		this.origin.x += (this.direction.x * corrEps);
		this.origin.y += (this.direction.y * corrEps);
		this.origin.z += (this.direction.z * corrEps);

		// Kehrwerte für schnelleren AABB-Ray-Test
		// Keine Angst vor Division durch 0: Dann kommt +/- infinity raus,
		// was auch dort im Test für den Größenvergleich benötigt wird.
		reciDir = new Vec3(1.0 / direction.x, 1.0 / direction.y, 1.0 / direction.z);
	}

	/**
	 * Gibt den konkreten Ort zurück, der sich "alpha" weit vom Ray-
	 * Ursprung entfernt befindet.
	 */
	public Vec3 evaluate(double alpha)
	{
		Vec3 out = new Vec3(origin);
		out.x += (direction.x * alpha);
		out.y += (direction.y * alpha);
		out.z += (direction.z * alpha);
		return out;
	}

	public String toString()
	{
		return "Ray(new " + origin + ", new " + direction + ")";
	}
}
