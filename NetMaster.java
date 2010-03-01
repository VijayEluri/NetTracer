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

			// Nichts mehr zu machen, speichere das Ding.
			theScene.pixels = pixels;
			try
			{
				TIFFWriter.writeRGBImage(theScene, new File("/tmp/wth"));
			}
			catch (Exception e) {} // ignore erstmal
		}
	}

	public static int getThreadsForClient(String host, int port)
	{
		Socket s = null;
		int numthreads = -1;
		try
		{
			s = new Socket(host, port);
			System.out.println("Frage " + host + ":" + port + " nach Threads.");

			ObjectInputStream ois =
				new ObjectInputStream(s.getInputStream());

			if (ois.readInt() >= NetCodes.VERSION)
			{
				System.out.println("Version ok.");

				ObjectOutputStream oos =
					new ObjectOutputStream(s.getOutputStream());

				oos.writeInt(NetCodes.QUERY_THREADS);
				oos.flush();
				System.out.println("Warte auf Antwort...");
				numthreads = ois.readInt();
				System.out.println("Node kann " + numthreads + " Threads.");
			}
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

		return numthreads;
	}

	public static void spawnHandlersFor(String host, int port)
		throws Exception
	{
		int numthreads = getThreadsForClient(host, port);

		for (int i = 0; i < numthreads; i++)
		{
			Handler h = new Handler();
			h.host = host;
			h.port = port;

			h.start();
		}
	}

	public static void main(String[] args) throws Exception
	{
		// TODO: getopt()

		System.setOut(new NetConsole(System.out));
		System.setErr(new NetConsole(System.err));

		loadScene("scenes/julia-pres-1.scn");

		String[] hosts = {
			"localhost", "localhost", "mobiltux", "mobiltux"
		};
		int[] ports = {
			7431, 7431, 7431, 7431
		};

		for (int i = 0; i < hosts.length; i++)
		{
			spawnHandlersFor(hosts[i], ports[i]);
		}
	}

	/**
	 * Hiermit wird eine Node bedient.
	 */
	private static class Handler extends Thread
	{
		public String host = null;
		public int port = -1;

		public Socket s = null;

		public void handle() throws Exception
		{
			System.out.println("Verbinde zu: " + host + ":" + port);
			s = new Socket(host, port);
			System.out.println("Verbunden mit: " + host + ":" + port);

			System.out.println("Teste Version...");
			ObjectInputStream ois =
				new ObjectInputStream(s.getInputStream());

			if (ois.readInt() < NetCodes.VERSION)
				return;
			System.out.println("Version ok.");

			ObjectOutputStream oos =
				new ObjectOutputStream(s.getOutputStream());

			// NOOP senden, also *nicht* Threads abfragen.
			oos.writeInt(NetCodes.NOOP);

			System.out.println("Sende Szene...");
			oos.writeObject(theScene);
			oos.flush();
			System.out.println("Szene gesendet.");

			boolean run = true;
			while (run)
			{
				int cmd = ois.readInt();
				switch (cmd)
				{
					case NetCodes.REQUEST_JOB:
						System.out.println("Jobanfrage.");
						int[] jobInfo = getFreeJob();
						if (jobInfo == null)
						{
							System.out.println("Keine Jobs mehr vorhanden.");
							oos.writeInt(-1);
						}
						else
						{
							System.out.println("Sende jobInfo.");
							for (int i = 0; i < jobInfo.length; i++)
							{
								oos.writeInt(jobInfo[i]);
							}
						}
						oos.flush();
						break;

					case NetCodes.JOB_COMPLETED:
						System.out.println("Empfange Ergebnis.");
						int yOff = ois.readInt();
						int toid = ois.readInt();

						RGBColor[][] px = (RGBColor[][])ois.readObject();

						// Ergebnis im Ziel einhängen.
						for (int y = 0; y < px.length; y++)
						{
							pixels[y + yOff] = px[y];
						}

						System.out.println("Ergebnis erhalten.");
						setCompleted(toid);
						break;

					case NetCodes.QUIT:
					default:
						System.out.println("Beende.");
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
