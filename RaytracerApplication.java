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

		if (args.length == 0)
		{
			System.err.println("Gib die zu rendernde Szene als Parameter an.");
			System.exit(1);
		}

		if (args[i].equals("-h"))
		{
			i++;
			headlessTarget = args[i];
			i++;
		}

		if (!theScene.loadScene(args[i]))
		{
			System.err.println("Konnte Szene nicht laden.");
			System.exit(1);
		}

		if (headlessTarget != null)
		{
			if (!theScene.initHeadlessTarget(headlessTarget))
			{
				System.err.println("Kann nicht headless rendern.");
				System.exit(1);
			}
		}

		if (!theScene.prepareScene())
		{
			System.err.println("Konnte Szene nicht vorbereiten.");
			System.exit(1);
		}

		theScene.renderAll();
	}
}
