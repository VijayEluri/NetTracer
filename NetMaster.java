import java.util.*;
import java.net.*;
import java.io.*;

public class NetMaster
{
	public static Scene theScene = null;
	public static RGBColor[][] pixels = null;

	public static int bunchsize = 5;
	public static short[] tokens = null;

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
	 * 4 : alloc'd
	 */
	public static int[] getFreeJob(int desiredTokens)
	{
		int startToken = -1;
		int type = -1;
		int allocd = 0;
		synchronized (tokens)
		{
			// Versuche, <desiredTokens> Stück freie Token zu bekommen, die
			// aneinander liegen.
			for (int i = 0; i < tokens.length; i++)
			{
				if (tokens[i] == free)
				{
					startToken = i;

					// Hier fangen wir an. Solange das aktuell betrachtete Token
					// noch frei ist und wir noch mehr wollen, reservieren wir.
					while (i < tokens.length
							&& tokens[i] == free
							&& allocd < desiredTokens)
					{
						tokens[i]++;
						allocd++;
						i++;
					}

					// Okay, raus aus der for-Schleife.
					break;
				}
			}

			// Nichts mehr frei?
			if (allocd == 0)
				return null;

			// Primärer Job oder Antialiasing?
			if (target == 2)
				type = 0;
			else
				type = 1;
		}

		// Von Tokens in Pixelgrenzen umrechnen.
		int yOff = startToken * bunchsize;
		int rows = bunchsize * allocd;

		if (yOff + rows >= theScene.set.sizeY)
			rows = theScene.set.sizeY - yOff;

		return new int[] {yOff, rows, startToken, type, allocd};
	}

	public static short getCurrentTarget()
	{
		synchronized (tokens)
		{
			return target;
		}
	}

	public static void setCompleted(int start, int num)
	{
		synchronized (tokens)
		{
			// Markiere alle als fertig.
			for (int i = start; i < tokens.length && i - start < num; i++)
				tokens[i]++;

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

		String scenePath = "scenes/example4.scn";
		if (args.length > 0)
			scenePath = args[0];

		System.setOut(new NetConsole(System.out));
		System.setErr(new NetConsole(System.err));

		loadScene(scenePath);

		System.out.println("Lese Nodes von STDIN, beende mit EOF:");
		ArrayList<String> hosts = new ArrayList<String>();
		ArrayList<Integer> ports = new ArrayList<Integer>();
		Scanner sin = new Scanner(System.in);
		while (sin.hasNext())
		{
			String line = sin.nextLine();
			String[] split = line.split(":");
			hosts.add(split[0]);
			ports.add(new Integer(split[1]));
		}

		for (int i = 0; i < hosts.size(); i++)
		{
			spawnHandlersFor(hosts.get(i), ports.get(i));
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

			int yOff, toid, allocd;
			RGBColor[][] px;
			boolean run = true;
			long t_start = 0, t_end = 0;
			int jobsize = 1, lastjobsize = 1;
			while (run)
			{
				int cmd = ois.readInt();
				switch (cmd)
				{
					case NetCodes.REQUEST_JOB:
						System.out.println("Jobanfrage.");

						// Schätze linear ab, wieviele Tokens es bräuchte, damit
						// dieser Job eine vernünftige Zeit braucht.
						if (t_start != 0 && t_end != 0)
						{
							double millidiff = (double)(t_end - t_start);

							lastjobsize = jobsize;
							jobsize = (int)((lastjobsize / millidiff) * 15000);

							if (jobsize < 1)
								jobsize = 1;
							else if (jobsize > 50)
								jobsize = 50;
						}

						int[] jobInfo = getFreeJob(jobsize);
						if (jobInfo == null)
						{
							if (getCurrentTarget() == 2 && theScene.set.AARays > 0)
							{
								// Im Moment ist nichts frei, aber es kommt noch
								// Antialiasing. Bitte warten.
								System.out.println("Muss warten.");
								oos.writeInt(-1);

								// Wir sind an der Grenze zum Antialiasing. Da dort die
								// Performance grundlegend anders sein kann, fangen wir
								// auf jeden Fall wieder mit der kleinstmöglichen
								// Tokengröße an.
								t_start = 0;
								t_end = 0;
								jobsize = 1;
								lastjobsize = 1;
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
							System.out.println("Sende jobInfo, Größe: " + jobsize);
							for (int i = 0; i < jobInfo.length; i++)
							{
								oos.writeInt(jobInfo[i]);
							}
							t_start = System.currentTimeMillis();
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
						allocd = ois.readInt();

						px = (RGBColor[][])ois.readObject();

						// Ergebnis im Ziel einhängen.
						for (int y = 0; y < px.length; y++)
						{
							pixels[y + yOff] = px[y];
						}

						System.out.println("Ergebnis erhalten, Token fertig.");
						setCompleted(toid, allocd);
						t_end = System.currentTimeMillis();
						break;

					case NetCodes.JOB_COMPLETED_AA:
						System.out.println("Empfange AA-Ergebnis.");
						yOff = ois.readInt();
						toid = ois.readInt();
						allocd = ois.readInt();

						px = (RGBColor[][])ois.readObject();
						System.out.println("AA-Ergebnis erhalten. Addiere.");

						// Ergebnis im Ziel dazuaddieren. Nur kritische Pixel.
						for (int y = 0; y < px.length; y++)
							for (int x = 0; x < px[0].length; x++)
								if (theScene.criticalPixels[y + yOff][x])
									pixels[y + yOff][x].addAllSamples(px[y][x]);

						System.out.println("Addition fertig, Token fertig.");
						setCompleted(toid, allocd);
						t_end = System.currentTimeMillis();
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
