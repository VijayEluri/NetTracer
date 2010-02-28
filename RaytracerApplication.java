/**
 * Hauptanwendung.
 */
public class RaytracerApplication
{
	public static void main(String[] args)
	{
		int i = 0;
		Scene theScene = new Scene();
		String headlessTarget = null;

		if (args.length != 0)
		{
			if (args[i].equals("-h"))
			{
				i++;
				headlessTarget = args[i];
				i++;
			}

			if (theScene.loadEnvironment(args[i]))
			{
				theScene.render(headlessTarget);
			}
			else
			{
				System.err.println("Konnte Szene nicht laden.");
			}
		}
		else
		{
			System.err.println("Gib die zu rendernde Szene als Parameter an.");
		}
	}
}
