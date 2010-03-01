import java.net.*;
import java.io.*;

public class NetMaster
{
	public static Scene theScene = null;
	public static RGBColor[][] pixels = null;

	public static int bunchsize = 5;
	public static short[] tokens = null;
	public static boolean direction = false;

	public static void loadScene(String path)
	{
		theScene = new Scene();
		if (!theScene.loadScene(path))
		{
			System.err.println("Konnte Szene nicht laden.");
			System.exit(1);
		}

		// Lokalen Buffer erzeugen, der aber nur null's enthält. Die
		// Daten kommen von den Clients, wir brauchen hier keine
		// fertigen schwarzen Pixel.
		pixels = new RGBColor[theScene.set.sizeY][];
		for (int y = 0; y < theScene.set.sizeY; y++)
			pixels[y] = new RGBColor[theScene.set.sizeX];

		// Tokens. Ein Token meint <bunchsize> Zeilen.
		tokens = new short[
				(int)Math.ceil(theScene.set.sizeY / (double)bunchsize)];
	}

	/**
	 * 0 : offset
	 * 1 : rows
	 * 2 : token-index
	 */
	public static int[] getFreeJob()
	{
		int token = -1;
		synchronized (tokens)
		{
			// Von vorne oder hinten suchen?
			if (direction)
			{
				for (int i = 0; i < tokens.length; i++)
				{
					if (tokens[i] == 0)
					{
						tokens[i] = 1;
						token = i;
						break;
					}
				}
			}
			else
			{
				for (int i = tokens.length - 1; i >= 0; i--)
				{
					if (tokens[i] == 0)
					{
						tokens[i] = 1;
						token = i;
						break;
					}
				}
			}

			direction = !direction;
		}

		// Nichts mehr frei?
		if (token == -1)
			return null;

		// Von Tokens in Pixelgrenzen umrechnen.
		int yOff = token * bunchsize;
		int rows = bunchsize;

		if (yOff + rows >= theScene.set.sizeY)
			rows = theScene.set.sizeY - yOff;

		return new int[] {yOff, rows, token};
	}

	public static void setCompleted(int token)
	{
		synchronized (tokens)
		{
			tokens[token] = 2;

			// Schau nach, ob wir jetzt ganz fertig sind.
			for (int i = 0; i < tokens.length; i++)
				if (tokens[i] != 2)
					return;

			theScene.pixels = pixels;
			try
			{
				TIFFWriter.writeRGBImage(theScene, new File("/tmp/wth"));
			}
			catch (Exception e) {} // ignore erstmal
		}
	}

	public static void main(String[] args) throws Exception
	{
		// TODO: getopt()

		loadScene("scenes/example4.scn");

		String[] hosts = {
			"localhost", "localhost"
		};
		int[] ports = {
			7431, 7432
		};

		Console out = new Console()
		{
			public void println(String s)
			{
				synchronized (this)
				{
					System.out.println(s);
				}
			}
		};

		for (int i = 0; i < hosts.length; i++)
		{
			Handler h = new Handler();
			h.host = hosts[i];
			h.port = ports[i];
			h.out = out;

			h.start();
		}
	}

	/**
	 * Hiermit wird eine Node bedient.
	 */
	private static class Handler extends Thread
	{
		public String host = null;
		public int port = -1;
		public Console out = null;

		public Socket s = null;

		private void p(String s)
		{
			if (out != null)
				out.println(Thread.currentThread() + "> " + s);
		}

		public void handle() throws Exception
		{
			p("Verbinde zu: " + host + ":" + port);
			s = new Socket(host, port);
			p("Verbunden mit: " + host + ":" + port);

			p("Teste Version...");
			ObjectInputStream ois =
				new ObjectInputStream(s.getInputStream());

			if (ois.readInt() < NetCodes.VERSION)
				return;
			p("Version ok.");

			ObjectOutputStream oos =
				new ObjectOutputStream(s.getOutputStream());

			p("Sende Szene...");
			oos.writeObject(theScene);
			oos.flush();
			p("Szene gesendet.");

			boolean run = true;
			while (run)
			{
				int cmd = ois.readInt();
				switch (cmd)
				{
					case NetCodes.REQUEST_JOB:
						p("Jobanfrage.");
						int[] jobInfo = getFreeJob();
						if (jobInfo == null)
						{
							p("Keine Jobs mehr vorhanden.");
							oos.writeInt(-1);
							run = false;
						}
						else
						{
							p("Sende jobInfo.");
							for (int i = 0; i < jobInfo.length; i++)
							{
								oos.writeInt(jobInfo[i]);
							}
						}
						oos.flush();
						break;

					case NetCodes.JOB_COMPLETED:
						p("Empfange Ergebnis.");
						int yOff = ois.readInt();
						int toid = ois.readInt();

						RGBColor[][] px = (RGBColor[][])ois.readObject();

						// Ergebnis im Ziel einhängen.
						for (int y = 0; y < px.length; y++)
						{
							pixels[y + yOff] = px[y];
						}

						p("Ergebnis erhalten.");
						setCompleted(toid);
						break;

					case NetCodes.QUIT:
					default:
						p("Beende.");
						run = false;
				}
			}
		}

		@Override
		public void run()
		{
			try
			{
				handle();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (s != null)
				{
					try
					{
						s.shutdownInput();
						s.shutdownOutput();
						s.close();
					}
					catch (Exception e) {} // ignore
				}
			}
		}
	}

	/**
	 * Interface für irgendeine Konsole, könnte auch GUI sein.
	 */
	private static interface Console
	{
		public void println(String s);
	}
}

// vim: set ts=2 sw=2 :
