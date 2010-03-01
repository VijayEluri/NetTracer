import java.net.*;
import java.io.*;

public class NetMaster
{
	public static Scene theScene = null;
	public static RGBColor[][] pixels = null;

	public static int bunchsize = 5;
	public static short[] tokens = null;
	public static boolean direction = false;

	public static short free = 0;
	public static short target = 2;

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
	 * 3 : type (0 = regular, 1 = antialiasing)
	 */
	public static int[] getFreeJob()
	{
		int token = -1;
		int type = -1;
		synchronized (tokens)
		{
			// Von vorne oder hinten suchen?
			if (direction)
			{
				for (int i = 0; i < tokens.length; i++)
				{
					if (tokens[i] == free)
					{
						tokens[i]++;
						token = i;
						break;
					}
				}
			}
			else
			{
				for (int i = tokens.length - 1; i >= 0; i--)
				{
					if (tokens[i] == free)
					{
						tokens[i]++;
						token = i;
						break;
					}
				}
			}

			direction = !direction;

			// Nichts mehr frei?
			if (token == -1)
				return null;

			// Primärer Job oder Antialiasing?
			if (tokens[token] == 1)
				type = 0;
			else if (tokens[token] == 4)
				type = 1;
		}

		// Von Tokens in Pixelgrenzen umrechnen.
		int yOff = token * bunchsize;
		int rows = bunchsize;

		if (yOff + rows >= theScene.set.sizeY)
			rows = theScene.set.sizeY - yOff;

		return new int[] {yOff, rows, token, type};
	}

	public static short getCurrentTarget()
	{
		synchronized (tokens)
		{
			return target;
		}
	}

	public static void setCompleted(int token)
	{
		synchronized (tokens)
		{
			tokens[token]++;

			// Status ausgeben.
			int bunchDone = 0;
			String outstr = "";
			boolean hadOne = false;
			for (int i = 0; i < tokens.length; i++)
			{
				hadOne = true;
				if (tokens[i] == target)
					bunchDone++;

				if (((i + 1) % 50) == 0)
				{
					outstr += "[" + (bunchDone < 10 ? "0" : "") + bunchDone + "]";
					bunchDone = 0;
					hadOne = false;
				}
			}
			if (hadOne)
			{
				outstr += "[" + (bunchDone < 10 ? "0" : "") + bunchDone + "]";
			}
			System.out.println(outstr);

			// Schau nach, ob wir jetzt fertig sind.
			for (int i = 0; i < tokens.length; i++)
				if (tokens[i] != target)
					return;

			// Nichts mehr zu machen, speichere das Ding.
			theScene.pixels = pixels;
			try
			{
				TIFFWriter.writeRGBImage(theScene, new File("/tmp/wth" + target));
			}
			catch (Exception e) {} // ignore erstmal

			// Wir sind mit den primären Strahlen fertig. Jetzt AA?
			if (target == 2 && theScene.set.AARays > 0)
			{
				free = 3;
				target = 5;

				// Alle Tokens wieder auf "frei" setzen.
				for (int i = 0; i < tokens.length; i++)
					tokens[i] = free;
			}
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

	public static synchronized void findCriticalPixels()
	{
		if (theScene.criticalPixels == null)
		{
			// Array bauen.
			theScene.criticalPixels = new boolean[theScene.set.sizeY][];
			for (int y = 0; y < theScene.set.sizeY; y++)
				theScene.criticalPixels[y] = new boolean[theScene.set.sizeX];

			// Array füllen, nutze vorhandenen Code.
			theScene.renderPhase(1, -1);
		}
	}

	public static void main(String[] args) throws Exception
	{
		// TODO: getopt()

		System.setOut(new NetConsole(System.out));
		System.setErr(new NetConsole(System.err));

		loadScene("scenes/julia-pres-1.scn");

		String[] hosts = {
			"localhost", "mobiltux"
		};
		int[] ports = {
			7431, 7431
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

			int yOff, toid;
			RGBColor[][] px;
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
							if (getCurrentTarget() == 2 && theScene.set.AARays > 0)
							{
								// Im Moment ist nichts frei, aber es kommt noch
								// Antialiasing. Bitte warten.
								System.out.println("Muss warten.");
								oos.writeInt(-1);
							}
							else
							{
								// Du kannst gehen, es ist gar nichts mehr da.
								System.out.println("Keine Jobs mehr vorhanden.");
								oos.writeInt(-2);
							}
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

					case NetCodes.REQUEST_CRITICAL:
						System.out.println("Anfrage für kritische Pixel.");
						findCriticalPixels();
						oos.writeObject(theScene.criticalPixels);
						oos.flush();
						break;

					case NetCodes.JOB_COMPLETED:
						System.out.println("Empfange Ergebnis.");
						yOff = ois.readInt();
						toid = ois.readInt();

						px = (RGBColor[][])ois.readObject();

						// Ergebnis im Ziel einhängen.
						for (int y = 0; y < px.length; y++)
						{
							pixels[y + yOff] = px[y];
						}

						System.out.println("Ergebnis erhalten, Token fertig.");
						setCompleted(toid);
						break;

					case NetCodes.JOB_COMPLETED_AA:
						System.out.println("Empfange AA-Ergebnis.");
						yOff = ois.readInt();
						toid = ois.readInt();

						px = (RGBColor[][])ois.readObject();
						System.out.println("AA-Ergebnis erhalten. Addiere.");

						// Ergebnis im Ziel dazuaddieren. Nur kritische Pixel.
						for (int y = 0; y < px.length; y++)
							for (int x = 0; x < px[0].length; x++)
								if (theScene.criticalPixels[y + yOff][x])
									pixels[y + yOff][x].addAllSamples(px[y][x]);

						System.out.println("Addition fertig, Token fertig.");
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
