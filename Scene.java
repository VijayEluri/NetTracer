import java.util.Random;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.*;

/**
 * Die ganze Szene, die dann auch gerendert werden kann
 */
public class Scene implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public Camera eye;
	public Object3D[] objects;
	public Light[] lights;

	// Der BoundingVolumeTree
	public BVNode bvroot = null;

	// Hier wird das Bild am Ende gespeichert
	public RGBColor[][] pixels = null;

	// Interessierte können hier warten und benachrichtigt werden
	public LinkedList<Boolean> repaintQueue = new LinkedList<Boolean>();

	// AA-Informationen
	public boolean[][] criticalPixels = null;

	// Thread-Koordination
	private int[] nextFreeRow = new int[1];
	public ThreadPositions tpos = null;
	public Boolean running = false;

	// Für jegliche Random-Werte, dieses Objekt ist threadsafe.
	// Liefert eine schönere Verteilung als Math.random(), was vorallem
	// beim Antialiasing deutlich (!) sichtbar ist.
	private final Random rGen = new Random(System.currentTimeMillis());

	/**
	 * Settings für eine zu rendernde Szene
	 */
	public class SceneSettings implements Serializable
	{
		private static final long serialVersionUID = 20100301002L;

		public int sizeX = 640, sizeY = 480;
		public int maxdepth = 4;

		public File headless = null;
		public boolean hasGUI = true;
		public boolean useBVT = true;
		public boolean noLighting = false;
		public boolean noShadowFeelers = false;
		public boolean fakeDistanceShadow = false;
		public double fakeDistanceScale = 1.0;
		public double fakeDistanceExp = 1.0;
		public int AARays = 0, threads = 2, rowstep = 6;
		public RGBColor environ = RGBColor.black();

		// Bestimmt, wann zwei Pixel als "unterschiedlich" erkannt werden
		// sollen. Dieser Wert wird noch durch 255.0 geteilt, die Angabe
		// erfolgt also in "RGB-Farbschritten"
		public double colorDelta = 2.0;

		public void dump()
		{
			System.out.println("Settings:");
			System.out.println("---------");
			System.out.println("Auflösung: " + sizeX + "x" + sizeY);
			System.out.println("Threads: " + threads + " mit " + rowstep + " Zeilen pro Thread");
			System.out.println("Maximale Tiefe: " + maxdepth);
			System.out.println("AA-Rays: " + AARays);
			System.out.println("Farb-Delta: " + colorDelta);
			System.out.println("BoundingVolumeTree nutzen: " + useBVT);
			System.out.println("Beleuchtung auslassen: " + noLighting);
			System.out.println();
		}
	}
	public SceneSettings set;

	/**
	 * Einer der Threads, die parallel die Pixel rendern. Siehe auch
	 * doPrimaryRays() und render() weiter unten.
	 */
	private class WorkerThread extends Thread
	{
		private int ID;
		private boolean primary;
		private int numrays;
		private Random rGen = new Random(System.currentTimeMillis());

		public WorkerThread(int ID, boolean primary, int numrays)
		{
			this.ID = ID;
			this.primary = primary;
			this.numrays = numrays;
		}

		public void run()
		{
			//setPriority(Thread.MAX_PRIORITY);
			doPrimaryRays(this.ID, this.primary, this.numrays, set.rowstep, this.rGen);
		}
	}

	/**
	 * Führt einen Intersectiontest dieses Rays mit allen Objekten
	 * in der Szene durch und liefert das nächste Objekt
	 */
	public Intersection doIntersectionTest(Ray r)
	{
		double windist = 10e100;
		Intersection winner = null;

		if (bvroot != null)
		{
			double[] singleTemp = new double[2];

			// Eintrittspunkt in Root-Node + Epsilon, falls ein Schnitt
			// existiert.
			if (!bvroot.space.alphaEntryExit(r, singleTemp))
				return null;
			Vec3 nextPos = r.evaluate(singleTemp[0] + BVNode.traversalEpsilon);

			// Steige durch den Tree und suche erste Intersection
			Intersection ints = null;
			BVNode curnode = bvroot;
			while (curnode != null)
			{
				// Finde das Kind, das den aktuellen Punkt enthält
				BVNode child = curnode.findChild(nextPos);

				// Teste die dortigen Primitive und suche das mit geringster
				// Entfernung. Ganz wichtig: Der Schnittpunkt muss
				// innerhalb dieser Node liegen!
				for (RenderingPrimitive p : child.prims)
				{
					ints = p.intersectionTest(r);
					if (ints != null && ints.distance < windist
						&& child.space.contains(ints.at))
					{
						winner = ints;
						windist = ints.distance;
					}
				}

				// Hast du einen validen Schnitt gefunden? Dann raus.
				if (winner != null)
					return winner;

				// Gehe zum Austrittspunkt + Epsilon
				nextPos = r.evaluate(child.space.alphaExit(r) + BVNode.traversalEpsilon);

				// Finde die nächste Node, die diesen Punkt enthält
				// (wandert nur so weit wie nötig im Tree nach oben).
				// Im nächsten Durchgang der Schleife wird dann erst das
				// passende Kind dazu gesucht.
				// Kann null sein, falls dieser Punkt nirgends enthalten
				// ist.
				curnode = child.findNextParent(nextPos);
			}

			return null;
		}
		else
		{
			// Das schneidende Dreieck suchen, das am nächsten liegt
			for (Object3D o : objects)
			{
				RenderingPrimitive[] m = o.getRenderingPrimitives();

				for (RenderingPrimitive t : m)
				{
					Intersection ints = t.intersectionTest(r);
					if (ints != null && ints.distance < windist)
					{
						winner = ints;
						windist = ints.distance;
					}
				}
			}
		}
		return winner;
	}

	/**
	 * Da ich keine Caustics bauen will, aber auch keine knallharten
	 * Schatten bei transparenten Objekten sehen möchte, hier eine
	 * leicht abgewandelte Variante des globalen Intersection Tests:
	 * Errechne über alle transparenten Objekte, die der Lichtstrahl
	 * kreuzt, einen Faktor, der die Intensität des Lichts bestimmt.
	 * Sobald ein Objekt gefunden wurde, das nicht transparent ist,
	 * wird abgebrochen.
	 */
	public double doShadowFeeling(Ray r, double maxdist)
	{
		if (set.noShadowFeelers)
			return 1.0;

		if (set.fakeDistanceShadow)
		{
			double distanceFromEye = r.origin.minus(eye.origin).length();
			return set.fakeDistanceScale / Math.pow(distanceFromEye, set.fakeDistanceExp);
		}

		double lightscale = 1.0;

		if (bvroot != null)
		{
			// Bei ShadowFeeling kann vorausgesetzt werden, dass der Ray
			// schon in einer Node startet, sonst wäre es kein Shadow-
			// Feeler. Spare dir also ein paar Schritte und fange direkt
			// mit dem origin an.
			Vec3 nextPos = r.origin;

			// Steige durch den Tree und suche alle Intersections
			Intersection ints = null;
			BVNode parent = bvroot;
			boolean tooFar;
			while (parent != null)
			{
				// Finde das Kind, das den aktuellen Punkt enthält
				BVNode child = parent.findChild(nextPos);

				// Teste alle Primitive in dieser Node
				tooFar = false;
				for (RenderingPrimitive p : child.prims)
				{
					ints = p.intersectionTest(r);
					// ... aber nur, wenn der Schnittpunkt auch wirklich
					// innerhalb der Node liegt.
					if (ints != null && child.space.contains(ints.at))
					{
						if (ints.distance < maxdist)
						{
							// Du bist auf ein Objekt gestoßen, das nicht
							// transluzent ist, also ist hier voller
							// Schatten.
							if (ints.mat.transparency == 0.0)
								return 0.0;

							lightscale *= ints.mat.transparency;
						}
						else
							// Du hast in diesem Satz einen validen
							// Schnittpunkt gefunden, der aber hinter
							// dem Licht liegt. Merke dir das, denn dann...
							tooFar = true;
					}
				}
				// ...kann die Suche beendet werden, da alle folgenden
				// Primitive in den kommenden Nodes auch hinter dem Licht
				// liegen werden.
				if (tooFar)
					return lightscale;

				// Gehe zum Austrittspunkt + Epsilon
				nextPos = r.evaluate(child.space.alphaExit(r) + BVNode.traversalEpsilon);

				// Finde die nächste Node, die diesen Punkt enthält
				// (wandert nur so weit wie nötig im Tree nach oben).
				// Im nächsten Durchgang der Schleife wird dann erst das
				// passende Kind dazu gesucht.
				// Kann null sein, falls dieser Punkt nirgends enthalten
				// ist.
				parent = child.findNextParent(nextPos);
			}

			// Wenn wir hier ankommen, haben wir den Tree verlassen,
			// aber das Licht noch nicht erreicht, also kommt da auch
			// nichts mehr.
			return lightscale;
		}
		else
		{
			for (Object3D o : objects)
			{
				RenderingPrimitive[] m = o.getRenderingPrimitives();

				for (RenderingPrimitive t : m)
				{
					Intersection ints = t.intersectionTest(r);
					if (ints != null && ints.distance < maxdist)
					{
						if (ints.mat.transparency == 0.0)
						{
							return 0.0;
						}

						lightscale *= ints.mat.transparency;
					}
				}
			}
		}

		return lightscale;
	}

	/**
	 * Berechnet die Richtung für einen TransmissionRay
	 * 
	 * Page mit vielen Skizzen und Erklärungen:
	 * http://www.cs.umbc.edu/~rheingan/435/pages/res/gen-11.Illum-single-page-0.html
	 */
	public Vec3 calcTransmissionDirection(Vec3 V, Vec3 N, double iorNew)
	{
		// Code aus Skript
		Vec3 tLat = new Vec3(N);
		tLat.scale(N.dot(V));
		tLat.subtract(V);

		tLat.scale(1.0 / iorNew);

		double sinSq = tLat.lengthSquared();

		// Innere Totalreflexion?
		if (sinSq > 1.0)
			return null;

		tLat.subtract(N.times(Math.sqrt(1 - sinSq)));
		return tLat;
	}

	/**
	 * Eine komplette Stufe im Raytracing: Besorgt die Farbe dieses
	 * einen Rays. Dabei bestimmen ShadowFeeler, ob eine Lichtquelle
	 * sichtbar ist und mit welcher Intensität. ReflectionRays werden,
	 * falls das Objekt reflektiv ist, ausgesandt - analog mit
	 * TransmissionRays.
	 */
	public RGBColor traceRay(Ray r, int depth, Random rGen)
	{
		// Schau erstmal, wo dieser Strahl landet.
		Intersection intres = doIntersectionTest(r);

		// Kein Schnitt, schwarz
		if (intres == null)
			return new RGBColor(set.environ);

		RGBColor out = RGBColor.black();

		// Kürzere Schreibweisen...
		Vec3 N = intres.normal;
		Vec3 hitPoint = intres.at;
		Material targetMat = intres.mat;
		RGBColor baseDiffuse = intres.diffuseColor;
		RGBColor baseSpecular = intres.specularColor;
		RGBColor baseTransparent = intres.transparentColor;

		// Vorberechnung von häufig gebrauchten Vektoren:

		// Negative Ray-Richtung: V = -U
		Vec3 V = r.direction.times(-1.0);

		// Reflektionsvektor: R = 2 * (N*V) * N - V
		Vec3 R = new Vec3(N);
		R.scale(2.0 * N.dot(V));
		R.subtract(V);

		// Lokale Beleuchtung, sofern diese nicht deaktiviert ist
		if (!set.noLighting)
		{
			double[] li = new double[2];
			double lightscale = 0.0, shinyscale = 0.0, res = 0.0;

			for (Light l : lights)
			{
				if (l instanceof SphereLight)
				{
					// Ein SphereLight. Hier werden mehrere Rays geschossen
					// und das Licht wird jedesmal ein Stück (abhängig
					// von seinem Radius) geschubst, dann wird der
					// normale ShadowFeeler in diese Richtung ausgesandt.

					// Muss vor den normalen PointLights kommen, da diese
					// Klasse davon abgeleitet ist...

					SphereLight slight = (SphereLight)l;
					lightscale = 0.0;
					shinyscale = 0.0;

					// Wichtig: Für die Intensität des Lichtes wird auch
					// der jittered Vec3 genommen. Sonst entstehen zwar
					// SoftShadows, aber kein AreaLight.

					Vec3 npos;
					for (int i = 0; i < slight.numrays; i++)
					{
						// Schubsen
						npos = l.origin.jittered(slight.radius, rGen);

						// Jetzt normale ShadowFeeler-Geschichte
						npos.subtract(hitPoint);

						Ray shadowFeeler = new Ray(hitPoint, new Vec3(npos));
						res = doShadowFeeling(shadowFeeler, npos.length());

						// Wenn das Licht in diesem Fall zu sehen war,
						// addiere die dann entstandene Intensität dazu.
						if (res > 0.0)
						{
							l.lighting(targetMat, npos, R, N, li);
							lightscale += li[0];
							shinyscale += li[1];
						}
					}

					// Lichtintensität über Anzahl Rays skalieren, denn
					// rayscale = 1.0 / numrays
					// Außerdem den Faktor "res" mit dazunehmen, damit
					// das Licht durch transparente Objekte abgeschwächt
					// wird.
					lightscale *= res * slight.rayscale;
					shinyscale *= res * slight.rayscale;

					if (lightscale > 0.0)
					{
						// War das Licht sichtbar? Dann multipliziere
						// seine Farbe mit der diffusen Farbe des
						// Materials, skaliert mit dem Transparenzfaktor.

						// Verrechne auch mit lokaler Transparenz bzw.
						// Spekularität.
						lightscale *= (1.0 - targetMat.transparency);
						lightscale *= (1.0 - targetMat.specularity);
						out.add(baseDiffuse.times(lightscale).product(l.color));

						// Shininess läuft unabhängig von diesen beiden.
						out.add(baseSpecular.times(shinyscale).product(l.color));
					}
				}
				else //if (l instanceof PointLight)
				{
					// Ganz normale Beleuchtung mit einem Punktlicht
					Vec3 L = l.origin.minus(hitPoint);

					Ray shadowFeeler = new Ray(hitPoint, new Vec3(L));
					res = doShadowFeeling(shadowFeeler, L.length());

					if (res > 0.0)
					{
						l.lighting(targetMat, L, R, N, li);
						lightscale = res * li[0];

						// Verrechne auch mit lokaler Transparenz bzw.
						// Spekularität.
						lightscale *= (1.0 - targetMat.transparency);
						lightscale *= (1.0 - targetMat.specularity);
						out.add(baseDiffuse.times(lightscale).product(l.color));

						// Shininess läuft unabhängig von diesen beiden.
						out.add(baseSpecular.times(li[1] * res).product(l.color));
					}
				}
			}
		}
		else
			out.add(baseDiffuse);

		if (depth == 0)
			return out;

		// Specularity
		if (targetMat.specularity > 0.0)
		{
			if (targetMat.roughness > 0.0)
			{
				// Material ist rauh, erzeuge blurry reflection
				RGBColor specCol = RGBColor.black();

				for (int i = 0; i < targetMat.roughRays; i++)
				{
					Ray reflectionRay = new Ray(hitPoint, R.jittered(targetMat.roughness, rGen));
					specCol.add(traceRay(reflectionRay, depth - 1, rGen));
				}

				// Reflektiertes Licht über die Rays mitteln
				specCol.scale(1.0 / targetMat.roughRays);

				// Dann mit specularity skalieren
				specCol.scale(targetMat.specularity);

				// Reflektionen "in" der spekularen Farbe des Materials
				// erscheinen lassen
				specCol.multiply(baseSpecular);

				out.add(specCol);
			}
			else
			{
				// Scharfe Reflektionen
				Ray reflectionRay = new Ray(hitPoint, R);
				RGBColor specCol = traceRay(reflectionRay, depth - 1, rGen);
				specCol.scale(targetMat.specularity);
				specCol.multiply(baseSpecular);
				out.add(specCol);
			}
		}

		// Transparency
		if (targetMat.transparency > 0.0)
		{
			Vec3 t;
			// Ganz wichtig: Trifft der Strahl von hinten bzw. innen
			// auf das Primitiv, dann müssen ior UND Normale invertiert
			// werden! Das steht leider nur an einer Stelle:
			// http://www.devmaster.net/articles/raytracing_series/part3.php
			// Alle anderen Quellen sehen das wohl als trivial an ...
			if (N.dot(V) < 0.0)
			{
				// Austritt
				t = calcTransmissionDirection(V, N.times(-1.0), 1.0 / targetMat.ior);
			}
			else
			{
				// Eintritt
				t = calcTransmissionDirection(V, N, targetMat.ior);
			}

			if (t != null)
			{
				if (targetMat.cloudiness > 0.0)
				{
					// Cloudiness - selbes Prinzip wie bei Roughness
					RGBColor transCol = RGBColor.black();

					for (int i = 0; i < targetMat.cloudyRays; i++)
					{
						Ray transmissionRay = new Ray(hitPoint, t.jittered(targetMat.cloudiness, rGen));
						transCol.add(traceRay(transmissionRay, depth - 1, rGen));
					}

					transCol.scale(1.0 / targetMat.cloudyRays);
					transCol.scale(targetMat.transparency);
					transCol.multiply(baseTransparent);
					out.add(transCol);
				}
				else
				{
					// Normale Transparenz
					Ray transmissionRay = new Ray(hitPoint, t);
					RGBColor transCol = traceRay(transmissionRay, depth - 1, rGen);
					transCol.scale(targetMat.transparency);
					transCol.multiply(baseTransparent);
					out.add(transCol);
				}
			}
		}

		return out;
	}

	/**
	 * Bestimmt die Farbe eines Pixels. Wird vom Antialiasing mehrfach
	 * mit leicht unterschiedlichen Pixelpositionen aufgerufen und die
	 * Ergebnisse werden durch die RGBColor-Klasse gemittelt.
	 * 
	 * Hier findet auch die Unterscheidung und Berechnung von DOF statt.
	 */
	public RGBColor renderPixel(double x, double y, int maxdepth, Random rGen)
	{
		Ray clearRay = eye.castRay(x, y);

		if (eye.focalDistance == 0.0)
			return traceRay(clearRay, maxdepth, rGen);
		else
		{
			// Finde Punkt, wo dieser Ray auf der Focal Plane landet.
			// Bestimmte Annahmen können gemacht werden, z.B. dass der
			// Ray nie parallel zu dieser Ebene liegt oder der Schnitt-
			// punkt hinter der Kamera.
			double c = clearRay.direction.dot(eye.viewdirInv);
			double alpha = (eye.dofLambda - clearRay.origin.dot(eye.viewdirInv)) / c;
			Vec3 clearPoint = clearRay.evaluate(alpha);

			// Verfolge nun mehrere Rays, deren Ergebnisse anschließend
			// gemittelt werden: Wackle dabei am Ursprung des Rays, aber
			// die Richtung ist immer so gewählt, dass der Ray durch den
			// clearPoint auf der Focal-Plane verläuft.
			RGBColor out = RGBColor.empty();
			for (int i = 0; i < eye.dofRays; i++)
			{
				double offsetX = rGen.nextGaussian() * 0.5 * eye.dofAmount;
				double offsetY = rGen.nextGaussian() * 0.5 * eye.dofAmount;

				// Randomisierter Ursprung entlang der Bildebene
				Vec3 dofOrigin = new Vec3(eye.origin);
				dofOrigin.add(eye.updir.times(offsetY));
				dofOrigin.add(eye.rdir.times(offsetX));

				// Angepasste Blickrichtung - wird automatisch im
				// Ray-Konstruktor normalisiert
				Vec3 dofDirection = clearPoint.minus(dofOrigin);

				// Verfolge diesen Strahl
				Ray dofRay = new Ray(dofOrigin, dofDirection);
				out.addSample(traceRay(dofRay, maxdepth, rGen));
			}

			// Gewonnene Samples mitteln.
			out.normalize();

			return out;
		}
	}

	public boolean initHeadlessTarget(String path)
	{
		File f = new File(path);
		if (!f.getParentFile().canWrite())
		{
			System.err.println("Kann nach `" + f + "' nicht schreiben.");
			return false;
		}

		set.headless = f;
		set.hasGUI = false;
		return true;
	}

	/**
	 * Rendert die Szene mit den geladenen Settings
	 */
	public boolean prepareScene()
	{
		if (set == null)
		{
			System.err.println("Keine Settings geladen, kann nicht rendern.");
			return false;
		}
		if (eye == null || objects == null || lights == null)
		{
			System.err.println("Objekte und Lichter müssen schon definiert sein... breche ab.");
			return false;
		}

		set.dump();
		System.out.println("Objekte: " + objects.length);
		System.out.println("Lichter: " + lights.length);
		System.out.println();


		// ############################################################
		// Vorarbeit: Erstelle BV-Tree

		if (set.useBVT)
		{
			long startTime = 0;

			startTime = System.currentTimeMillis();

			ArrayList<RenderingPrimitive> allPrims = new ArrayList<RenderingPrimitive>();
			for (Object3D o : objects)
				for (RenderingPrimitive p : o.getRenderingPrimitives())
					allPrims.add(p);

			System.out.println("Baue große Bounding Box um "
					+ allPrims.size() + " Primitive ...");

			AABB bigBox = new AABB(allPrims, 0);

			// Bei komplexen Szenen ist es schneller, wenn immer Würfel
			// statt irgendwelchen Quadern den Raum aufteilen, da dies
			// eine gleichmäßigere Aufteilung ergibt. Suche daher den
			// maximalen Radius der Szenen-Box und setze diesen auf
			// allen Achsen. Dadurch entstehen zwar mehr Nodes und der
			// Baum muss tiefer sein (Bau dauert länger), dafür ist der
			// Rendervorgang dann ~3-4 Mal schneller.
			double max = bigBox.radii.x;
			if (bigBox.radii.y > max)
				max = bigBox.radii.y;
			if (bigBox.radii.z > max)
				max = bigBox.radii.z;
			bigBox.radii.x = max;
			bigBox.radii.y = max;
			bigBox.radii.z = max;

			// ... ein kleines Epsilon schadet nie.
			bigBox.radii.scale(1.0 + 1e-10);

			System.out.println("Fertig. " + ((System.currentTimeMillis() - startTime) / 1000.0) + " Sekunden.\n");


			System.out.println("Baue BV-Tree ...");
			startTime = System.currentTimeMillis();

			bvroot = new BVNode(null, allPrims, bigBox, BVNode.MAXDEPTH);

			System.out.println("Fertig. " + ((System.currentTimeMillis() - startTime) / 1000.0) + " Sekunden.");


			int numtreenodes = bvroot.getNumNodes();
			System.out.println("Anzahl Nodes im Baum: " + numtreenodes);
			if (numtreenodes < BVNode.NODETHRESHOLD)
			{
				System.out.println("-- Das sind zu wenige, verwerfe den Baum.");
				bvroot = null;
			}
			System.out.println();
		}
		else
		{
			bvroot = null;
		}

		return true;
	}

	public boolean initPixbufs()
	{
		// Ausgabearray fertig machen
		pixels = new RGBColor[set.sizeY][];
		for (int y = 0; y < set.sizeY; y++)
		{
			pixels[y] = new RGBColor[set.sizeX];
			for (int x = 0; x < set.sizeX; x++)
			{
				// Am Anfang alles schwarz, damit das Fenster jederzeit
				// ungehindert irgendein Bild darstellen kann
				pixels[y][x] = RGBColor.black();
			}
		}

		// AA-Infoarray: Kritische Pixel
		criticalPixels = new boolean[set.sizeY][];
		for (int y = 0; y < set.sizeY; y++)
			criticalPixels[y] = new boolean[set.sizeX];

		return true;
	}

	public boolean initUI()
	{
		// Ausgabefenster hochfeuern
		if (set.hasGUI)
		{
			final Scene me = this;
			java.awt.EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					new OutputWindow(me).setVisible(true);
				}
			});
		}
		else
		{
			new ShellProgress(this, set.sizeY);
		}

		return true;
	}

	public void renderAll()
	{
		tpos = new ThreadPositions(set.threads);

		// Normales Rendern
		renderPhase(0, -1);

		// Das folgende ist billig, das kannst du auch machen, wenn
		// eigentlich kein AA stattfinden soll. Dann sieht man immer
		// etwas, wenn man im OutputWindow auf den Knopf drückt.
		renderPhase(1, -1);

		// Antialiasing
		renderPhase(2, set.AARays);

		System.out.println("Rendern beendet.\n");

		if (set.headless != null)
		{
			try
			{
				TIFFWriter.writeRGBImage(this, set.headless);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			// Wenn wir headless rendern, dann sind wir jetzt garantiert
			// fertig, also können alle Threads runtergefahren werden
			// etc.
			System.exit(0);
		}
	}

	/**
	 * Eine Render-Phase:
	 * 0 = initiales Rendern
	 * 1 = Sammeln der AA-Informationen
	 * 2 = Antialiasing
	 * 
	 * Falls AA durchgeführt wird, steht in "info" die Anzahl der Rays.
	 */
	public void renderPhase(int p, int info)
	{
		// Exklusiv. Es ist nicht die ganze Methode synchronized, da
		// sonst die "Jobs" quasi gequeued werden - ungewünscht, wenn
		// es schon läuft, soll einfach nichts passieren.
		synchronized (running)
		{
			if (running)
				return;

			running = true;
		}

		WorkerThread[] workers;
		long startTime = System.currentTimeMillis();

		switch (p)
		{
			case 0:
				System.out.println("Rendere ...");

				// Setze nächste freie Zeile zurück
				nextFreeRow[0] = 0;

				tpos.reset();

				// Erstelle und starte WorkerThreads und lasse diese die erste
				// Iteration durchlaufen
				workers = new WorkerThread[set.threads];
				for (int i = 0; i < workers.length; i++)
				{
					workers[i] = new WorkerThread(i, true, -1);
					workers[i].start();
				}

				// Warte, bis diese Threads fertig sind
				for (WorkerThread t : workers)
				{
					try
					{
						t.join();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						System.exit(-1);
					}
				}

				tpos.reset();

				break;

			case 1:
				System.out.println("Suche kritische Pixel ...");

				// Finde kritische Pixel - gehe über das ganze Bild und schaue,
				// ob sich der betrachtete Pixel von seinem Vorgänger oder
				// dem in der Zeile darüber unterscheidet. Falls ja, markiere
				// alle 8 Pixel um ihn herum als glättenswert.
				for (int y = 1; y < set.sizeY; y++)
				{
					for (int x = 1; x < set.sizeX; x++)
					{
						if (   pixels[y][x].differs(pixels[y][x-1], set.colorDelta / 255.0)
							|| pixels[y][x].differs(pixels[y-1][x], set.colorDelta / 255.0))
						{
							for (int yoff = -1; yoff <= 1; yoff++)
							{
								for (int xoff = -1; xoff <= 1; xoff++)
								{
									int pidxX = x + xoff;
									int pidxY = y + yoff;

									// Nicht über den Rand gehen
									if (pidxX >= 0 && pidxX < set.sizeX && pidxY >= 0 && pidxY < set.sizeY)
									{
										criticalPixels[pidxY][pidxX] = true;
									}
								}
							}
						}
					}
				}
				break;

			case 2:
				System.out.println("Antialiasing: " + info + " Rays ...");

				// Will der User überhaupt Antialiasing?
				if (info > 0)
				{
					// Nächste freie Zeile zurücksetzen
					nextFreeRow[0] = 0;

					tpos.reset();

					// Wieder WorkerThreads starten, diesmal machen die aber
					// nur Antialiasing
					workers = new WorkerThread[set.threads];
					for (int i = 0; i < workers.length; i++)
					{
						workers[i] = new WorkerThread(i, false, info);
						workers[i].start();
					}

					// Warte, bis die fertig sind.
					for (WorkerThread t : workers)
					{
						try
						{
							t.join();
						}
						catch (Exception e)
						{
							e.printStackTrace();
							System.exit(-1);
						}
					}

					tpos.reset();
				}
				break;
		}

		synchronized (running)
		{
			running = false;

			// Falls eine Progress bar an der Shell benutzt wurde, dann
			// wird die jetzt "gelöscht". Andernfalls hat das überhaupt
			// keinen Effekt.
			ShellProgress.clearLine();

			System.out.println("Fertig. " + ((System.currentTimeMillis() - startTime) / 1000.0) + " Sekunden.\n");
		}
	}

	/**
	 * Wird von einem Thread aufgerufen. Markiert in nextFreeRow[0] die
	 * nächste Anzahl an "rowstep" Zeilen als "in Bearbeitung" und zeichnet
	 * diese. Wird so lange wiederholt, bis keine Zeilen mehr frei sind.
	 * @param primary Erster Durchgang mit ggf. Supersampling (true) oder
	 *                schon Antialiasing-Phase (false)?
	 * @param numrays Wieviele Rays pro Pixels sollen geschossen werden?
	 * @param rowstep Wieviele Zeilen sollen in einem Durchgang bearbeitet
	 *                werden?
	 */
	private void doPrimaryRays(int ID, boolean primary, int numrays, int rowstep, Random rGen)
	{
		int curstart = 0;

		while (true)
		{
			// Hole dir hier threadsafe den nächsten Packen an Zeilen,
			// die zum Rendern frei sind
			synchronized (nextFreeRow)
			{
				if (nextFreeRow[0] < set.sizeY)
				{
					curstart = nextFreeRow[0];
					nextFreeRow[0] += rowstep;
				}
				else
				{
					curstart = -1;
				}
			}

			// Nichts mehr frei? Dann raus
			if (curstart == -1)
				return;

			if (primary)
			{
				// Primäre Strahlen

				for (int y = curstart; (y < set.sizeY) && (y < curstart + rowstep); y++)
				{
					for (int x = 0; x < set.sizeX; x++)
					{
						// Den normalen Strahl abschicken.
						pixels[y][x] = renderPixel(x, y, set.maxdepth, rGen);
					}
				}
			}
			else
			{
				// Antialiasing

				// Renne nochmal über das ganze Bild ...
				for (int y = curstart; (y < set.sizeY) && (y < curstart + rowstep); y++)
				{
					for (int x = 0; x < set.sizeX; x++)
					{
						// ... und schaue, ob hier ein kritischer Pixel ist ...
						if (criticalPixels[y][x])
						{
							if (numrays <= 16)
							{
								double sqrays = Math.sqrt(numrays);
								// Ordered Grid bis 16 Rays
								for (int rx = 0; rx < sqrays; rx++)
								for (int ry = 0; ry < sqrays; ry++)
								{
									double offsetX = ((double)rx - 0.5 * sqrays) / sqrays;
									double offsetY = ((double)ry - 0.5 * sqrays) / sqrays;

									pixels[y][x].addSample(
											renderPixel(
												x + offsetX,
												y + offsetY,
												set.maxdepth,
												rGen)
											);
								}
							}
							else
							{
								// Gauss-Jitter bei allem darüber
								for (int i = 0; i < numrays; i++)
								{
									double offsetX = rGen.nextGaussian() * 0.5;
									double offsetY = rGen.nextGaussian() * 0.5;

									pixels[y][x].addSample(
											renderPixel(
												x + offsetX,
												y + offsetY,
												set.maxdepth,
												rGen)
											);
								}
							}
						}
					}
				}
			}

			// Der folgende Teil sollte für's "Real life" nochmal über-
			// arbeitet werden, da er doch recht viel Locking verursacht.

			// Womit bin ich fertig? Für eventuelle Zeichner. 
			tpos.update(ID, curstart + rowstep);

			// Wartende informieren - wenn das, so wie hier, nach jeder
			// fertigen Reihe passiert, geht natürlich durch das häufige
			// Refresh (im Zeichner) ein bisschen Leistung verloren.
			// Hält sich aber in Grenzen und sieht schön aus. ;)
			synchronized (repaintQueue)
			{
				// Einfach ein Token reinsetzen und irgendeinen
				// benachrichtigen
				repaintQueue.addLast(true);
				repaintQueue.notify();
			}
		}
	}

	public boolean renderPartialPrimary(int yOff, int rows)
	{
		// Ausgabearray fertig machen
		if (pixels == null || pixels.length != rows)
		{
			pixels = new RGBColor[rows][];
			for (int y = 0; y < rows; y++)
				pixels[y] = new RGBColor[set.sizeX];
		}

		for (int y = 0; y < rows; y++)
		{
			for (int x = 0; x < set.sizeX; x++)
			{
				// Den normalen Strahl abschicken.
				pixels[y][x] = renderPixel(x, y + yOff, set.maxdepth, rGen);
			}
		}

		return true;
	}

	public boolean renderPartialAntiAlias(int yOff, int rows)
	{
		// Ausgabearray fertig machen
		if (pixels == null || pixels.length != rows)
		{
			pixels = new RGBColor[rows][];
			for (int y = 0; y < rows; y++)
			{
				pixels[y] = new RGBColor[set.sizeX];
				// Diesmal brauchen wir leere Grundpixel. Die sind schwarz, aber
				// der Sample-Counter steht auf 0.
				for (int x = 0; x < set.sizeX; x++)
				{
					pixels[y][x] = RGBColor.empty();
				}
			}
		}

		// Wir rendern jetzt zusätzliche Samples für die kritischen Pixel
		// und nur für die.
		for (int y = 0; y < rows; y++)
		{
			for (int x = 0; x < set.sizeX; x++)
			{
				if (criticalPixels[y + yOff][x])
				{
					if (set.AARays <= 16)
					{
						double sqrays = Math.sqrt(set.AARays);
						// Ordered Grid bis 16 Rays
						for (int rx = 0; rx < sqrays; rx++)
						for (int ry = 0; ry < sqrays; ry++)
						{
							double offsetX = ((double)rx - 0.5 * sqrays) / sqrays;
							double offsetY = ((double)ry - 0.5 * sqrays) / sqrays;

							pixels[y][x].addSample(renderPixel(
										x + offsetX,
										y + offsetY + yOff,
										set.maxdepth, rGen)
									);
						}
					}
					else
					{
						for (int i = 0; i < set.AARays; i++)
						{
							double offsetX = rGen.nextGaussian() * 0.5;
							double offsetY = rGen.nextGaussian() * 0.5;

							pixels[y][x].addSample(renderPixel(
										x + offsetX,
										y + offsetY + yOff,
										set.maxdepth, rGen)
									);
						}
					}
				}
			}
		}

		return true;
	}

	private boolean shortDiffer(short[][] px, int x, int y, int dx, int dy)
	{
		// Teste pro Kanal, ob Toleranz überschritten wird.
		double tol = set.colorDelta;
		return (
				   Math.abs(px[y][(x * 3)    ] - px[y + dy][((x + dx) * 3)    ]) > tol
				|| Math.abs(px[y][(x * 3) + 1] - px[y + dy][((x + dx) * 3) + 1]) > tol
				|| Math.abs(px[y][(x * 3) + 2] - px[y + dy][((x + dx) * 3) + 2]) > tol
		);
	}

	/**
	 * Wie die Suche in renderPhase(), jedoch auf externem short[][]
	 * arbeiten. Nötig für speicherschonendes Netzwerkrendering.
		*
		* Es wird erwartet, dass dieses Array dieselben Dimensionen wie die
		* Szene selbst erfüllt.
	 */
	public void findCriticalShort(short[][] px)
	{
		for (int y = 1; y < set.sizeY; y++)
		{
			for (int x = 1; x < set.sizeX; x++)
			{
				if (shortDiffer(px, x, y, -1, 0) || shortDiffer(px, x, y, 0, -1))
				{
					// Abweichung vorhanden, markiere alle 8 Nachbarn.
					for (int yoff = -1; yoff <= 1; yoff++)
					{
						for (int xoff = -1; xoff <= 1; xoff++)
						{
							int pidxX = x + xoff;
							int pidxY = y + yoff;

							// Nicht über den Rand gehen
							if (pidxX >= 0
									&& pidxX < set.sizeX
									&& pidxY >= 0
									&& pidxY < set.sizeY)
							{
								criticalPixels[pidxY][pidxX] = true;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Lade die Settings und Szene aus dieser Datei
	 */
	public boolean loadScene(String path)
	{
		SceneSettings  nset    = new SceneSettings();
		List<Material> nmats   = new LinkedList<Material>();
		List<Object3D> nobjs   = new LinkedList<Object3D>();
		List<Light>    nlights = new LinkedList<Light>();

		try
		{
			SceneReader in = new SceneReader(path);

			String[] tokens = null;
			while ((tokens = in.getNextTokens()) != null)
			{
				switch (tokens.length)
				{
					// Einstellige Zeilen (nur Schlüsselwort)
					case 1:
						// Camera fängt an
						if (tokens[0].equals("camera"))
							eye = new Camera(in);

						// Sphere fängt an
						if (tokens[0].equals("sphere"))
							nobjs.add(new Sphere3D(in, nmats));

						// OFF-File fängt an
						if (tokens[0].equals("offreader"))
							nobjs.add(new OFFTriMesh3D(in, nmats));

						// OBJ-File fängt an
						if (tokens[0].equals("objreader"))
							nobjs.add(new OBJTriMesh3D(in, nmats));

						// Blob fängt an
						if (tokens[0].equals("blob"))
							nobjs.add(new Blob3D(in, nmats));

						// QJulia3D fängt an
						if (tokens[0].equals("qjulia"))
							nobjs.add(new QJulia3D(in, nmats));

						// Mandelbulb fängt an
						if (tokens[0].equals("mandelbulb"))
							nobjs.add(new Mandelbulb(in, nmats));

						// PointLight fängt an
						if (tokens[0].equals("pointlight"))
							nlights.add(new PointLight(in));

						// SphereLight fängt an
						if (tokens[0].equals("spherelight"))
							nlights.add(new SphereLight(in));

						// Headlight an Position der Kamera
						if (tokens[0].equals("headlight"))
							nlights.add(new Headlight(in, eye));

						break;

					// Zweistellige Felder (Key Value)
					case 2:
						// Rendersettings
						if (tokens[0].equals("sizeX"))
							nset.sizeX = new Integer(tokens[1]);
						if (tokens[0].equals("sizeY"))
							nset.sizeY = new Integer(tokens[1]);
						if (tokens[0].equals("maxdepth"))
							nset.maxdepth = new Integer(tokens[1]);
						if (tokens[0].equals("AARays") || tokens[0].equals("maxRays"))
							nset.AARays = new Integer(tokens[1]);
						if (tokens[0].equals("useBVT"))
							nset.useBVT = (new Integer(tokens[1]) == 1);

						if (tokens[0].equals("headless"))
						{
							if (!initHeadlessTarget(tokens[1]))
							{
								System.err.println("Kann nicht headless rendern.");
								System.exit(1);
							}
						}

						if (tokens[0].equals("noLighting"))
							nset.noLighting = (new Integer(tokens[1]) == 1);
						if (tokens[0].equals("noShadowFeelers"))
							nset.noShadowFeelers = (new Integer(tokens[1]) == 1);
						if (tokens[0].equals("fakeDistanceShadow"))
							nset.fakeDistanceShadow = (new Integer(tokens[1]) == 1);
						if (tokens[0].equals("fakeDistanceScale"))
							nset.fakeDistanceScale = new Double(tokens[1]);
						if (tokens[0].equals("fakeDistanceExp"))
							nset.fakeDistanceExp = new Double(tokens[1]);

						if (tokens[0].equals("threads"))
							nset.threads = new Integer(tokens[1]);
						if (tokens[0].equals("rowstep"))
							nset.rowstep = new Integer(tokens[1]);
						if (tokens[0].equals("colorDelta"))
							nset.colorDelta = new Double(tokens[1]);

						// UniformMaterial fängt an
						if (tokens[0].equals("unimat") || tokens[0].equals("mat"))
							nmats.add(new UniformMaterial(in, tokens[1]));

						// ProceduralMaterial fängt an
						if (tokens[0].equals("procmat"))
							nmats.add(new ProceduralMaterial(in, tokens[1]));

						// TextureMaterial fängt an
						if (tokens[0].equals("texmat"))
							nmats.add(new TextureMaterial(in, tokens[1]));

						break;

					// Zweistellige Felder (Key Value)
					case 3:
						// Rendersettings
						if (tokens[0].equals("size"))
						{
							nset.sizeX = new Integer(tokens[1]);
							nset.sizeY = new Integer(tokens[2]);
						}

						break;

					case 4:
						// Umgebungsfarbe
						if (tokens[0].equals("environment"))
						{
							nset.environ = new RGBColor(
									new Double(tokens[1]),
									new Double(tokens[2]),
									new Double(tokens[3])
									);
						}

						break;
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Hoppla. Szene konnte nicht geladen werden:");
			e.printStackTrace();
			return false;
		}

		// Hier angekommen, dann ist alles klar. Übernimm das alles.
		set = nset;
		objects = nobjs.toArray(new Object3D[0]);
		lights = nlights.toArray(new Light[0]);
		// Cam aktualisieren
		eye.setResolution(set.sizeX, set.sizeY);
		return true;
	}

	/**
	 * Holt die Farbe mit diesem Namen aus der Liste
	 */
	public static Material getLoadedMaterial(String name, List<Material> mats)
	{
		for (Material m : mats)
			if (m.name.equals(name))
				return m;

		return null;
	}
}
