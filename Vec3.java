import java.util.Random;
import java.io.Serializable;

/**
 * Selbsterklärende Klasse mit Vektor-Operationen
 */
public class Vec3 implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public double x, y, z;

	public Vec3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3(Vec3 other)
	{
		this(other.x, other.y, other.z);
	}

	public Vec3()
	{
		this(0, 0, 0);
	}

	/**
	 * Nur damit via i über die Achsen iteriert werden kann.
	 */
	public double getAxis(int which)
	{
		switch (which)
		{
			case 0:
				return x;
			case 1:
				return y;
			case 2:
				return z;
			default:
				return 0.0;
		}
	}

	/**
	 * Nur damit via i über die Achsen iteriert werden kann.
	 */
	public void setAxis(int which, double val)
	{
		switch (which)
		{
			case 0:
				x = val;
				break;
			case 1:
				y = val;
				break;
			case 2:
				z = val;
				break;
		}
	}

	public void add(Vec3 other)
	{
		x += other.x;
		y += other.y;
		z += other.z;
	}

	public void subtract(Vec3 other)
	{
		x -= other.x;
		y -= other.y;
		z -= other.z;
	}

	public Vec3 plus(Vec3 other)
	{
		return new Vec3(x + other.x, y + other.y, z + other.z);
	}

	public Vec3 minus(Vec3 other)
	{
		return new Vec3(x - other.x, y - other.y, z - other.z);
	}

	public void scale(double a)
	{
		x *= a;
		y *= a;
		z *= a;
	}

	public Vec3 times(double a)
	{
		Vec3 out = new Vec3(this);
		out.scale(a);
		return out;
	}

	public double dot(Vec3 other)
	{
		return x * other.x + y * other.y + z * other.z;
	}

	public Vec3 cross(Vec3 other)
	{
		return new Vec3(
							y * other.z - z * other.y,
							z * other.x - x * other.z,
							x * other.y - y * other.x
						);
	}

	public double length()
	{
		return Math.sqrt(dot(this));
	}

	public double lengthSquared()
	{
		return dot(this);
	}

	public void normalize()
	{
		scale(1.0 / length());
	}

	public Vec3 normalized()
	{
		return times(1.0 / length());
	}

	public Vec3 jittered(double epsilon, Random rGen)
	{
		// nextGaussian() sieht besser aus, ist aber definitiv langsamer.
		return new Vec3(this.x + (rGen.nextGaussian() * 0.5) * epsilon,
						this.y + (rGen.nextGaussian() * 0.5) * epsilon,
						this.z + (rGen.nextGaussian() * 0.5) * epsilon
						);
		/*
		return new Vec3(this.x + (Math.random() - 0.5) * epsilon,
						this.y + (Math.random() - 0.5) * epsilon,
						this.z + (Math.random() - 0.5) * epsilon
						);
		*/
	}

	public String toString()
	{
		return "Vec3(" + x + ", " + y + ", " + z + ")";
	}
}
