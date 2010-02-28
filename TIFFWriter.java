import java.io.*;

public class TIFFWriter
{
	private Scene scene = null;

	public TIFFWriter(Scene which)
	{
		this.scene = which;
	}

	public void save(File target)
	{
		System.out.println("Speichere nach " + target + " ...");
		System.out.println(scene.pixels.length + "x" + scene.pixels[0].length);
	}
}
