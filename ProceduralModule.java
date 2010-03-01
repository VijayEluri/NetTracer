import java.util.Random;
import java.io.Serializable;

public interface ProceduralModule
{
	// Interface
	// ---------
	public RGBColor getColor(Vec3 p);
	public double getValue(Vec3 p);
	public String toString();


	// Ein paar Module
	// ---------------

	// 3D-Checker
	public class Checker implements ProceduralModule, Serializable
	{
		private static final long serialVersionUID = 20100301001L;

		private RGBColor color1, color2;
		private Vec3 offset = new Vec3();
		private double scale = 1.0;

		public String toString() { return "Checker"; }

		public RGBColor getColor(Vec3 p)
		{
			double c = calc(p);
			return color1.times(c).plus(color2.times(1 - c));
		}

		public double getValue(Vec3 p)
		{
			return calc(p);
		}

		public Checker(SceneReader in) throws Exception
		{
			String[] tokens = null;
			while ((tokens = in.getNextTokens()) != null)
			{
				switch (tokens.length)
				{
					case 1:
						if (tokens[0].equals("end"))
						{
							if (color1 == null)
								color1 = RGBColor.black();
							if (color2 == null)
								color2 = RGBColor.white();
							return;
						}
						break;
					case 2:
						if (tokens[0].equals("scale"))
							scale = new Double(tokens[1]);
						break;
					case 4:
						if (tokens[0].equals("color1"))
							color1 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						if (tokens[0].equals("color2"))
							color2 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						if (tokens[0].equals("offset"))
							offset = new Vec3(new Double(tokens[1]),
											  new Double(tokens[2]),
											  new Double(tokens[3]));
						break;
				}
			}
			System.err.println("Fehler, unerwartetes Ende in Moduldefinition eines Checkers.");
			throw new Exception();
		}

		private double calc(Vec3 in)
		{
			Vec3 p = in.plus(offset);
			p.scale(scale);

			double a = 0, b = 0, c = 0;
			// Bilde erstmal die Koordinaten auf -1, 0 oder 1 ab
			a = (int)(p.x % 2);
			b = (int)(p.y % 2);
			c = (int)(p.z % 2);

			if (b == 0)
			{
				// Betrachte -a und spiegle, falls Vorzeichen
				// unterschiedlich
				if (p.x * p.z < 0)
					c = (-a == c ? 1.0 : 0.0);
				else
					c = (a == c ? 0.0 : 1.0);
			}
			else
			{
				// Durch Entscheidung "b = 0?" entsteht zusätzlich
				// ein Schachbrett in der Höhe, wenn hier das
				// Muster wieder gespiegelt wird, sprich mit steigender
				// Höhe wird immer wieder mal gespiegelt
				if (p.x * p.z < 0)
					c = (-a == c ? 0.0 : 1.0);
				else
					c = (a == c ? 1.0 : 0.0);
			}

			// Die untere Y-Hälfte muss nochmal global gespiegelt
			// werden, damit keine Naht entsteht
			if (p.y < 0)
			{
				c = (c == 0.0 ? 1.0 : 0.0);
			}

			return c;
		}
	}

	// Einfach was Buntes
	public class Noise implements ProceduralModule, Serializable
	{
		private static final long serialVersionUID = 20100301001L;

		private RGBColor color1, color2, color3;
		private double amplify = 1.0;
		private static Random rGen = new Random();

		public String toString() { return "Noise"; }

		public RGBColor getColor(Vec3 p)
		{
			RGBColor out = color1.times((rGen.nextGaussian() + 1.0) / 2.0);
			out.add(color2.times((rGen.nextGaussian() + 1.0) / 2.0));
			out.add(color3.times((rGen.nextGaussian() + 1.0) / 2.0));
			out.scale(amplify / 3.0);
			return out;
		}

		public double getValue(Vec3 p)
		{
			return (rGen.nextGaussian() + 1.0) / 2.0;
		}

		public Noise(SceneReader in) throws Exception
		{
			String[] tokens = null;
			while ((tokens = in.getNextTokens()) != null)
			{
				switch (tokens.length)
				{
					case 1:
						if (tokens[0].equals("end"))
						{
							if (color1 == null)
								color1 = RGBColor.red();
							if (color2 == null)
								color2 = RGBColor.green();
							if (color3 == null)
								color3 = RGBColor.blue();
							return;
						}
						break;
					case 2:
						if (tokens[0].equals("amplify"))
							amplify = new Double(tokens[1]);
						break;
					case 4:
						if (tokens[0].equals("color1"))
							color1 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						if (tokens[0].equals("color2"))
							color2 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						if (tokens[0].equals("color3"))
							color3 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						break;
				}
			}
			System.err.println("Fehler, unerwartetes Ende in Moduldefinition eines Moduls.");
			throw new Exception();
		}
	}

	// 3D-Grid
	public class Grid implements ProceduralModule, Serializable
	{
		private static final long serialVersionUID = 20100301001L;

		private RGBColor color1, color2;
		private double width = 0.01;

		public String toString() { return "Grid"; }

		public RGBColor getColor(Vec3 p)
		{
			double c = calc(p);
			return color2.times(c).plus(color1.times(1 - c));
		}

		public double getValue(Vec3 p)
		{
			return calc(p);
		}

		public Grid(SceneReader in) throws Exception
		{
			String[] tokens = null;
			while ((tokens = in.getNextTokens()) != null)
			{
				switch (tokens.length)
				{
					case 1:
						if (tokens[0].equals("end"))
						{
							if (color1 == null)
								color1 = RGBColor.black();
							if (color2 == null)
								color2 = RGBColor.white();
							return;
						}
						break;
					case 2:
						if (tokens[0].equals("width"))
							width = new Double(tokens[1]);
						break;
					case 4:
						if (tokens[0].equals("color1"))
							color1 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						if (tokens[0].equals("color2"))
							color2 = new RGBColor(new Double(tokens[1]),
												  new Double(tokens[2]),
												  new Double(tokens[3]));
						break;
				}
			}
			System.err.println("Fehler, unerwartetes Ende in Moduldefinition eines Checkers.");
			throw new Exception();
		}

		private double calc(Vec3 p)
		{
			double a = 0, b = 0, c = 0, d = 0.5 - width;

			a = p.x - Math.rint(p.x);
			b = p.y - Math.rint(p.y);
			c = p.z - Math.rint(p.z);

			if (a < d && b < d && c < d)
				return 1.0;
			else
				return 0.0;
		}
	}

	// Der "Punkt" p wird als Farbwert übernommen. Für intelligentere
	// Objekte gedacht (ursprünglich Mandelbulb).
	public class Hatch implements ProceduralModule, Serializable
	{
		private static final long serialVersionUID = 20100301001L;

		public String toString() { return "Hatch"; }

		public RGBColor getColor(Vec3 p)
		{
			return new RGBColor(p.x, p.y, p.z);
		}

		// FIXME: Wozu gibt es diese Funktion im Interface überhaupt?
		public double getValue(Vec3 p)
		{
			return 0.5;
		}

		public Hatch(SceneReader in) throws Exception
		{
			return;
		}
	}
}
