import java.io.Serializable;

/**
 * Irgendein Licht. Welches genau, ist für die weitere Verarbeitung
 * grundsätzlich egal.
 */
public abstract class Light implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public Vec3 origin;
	public double intensity = 1.0;
	public double decayRate = 0.1;
	public RGBColor color;

	/**
	 * Gibt die Intensität, die dieses Licht auf diesem Material erzeugt,
	 * zurück.
	 * 
	 * Das Ergebnis wird in li[] geschrieben:
	 * li[0] ist der diffuse Anteil
	 * li[1] ist der "spekulare" Anteil (shininess)
	 */
	public abstract void lighting(Material mat, Vec3 L, Vec3 R, 
									Vec3 N, double[] li);
}
