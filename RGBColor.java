import java.io.Serializable;

/**
 * Die AWT-Color-Klasse ist ziemlich beschränkt und außerdem nur im
 * RGB-Farbraum, was für uns natürlich nutzlos ist. Daher hier ein paar
 * grundlegende Operationen mit 32 Bit pro Kanal.
 * 
 * Noch kein Alpha-Kanal.
 */
public class RGBColor implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public double r, g, b;
	protected int samples = 1;

	public static RGBColor black()      { return new RGBColor(0.0, 0.0, 0.0); }
	public static RGBColor white()      { return new RGBColor(1.0, 1.0, 1.0); }
	public static RGBColor grey()       { return new RGBColor(0.7, 0.7, 0.7); }
	public static RGBColor red()        { return new RGBColor(1.0, 0.0, 0.0); }
	public static RGBColor green()      { return new RGBColor(0.0, 1.0, 0.0); }
	public static RGBColor blue()       { return new RGBColor(0.0, 0.0, 1.0); }
	public static RGBColor pink()       { return new RGBColor(1.0, 0.0, 1.0); }
	public static RGBColor darkred()    { return new RGBColor(0.5, 0.0, 0.0); }
	public static RGBColor darkgreen()  { return new RGBColor(0.0, 0.5, 0.0); }
	public static RGBColor darkblue()   { return new RGBColor(0.0, 0.0, 0.5); }
	public static RGBColor lightred()   { return new RGBColor(1.0, 0.5, 0.5); }
	public static RGBColor lightgreen() { return new RGBColor(0.5, 1.0, 0.5); }
	public static RGBColor lightblue()  { return new RGBColor(0.5, 0.5, 1.0); }

	public static RGBColor random() { return new RGBColor(Math.random(),
														  Math.random(),
														  Math.random()); }

	public static RGBColor empty()
	{
		RGBColor out = RGBColor.black();
		out.samples = 0;
		return out;
	}

	public RGBColor(double r, double g, double b)
	{
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public RGBColor(RGBColor other)
	{
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
	}

	public RGBColor(int rgb)
	{
		this.r = (double)((rgb & 0x00FF0000) >> 16) / 255.0;
		this.g = (double)((rgb & 0x0000FF00) >> 8) / 255.0;
		this.b = (double)(rgb & 0x000000FF) / 255.0;
	}

	public RGBColor()
	{
		this(0.0, 0.0, 0.0);
	}

	public void add(RGBColor other)
	{
		this.r += other.r;
		this.g += other.g;
		this.b += other.b;
	}

	public void addSample(RGBColor other)
	{
		add(other);
		samples++;
	}

	public void addAllSamples(RGBColor other)
	{
		add(other);
		samples += other.samples;
	}

	public void scale(double a)
	{
		this.r *= a;
		this.g *= a;
		this.b *= a;
	}

	public void multiply(RGBColor other)
	{
		this.r *= other.r;
		this.g *= other.g;
		this.b *= other.b;
	}

	public RGBColor plus(RGBColor other)
	{
		return new RGBColor(this.r + other.r, this.g + other.g, this.b + other.b);
	}

	public RGBColor times(double a)
	{
		return new RGBColor(this.r * a, this.g * a, this.b * a);
	}

	public RGBColor product(RGBColor other)
	{
		return new RGBColor(this.r * other.r, this.g * other.g, this.b * other.b);
	}

	public void normalize()
	{
		r /= (double)samples;
		g /= (double)samples;
		b /= (double)samples;
		samples = 1;
	}

	/**
	 * In einen int in Javas normalem Farbmodell umwandeln. Ein int ist
	 * 32 Bit groß, davon von MSB nach LSB:
	 * - 8 Bit Alpha
	 * - 8 Bit Rot
	 * - 8 Bit Grün
	 * - 8 Bit Blau
	 * Der Alphakanal spielt bei uns erstmal keine Rolle. Die Intensitäten
	 * werden dabei in den Bereich 0 - 255 geclipped.
	 */
	public int toRGB()
	{
		return 0xFF000000
			+ (toInteger(r / samples) << 16)
			+ (toInteger(g / samples) << 8)
			+ toInteger(b / samples);
	}

	public static int toInteger(double c)
	{
		int out = (int)(c * 255.0);

		if (out < 0)
			out = 0;
		else if (out > 255)
			out = 255;

		return out;
	}

	/**
	 * Legt a über b mit dem entsprechenden Alphawert von a.
	 */
	public static int overlayARGB(int a, int b)
	{
		// Java kennt keine unsigned Typen, also hole die linkesten 8
		// Byte, schiebe sie ganz nach rechts und verwirf dann alles
		// außer diesen 8 Byte. Das eliminiert das Vorzeichen.
		double alpha = (double)(((a & 0xFF000000) >> 24) & 0x000000FF) / 255.0;

		// Leg' jetzt a über b. Kanäle dabei getrennt, damit garantiert
		// bei eventuellen Rundungsfehlern nichts "rüberschwappt".
		int R = (int)( (double)((a & 0x00FF0000) >> 16) * alpha + (double)((b & 0x00FF0000) >> 16) * (1.0 - alpha) );
		int G = (int)( (double)((a & 0x0000FF00) >>  8) * alpha + (double)((b & 0x0000FF00) >>  8) * (1.0 - alpha) );
		int B = (int)( (double)((a & 0x000000FF)      ) * alpha + (double)((b & 0x000000FF)      ) * (1.0 - alpha) );

		// Eventuelle Rundungsfehler ausgleichen
		if (R < 0)
			R = 0;
		else if (R > 255)
			R = 255;

		if (G < 0)
			G = 0;
		else if (G > 255)
			G = 255;

		if (B < 0)
			B = 0;
		else if (B > 255)
			B = 255;

		// Wieder zusammensetzen und raus damit
		return 0xFF000000 + (R << 16) + (G << 8) + B;
	}

	/**
	 * Entscheidet, ob diese Farbe mit der gegebenen Toleranz von der
	 * anderen Farbe abweicht.
	 */
	public boolean differs(RGBColor o, double tol)
	{
		return (Math.abs(r - o.r) > tol)
			|| (Math.abs(g - o.g) > tol)
			|| (Math.abs(b - o.b) > tol);
	}

	public String toString()
	{
		return "RGBColor(" + r + ", " + g + ", " + b + ")";
	}
}
