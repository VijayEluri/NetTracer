import java.io.*;
import java.io.Serializable;

/**
 * "High-performance" TIFF-Writer, der Speicher spart und für sehr große
 * Bilder genutzt werden sollte.
 */
public class TIFFWriter implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public static final int SIZE_HEADER = 8;
	public static final int IFD_ENTRIES = 8;
	public static final int SIZE_IFD    = 2 + IFD_ENTRIES * 12 + 4;
	public static final int IMAGE_START = SIZE_HEADER + SIZE_IFD + 8 + 32;

	/**
	 * Write the image to the file. It'll be uncompressed.
	 */
	public static void writeRGBImage(Scene s, File f)
		throws IOException
	{
		System.out.println("Speichere nach " + f + " ...");

		TIFFWriter stream = new TIFFWriter(f,
				s.pixels[0].length, s.pixels.length);
		stream.seek(0);
		stream.writeData(s.pixels);
		stream.close();

		System.out.println("Gespeichert.");
	}

	/**
	 * Write the image to the file. It'll be uncompressed.
	 */
	public static void writeRGBImage(short[][] s, File f)
		throws IOException
	{
		System.out.println("Speichere nach " + f + " ...");

		TIFFWriter stream = new TIFFWriter(f,
				s[0].length / 3, s.length);
		stream.seek(0);
		stream.writeData(s);
		stream.close();

		System.out.println("Gespeichert.");
	}

	private FileOutputStream fos = null;
	private DataOutputStream dos = null;
	private BufferedOutputStream bos = null;
	private int w = 0;
	private int h = 0;

	/**
	 * Create a new TIFF-Streamer.
	 */
	public TIFFWriter(File f, int w, int h) throws IOException
	{
		this.w = w;
		this.h = h;

		// Try to open the file
		fos = new FileOutputStream(f);
		dos = new DataOutputStream(fos);
		bos = new BufferedOutputStream(fos);

		// Init the target: Write the header.
		writeHeader();
	}

	/**
	 * Seek to the given position relative to image start.
	 */
	public void seek(long pos) throws IOException
	{
		fos.getChannel().position(IMAGE_START + pos);
	}

	/**
	 * Close the file stream.
	 */
	public void close() throws IOException
	{
		fos.close();
	}

	/**
	 * Internal use: Write TIFF header.
	 */
	private void writeHeader() throws IOException
	{
		// First thing to do: Write the header
		// -----------------------------------

		// BigEndian and Magic Number
		dos.writeInt(0x4D4D002A);

		// First IFD (directly after header)
		dos.writeInt(SIZE_HEADER);

		// Write IFD
		// ---------

		// Number of entries entries
		dos.writeShort(IFD_ENTRIES);

		// Tags: Width and Height
		dos.writeInt(0x01000004);
		dos.writeInt(0x00000001);
		dos.writeInt(w);

		dos.writeInt(0x01010004);
		dos.writeInt(0x00000001);
		dos.writeInt(h);

		// BitsPerSample: 3 * VALUE per Channel
		dos.writeInt(0x01020003);
		dos.writeInt(0x00000003);
		dos.writeInt(SIZE_HEADER + SIZE_IFD);

		// PhotometricInterpretation: RGB
		dos.writeInt(0x01060003);
		dos.writeInt(0x00000001);
		dos.writeInt(0x00020000);

		// StripOffsets, i.e. beginning of the picture
		dos.writeInt(0x01110004);
		dos.writeInt(0x00000001);
		dos.writeInt(IMAGE_START);

		// SamplesPerPixel
		dos.writeInt(0x01150003);
		dos.writeInt(0x00000001);
		dos.writeInt(0x00030000);

		// RowsPerStrip: All rows in one strip
		dos.writeInt(0x01160004);
		dos.writeInt(0x00000001);
		dos.writeInt(h);

		// StripByteCounts: All image data
		dos.writeInt(0x01170004);
		dos.writeInt(0x00000001);
		dos.writeInt(w * h * 3);

		// End of IFD
		dos.writeInt(0x00000000);

		// VALUE for BitsPerSample
		dos.writeInt(0x00080008);
		dos.writeInt(0x00080000);

		// Padding
		for (int i = 0; i < 8; i++)
			dos.writeInt(0);
	}

	/**
	 * Write data of an image (may be partial) using a buffered stream
	 * but don't exceed the given limit.
	 */
	public void writeData(RGBColor[][] pixels) throws IOException
	{
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				pixels[y][x].normalize();
				bos.write(RGBColor.toInteger(pixels[y][x].r));
				bos.write(RGBColor.toInteger(pixels[y][x].g));
				bos.write(RGBColor.toInteger(pixels[y][x].b));
			}
		}
		bos.flush();
	}

	/**
	 * Write data of an image (may be partial) using a buffered stream
	 * but don't exceed the given limit.
	 */
	public void writeData(short[][] pixels) throws IOException
	{
		for (int y = 0; y < pixels.length; y++)
		{
			for (int x = 0; x < pixels[0].length; x++)
			{
				bos.write(pixels[y][x]);
			}
		}
		bos.flush();
	}
}
