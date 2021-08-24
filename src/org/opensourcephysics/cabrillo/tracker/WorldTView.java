/*
* The tracker package defines a set of video/image analysis tools
* built on the Open Source Physics framework by Wolfgang Christian.
*
* Copyright (c) 2021 Douglas Brown, Wolfgang Christian, Robert M. Hanson
*
* Tracker is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 3 of the License, or
* (at your option) any later version.
*
* Tracker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tracker; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
* or view the license online at <http://www.gnu.org/copyleft/gpl.html>
*
* For additional Tracker information and documentation, please see
* <http://physlets.org/tracker/>.
*/
package org.opensourcephysics.cabrillo.tracker;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;

import org.opensourcephysics.cabrillo.tracker.TrackerIO.ComponentImage;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.TPoint;
import org.opensourcephysics.media.core.VideoPlayer;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.FunctionTool;

/**
 * This is a TView of a TrackerPanel drawn in world space. It is a JPanel with a single component, WorldPanel. 
 * 
 * An unusual TView,
 * WorldTView is not just a JPanel, it is a full TrackerPanel. A distinction is made for several tracks, including CenterOfMass, PointMass,
 * Vector, and VectorStep, all of which call getDisplayedPanel() in order to get the displayed panel. 
 *
 * 
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class WorldTView extends TView {

	protected static final Icon WORLDVIEW_ICON = Tracker.getResourceIcon("axes.gif", true); //$NON-NLS-1$ ;

	private static final String[] panelProps = new String[] { TrackerPanel.PROPERTY_TRACKERPANEL_SIZE,
			TrackerPanel.PROPERTY_TRACKERPANEL_STEPNUMBER, TrackerPanel.PROPERTY_TRACKERPANEL_VIDEO,
			TrackerPanel.PROPERTY_TRACKERPANEL_IMAGE, TrackerPanel.PROPERTY_TRACKERPANEL_VIDEOVISIBLE,
			TrackerPanel.PROPERTY_TRACKERPANEL_MAGNIFICATION, ImageCoordSystem.PROPERTY_COORDS_TRANSFORM,
			TTrack.PROPERTY_TTRACK_DATA };



	// instance fields
	protected Integer mainPanelID;
	protected WorldPanel worldPanel;

	protected JLabel worldViewLabel;
	
	/**
	 * Constructs a WorldTView of the specified TrackerPanel
	 *
	 * @param panel the tracker panel to be viewed
	 */
	public WorldTView(TrackerPanel panel) {
		super(panel);
		worldViewLabel = new JLabel();
		worldViewLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 0));
		toolbarComponents.add(worldViewLabel);
		worldPanel = new WorldPanel(panel);
		add(worldPanel);
	}

	class WorldPanel extends TrackerPanel {

		protected JMenuItem copyImageItem;
		protected JMenuItem printItem;
		protected JMenuItem helpItem;

		@Override
		public boolean isWorldPanel() {
			return true;
		}

		private WorldPanel(TrackerPanel panel) {
			super(panel.frame);
			andWorld.clear();
			System.out.println("WorldTView init " + worldPanel + " " + panel);
			panel.andWorld.add(panelID);
			mainPanelID = panel.getID();
			initWP();
			setPlayerVisible(false);
			setDrawingInImageSpace(false);
			setPreferredSize(new Dimension(240, 180));
			setShowCoordinates(false);
			// world view button
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					mousePressedWP(e);
				}

			});
		}

		protected void mousePressedWP(MouseEvent e) {
			if (OSPRuntime.isPopupTrigger(e)) {
				createWorldPopup();
				popup.show(this, e.getX(), e.getY());
			}
		}

		private void initWP() {
			cleanup();
			// add this view to tracker panel listeners
			// note "track" and "clear" not needed since forwarded from TViewChooser
			TrackerPanel trackerPanel = getMainPanel();
			trackerPanel.addListeners(panelProps, worldPanel);
			// add this view to track listeners
			for (TTrack track : trackerPanel.getTracks()) {
				track.addPropertyChangeListener(TTrack.PROPERTY_TTRACK_COLOR, this); // $NON-NLS-1$
			}
		}

		protected void createWorldPopup() {
			getPopup().removeAll();
			getMenuItems();
			TrackerPanel trackerPanel = getMainPanel();
			if (trackerPanel.isEnabled("edit.copyImage")) { //$NON-NLS-1$
				copyImageItem.setText(TrackerRes.getString("TMenuBar.Menu.CopyImage")); //$NON-NLS-1$
				popup.add(copyImageItem);
				popup.add(snapshotItem);
			}
			if (trackerPanel.isEnabled("file.print")) { //$NON-NLS-1$
				if (popup.getComponentCount() > 0)
					popup.addSeparator();
				printItem.setText(TrackerRes.getString("TActions.Action.Print")); //$NON-NLS-1$
				popup.add(printItem);
			}
			if (popup.getComponentCount() > 0)
				popup.addSeparator();
			helpItem.setText(TrackerRes.getString("Tracker.Popup.MenuItem.Help")); //$NON-NLS-1$
			popup.add(helpItem);
			FontSizer.setFonts(popup, FontSizer.getLevel());
		}
		
		protected void getMenuItems() {
			if (copyImageItem != null)
				return;
			// copy image item
			Action copyImageAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					copyImage("clipboard");
				}
			};
			copyImageItem = new JMenuItem(copyImageAction);
			// print menu item
			Action printAction = new AbstractAction(TrackerRes.getString("TActions.Action.Print"), null) { //$NON-NLS-1$
				@Override
				public void actionPerformed(ActionEvent e) {
					copyImage("print");
				}
			};
			printItem = new JMenuItem(printAction);
			// help item
			helpItem = new JMenuItem();
			helpItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (frame != null) {
						frame.showHelp("world", 0); //$NON-NLS-1$
					}
				}
			});
		}

		protected void copyImage(String where) {
			ComponentImage img = new TrackerIO.ComponentImage(this);
			switch (where) {
			case "clipboard":
				img.copyToClipboard();
				break;
			case "print":
				img.print();
				break;
			}
		}

		public void refresh() {
			// axes & tape items
			TrackerPanel trackerPanel = getMainPanel();
			CoordAxes axes = trackerPanel.getAxes();
			if (axes != null) {
				axes.updateListenerVisible(this);
			}
			if (!trackerPanel.calibrationTools.isEmpty()) {
				for (TTrack next : trackerPanel.getTracks()) {
					if (trackerPanel.calibrationTools.contains(next)) {
						next.updateListenerVisible(this);
					}
				}
			}
			Iterator<Drawable> it = getDrawables().iterator();
			while (it.hasNext()) {
				Object next = it.next();
				if (next instanceof TTrack) {
					TTrack track = (TTrack) next;
					track.erase(panelID);
				}
			}
			TFrame.repaintT(this);
		}

		@Override
		public TrackerPanel getMainPanel() {
			return frame.getTrackerPanelForID(mainPanelID);
		}


		/**
		 * Overrides TrackerPanel getSnapPoint method.
		 *
		 * @return the snap point
		 */
		@Override
		public TPoint getSnapPoint() {
			return getMainPanel().getSnapPoint();
		}

		/**
		 * Overrides TrackerPanel getSelectedTrack method. Gets the selected track of
		 * trackerPanel.
		 *
		 * @return the selected track
		 */
		@Override
		public TTrack getSelectedTrack() {
			return getMainPanel().getSelectedTrack();
		}

		/**
		 * Sets the selected track
		 *
		 * @param track the track to select
		 */
		@Override
		public void setSelectedTrack(TTrack track) {
			if (mainPanelID != null)
				getMainPanel().setSelectedTrack(track);
		}

		/**
		 * Overrides DrawingPanel getDrawables method. Returns all drawables in the
		 * tracker panel plus those in this world view.
		 *
		 * @return a list of Drawable objects
		 */
		@Override
		public ArrayList<Drawable> getDrawables() {
			if (mainPanelID == null) {
				return super.getDrawables();
			}
			TrackerPanel trackerPanel = getMainPanel();
			// return all drawables in trackerPanel (except PencilScenes) plus those in this
			// world view
			ArrayList<Drawable> list = trackerPanel.getDrawables();
			list.addAll(super.getDrawables());
			// remove PencilScenes
			list.removeAll(trackerPanel.getDrawablesTemp(PencilScene.class));
			trackerPanel.clearTemp();
			// put mat behind everything
			TMat mat = trackerPanel.getMat();
			if (mat != null && list.get(0) != mat) {
				list.remove(mat);
				list.add(0, mat);
			}
//			// remove noData message if trackerPanel is not empty
//			if (!trackerPanel.isEmpty)
//				remove(noData);
			return list;
		}

		/**
		 * Overrides VideoPanel getPlayer method. Returns the tracker panel's player.
		 *
		 * @return the video player
		 */
		@Override
		public VideoPlayer getPlayer() {
			// workaround to prevent null pointer exception during instantiation
			return (mainPanelID == null ? super.getPlayer() : getMainPanel().getPlayer());
		}

		/**
		 * Overrides VideoPanel getCoords method. Returns the tracker panel's coords.
		 *
		 * @return the current image coordinate system
		 */
		@Override
		public ImageCoordSystem getCoords() {
			// workaround to prevent null pointer exception during instantiation
			return (mainPanelID == null ? super.getCoords() : getMainPanel().getCoords());
		}

		@Override
		protected boolean unTracked() {
			return false;
		}

		/**
		 * Overrides InteractivePanel getInteractive method.
		 * 
		 * @return null
		 */
		@Override
		public Interactive getInteractive() {
			return null;
		}

		/**
		 * Configures this panel. Overrides TrackerPanel method.
		 */
		@Override
		protected void configure() {
			// set tiny preferred size so auto zooms to very small
			setPreferredSize(new Dimension(1, 1));
//			coords.addPropertyChangeListener(this);
			// remove DrawingPanel option controller
			removeOptionController();
		}

		public void cleanup() {
			// remove this listener from tracker panel
			if (mainPanelID != null) {
				getMainPanel().removeListeners(panelProps, this);
				// remove this listener from tracks
				for (Integer n : TTrack.panelActiveTracks.keySet()) {
					TTrack.panelActiveTracks.get(n).removePropertyChangeListener(TTrack.PROPERTY_TTRACK_COLOR, this); // $NON-NLS-1$
				}
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent e) {
			propertyChangeImpl(e);
		}
		
		@Override
		public void dispose() {
			cleanup();
			if (mainPanelID != null) {
				TrackerPanel trackerPanel = getMainPanel();
				trackerPanel.removePropertyChangeListener(TrackerPanel.PROPERTY_TRACKERPANEL_CLEAR, this);
				trackerPanel.removePropertyChangeListener(FunctionTool.PROPERTY_FUNCTIONTOOL_FUNCTION, this);
				mainPanelID = null;
			}
			toolbarComponents = null;
			frame.deallocatePanelID(panelID);
			super.dispose();
		}

		public WorldTView getWorldView() {
			return WorldTView.this;
		}

		public boolean isActive() {
			return (TViewChooser.isSelectedView(WorldTView.this) && isViewPaneVisible());
		}


	} // end WorldPanel
	
	

	/**
	 * Refreshes all tracks
	 */
	@Override
	public void refresh() {
		if (!isViewPaneVisible())
			return;
		worldViewLabel.setText(TrackerRes.getString("WorldTView.Button.World")); //$NON-NLS-1$
		worldPanel.refresh();
	}

	/**
	 * Initializes this view
	 */
	@Override
	public void init() {
		worldPanel.initWP();
	}

	/**
	 * Cleans up this view
	 */
	@Override
	public void cleanup() {
		worldPanel.cleanup();
	}

	
	/**
	 * Disposes of the 
	 */
	@Override
	public void dispose() {
		worldPanel.dispose();
		worldPanel = null;
		super.dispose();
	}

	/**
	 * Gets the tracker panel being viewed
	 *
	 * @return the tracker panel being viewed
	 */
	@Override
	public TrackerPanel getTrackerPanel() {
		return worldPanel.getMainPanel();
	}

	/**
	 * Gets the name of the view
	 *
	 * @return the name of the view
	 */
	@Override
	public String getViewName() {
		return TrackerRes.getString("TFrame.View.World"); //$NON-NLS-1$
	}

	/**
	 * Gets the icon for this view
	 *
	 * @return the icon for the view
	 */
	@Override
	public Icon getViewIcon() {
		return WORLDVIEW_ICON;
	}

	/**
	 * Gets the type of view
	 *
	 * @return one of the defined types
	 */
	@Override
	public int getViewType() {
		return TView.VIEW_WORLD;
	}

	/**
	 * Gets the toolbar components
	 *
	 * @return an ArrayList of components to be added to a toolbar
	 */
	@Override
	public ArrayList<Component> getToolBarComponents() {
		worldViewLabel.setText(TrackerRes.getString("WorldTView.Button.World")); //$NON-NLS-1$
		return super.getToolBarComponents();
	}

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		propertyChangeImpl(e);
	}


	private void propertyChangeImpl(PropertyChangeEvent e) {
		// coming to WorldPanel or WorldView
		switch (e.getPropertyName()) {
		case TrackerPanel.PROPERTY_TRACKERPANEL_TRACK:
			if (e.getOldValue() != null) { // track removed
				TTrack removed = (TTrack) e.getOldValue();
				removed.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_COLOR, this); // $NON-NLS-1$
				removed.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_VISIBLE, this); // $NON-NLS-1$
			}
			refresh();
			break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_CLEAR:
			for (Integer n : TTrack.panelActiveTracks.keySet()) {
				TTrack track = TTrack.panelActiveTracks.get(n);
				track.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_COLOR, this); // $NON-NLS-1$
				track.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_VISIBLE, this); // $NON-NLS-1$
			}
			refresh();
			break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_STEPNUMBER:
		case TrackerPanel.PROPERTY_TRACKERPANEL_IMAGE:
		case TrackerPanel.PROPERTY_TRACKERPANEL_VIDEO:
		case TrackerPanel.PROPERTY_TRACKERPANEL_VIDEOVISIBLE:
		case TTrack.PROPERTY_TTRACK_COLOR:
		case TTrack.PROPERTY_TTRACK_VISIBLE:
			TFrame.repaintT(worldPanel);
			break;
		case ImageCoordSystem.PROPERTY_COORDS_TRANSFORM:
		case TrackerPanel.PROPERTY_TRACKERPANEL_SIZE:
		case TTrack.PROPERTY_TTRACK_DATA:
			refresh();
			break;
		default:
			System.err.println("WoldTView.propertyChange " + e.getPropertyName() + " " + e.getSource());
			break;
		}
		// no sending to super? 
		// no worldPanel.propertyChange(e); ? Original did not pass, either
	}

	/**
	 * Returns an XML.ObjectLoader to save and load object data.
	 *
	 * @return the XML.ObjectLoader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load object data.
	 */
	static class Loader implements XML.ObjectLoader {

		/**
		 * Saves object data.
		 *
		 * @param control the control to save to
		 * @param obj     the TrackerPanel object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			/** empty block */
		}

		/**
		 * Creates an object.
		 *
		 * @param control the control
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			return null;
		}

		/**
		 * Loads an object with data from an XMLControl.
		 *
		 * @param control the control
		 * @param obj     the object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			return obj;
		}

	}

}
