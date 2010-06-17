/*
	Copyright 2008, 2009, 2010  Peter Hofmann

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package raytracer.core;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.Serializable;

/**
 * Ganz einfaches Fenster, das im wesentlichen das OutputPanel enthält
 * und via Timer jede Sekunde den aktuellen Stand zeichnet
 */
public class OutputWindow extends JFrame implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private OutputPanel pnl = null;
	private final Scene toRender;
	private File letztesVerzeichnis = null;

	/**
	 * Irgendwann später soll das Panel neu gezeichnet werden.
	 */
	private void detachRepaint()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				pnl.repaint();
			}
		};
		t.start();
	}

	private void detachPhase(final int which, final int rays)
	{
		if (toRender == null)
			return;

		Thread t = new Thread()
		{
			public void run()
			{
				toRender.renderPhase(which, rays);
				detachRepaint();
			}
		};
		t.start();
	}

	/**
	 * Zeigt den Exportier-Dialog und exportiert ggf.
	 */
	private void showExportDialog()
	{
		// FileChooser mit Filterung bauen
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter()
		{
			public boolean accept(File f)
			{
				if (f.isDirectory()
					|| f.getName().toLowerCase().endsWith("png")
					|| f.getName().toLowerCase().endsWith("jpg"))
					return true;
				else
					return false;
			}
			public String getDescription()
			{
				return "PNG, JPG";
			}
		});

		// nur einzelne dateien waehlen koennen
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		if (letztesVerzeichnis != null)
		{
			chooser.setCurrentDirectory(letztesVerzeichnis);
		}

		// anzeigen
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			pnl.exportImage(chooser.getSelectedFile().getAbsolutePath());
		}

		letztesVerzeichnis = chooser.getCurrentDirectory();
	}

	/**
	 * Baut das Fenster auf und spawnt einen Thread, der im Hintergrund
	 * immer auf neue Pixel wartet.
	 */
	public OutputWindow(Scene toRender)
	{
		this.toRender = toRender;
		pnl = new OutputPanel(toRender);
		pnl.setPreferredSize(new Dimension(toRender.set.sizeX, toRender.set.sizeY));

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Raytracer");

		Container pane = getContentPane();

		if (!(pane.getLayout() instanceof BorderLayout))
		{
			// Falls durch irgendwelche Umstände hier kein BorderLayout als
			// Default genutzt wird, baue es auf die alte Weise

			setLayout(new GridLayout(1, 1));
			add(pnl);
		}
		else
		{
			// Nutze das BorderLayout für die Toolbar

			JToolBar toolBar = new JToolBar("Knöpfe");
			JButton btn;

			pane.add(pnl, BorderLayout.PAGE_END);
			pane.add(toolBar, BorderLayout.PAGE_START);

			// Bild exportieren
			btn = new JButton("Speichern");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					showExportDialog();
				}
			});
			toolBar.add(btn);

			// Ganz neu rendern
			btn = new JButton("Neu rendern");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					detachPhase(0, -1);
				}
			});
			toolBar.add(btn);

			// AA-Pixel togglen
			btn = new JButton("AA-Info");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					pnl.showCriticalPixels = !pnl.showCriticalPixels;
					detachRepaint();
				}
			});
			toolBar.add(btn);

			// Kritische Pixel neu suchen
			btn = new JButton("AA neu suchen");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					detachPhase(1, -1);
				}
			});
			toolBar.add(btn);

			// AA
			btn = new JButton("+ AA 2x2");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					detachPhase(2, 4);
				}
			});
			toolBar.add(btn);

			btn = new JButton("+ AA 4x4");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					detachPhase(2, 16);
				}
			});
			toolBar.add(btn);

			btn = new JButton("+ AA 64");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					detachPhase(2, 64);
				}
			});
			toolBar.add(btn);

			btn = new JButton("+ AA 256");
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					detachPhase(2, 256);
				}
			});
			toolBar.add(btn);
		}
		pack();

		final Scene tr = toRender;

		// Ein extra Thread zum Neu-Zeichnen, mit einer Queue, damit nix
		// verloren geht und fälschlicherweise Pixel stehenbleiben.
		Thread t = new Thread()
		{
			public void run()
			{
				while (true)
				{
					synchronized (tr.repaintQueue)
					{
						while (tr.repaintQueue.isEmpty())
						{
							try
							{
								tr.repaintQueue.wait();
							}
							catch (Exception e)
							{
								System.err.println("* Repaint thread died.");
								return;
							}
						}

						tr.repaintQueue.removeFirst();
					}

					detachRepaint();
				}
			}
		};
		t.start();
	}
}
