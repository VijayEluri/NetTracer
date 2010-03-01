import java.io.*;
import java.net.*;

public class NetNode
{
	public static void main(String[] args) throws Exception
	{
		String host = "localhost";
		int port = 7431;

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-h"))
			{
				host = args[i + 1];
				i++;
			}
			if (args[i].equals("-p"))
			{
				port = new Integer(args[i + 1]);
				i++;
			}
		}

		ServerSocket serv = new ServerSocket(port, 0,
				InetAddress.getByName(host));

		System.out.println("Node bereit.");

		while (true)
		{
			Socket cli = serv.accept();
			new Handler(cli).start();
		}
	}

	/**
	 * Hier wird gerechnet.
	 */
	private static class Handler extends Thread
	{
		public Socket s = null;
		public Scene theScene = null;

		public Handler(Socket s)
		{
			this.s = s;
		}

		public void handle() throws Exception
		{
			ObjectOutputStream oos
				= new ObjectOutputStream(s.getOutputStream());

			System.out.println("Sende meine Version.");
			oos.writeInt(NetCodes.VERSION);
			oos.flush();
			System.out.println("Fertig.");

			ObjectInputStream ois
				= new ObjectInputStream(s.getInputStream());

			int firstCmd = ois.readInt();
			if (firstCmd == NetCodes.QUERY_THREADS)
			{
				System.out.println("Gebe Threadzahl bekannt.");
				oos.writeInt(Runtime.getRuntime().availableProcessors());
				oos.flush();
				return;
			}

			System.out.println("Warte auf Szene...");
			theScene = (Scene)ois.readObject();
			System.out.println("Habe Szene.");

			System.out.println("Bereite Szene vor...");
			if (!theScene.prepareScene())
			{
				System.out.println("Konnte Szene nicht vorbereiten.");
				oos.writeInt(NetCodes.QUIT);
				oos.flush();
				return;
			}
			System.out.println("Szene bereit.");

			boolean run = true;
			int state = 0;
			while (run)
			{
				switch (state)
				{
					case 0:
						oos.writeInt(NetCodes.REQUEST_JOB);
						oos.flush();

						int yOff = ois.readInt();

						if (yOff == -1)
						{
							System.out.println("Gegenseite sagt: Erste Phase vorbei.");
							state++;
						}
						else
						{
							int rows = ois.readInt();
							int toid = ois.readInt();

							System.out.println("Habe Job: " + yOff + ", " + rows);

							if (!theScene.renderPartialPrimary(yOff, rows))
							{
								System.out.println("Fehler beim Rendern.");
								oos.writeInt(NetCodes.QUIT);
								oos.flush();
								run = false;
							}
							else
							{
								System.out.println("Job fertig, sende zurück...");
								oos.writeInt(NetCodes.JOB_COMPLETED);
								oos.writeInt(yOff);
								oos.writeInt(toid);
								oos.writeObject(theScene.pixels);
								oos.flush();
								oos.reset();  // reset ist wichtig, da sich pixels ändert
								System.out.println("Job gesendet.");
								state = 0;
							}
						}
						break;

					default:
						oos.writeInt(NetCodes.QUIT);
						oos.flush();
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

			System.out.println("Thread zuende.");
		}
	}
}

// vim: set ts=2 sw=2 :
