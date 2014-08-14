package trouve.mon.velib.station;

import trouve.mon.velib.ResourceFactory;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.util.SparseArray;

public class MarkerManager {

	enum MarkerSize {
		TINY, MID, NORMAL, BIG
	}

	// ----------------- Static Fields ------------------

	private static final float HIGHLIGHT_ALPHA = 0.4f;
	private static final float NORMAL_ALPHA = 1.0f;

	private static final float MID_ZOOM_LEVEL = 14.8f;
	private static final float TINY_ZOOM_LEVEL = 13.8f;
	private static final float MAX_ZOOM_LEVEL = 13.0f;

	// ----------------- Instance Fields ------------------

	private SparseArray<MarkerWrapper> normalMarkers = new SparseArray<MarkerWrapper>(20);
	private SparseArray<MarkerWrapper> midMarkers = new SparseArray<MarkerWrapper>(150);
	private SparseArray<MarkerWrapper> tinyMarkers = new SparseArray<MarkerWrapper>(600);

	private final GoogleMap map;
	private ResourceFactory resourceFactory;

	public boolean detailing = false;
	public int detailedStationNumber;
	public boolean actionIfNoStation = false;

	// ----------------- Constructor ------------------

	public MarkerManager(GoogleMap map, ResourceFactory delegate) {
		this.map = map;
		this.resourceFactory = delegate;
	}

	// ----------------- Instance Methods ------------------

	// TODO should check for GC and leak
	@SuppressWarnings("unchecked")
	public void refreshMarkers(boolean dataUpdated) {
		if (this.map != null) {
			SparseArray<MarkerWrapper> markerWrappers;
			MarkerSize size;

			if (map.getCameraPosition().zoom > MID_ZOOM_LEVEL) {
				resetMarkers(tinyMarkers, midMarkers);
				markerWrappers = normalMarkers;
				size = MarkerSize.NORMAL;
			} else if (map.getCameraPosition().zoom > TINY_ZOOM_LEVEL) {
				resetMarkers(normalMarkers, tinyMarkers);
				markerWrappers = midMarkers;
				size = MarkerSize.MID;
			} else if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
				resetMarkers(normalMarkers, midMarkers);
				markerWrappers = tinyMarkers;
				size = MarkerSize.TINY;
			} else {
				resetMarkers(normalMarkers, midMarkers, tinyMarkers);
				return;
			}

			LatLngBounds bounds = this.map.getProjection().getVisibleRegion().latLngBounds;
			SparseArray<Station> stations = StationManager.INSTANCE.getStationMap();
			for (int i = 0, nsize = stations.size(); i < nsize; i++) {
				Station station = stations.valueAt(i);
				MarkerWrapper markerWrapper = markerWrappers.get(station.getNumber());
				// if station is visible
				if (bounds.contains(station.getPosition())) {
					actionIfNoStation = false;
					if (markerWrapper == null) { // if there is no marker yet
						markerWrappers.put(station.getNumber(), new MarkerWrapper(addMarker(station, size), station));
					} else if (dataUpdated && station.isDifferent(markerWrapper)) {
						markerWrapper.getMarker().remove();
						MarkerSize s = size;
						if (detailing && station.getNumber() == detailedStationNumber) {
							s = (size == MarkerSize.NORMAL) ? MarkerSize.BIG : MarkerSize.NORMAL;
						}
						markerWrappers.put(station.getNumber(), new MarkerWrapper(addMarker(station, s), station));
					}
				} else { // station is not visible any more
							// if there is a marker and just updated
					if (markerWrapper != null && dataUpdated) {
						if (station.isDifferent(markerWrapper)) {
							markerWrapper.getMarker().remove();
							markerWrappers.remove(station.getNumber());
						}
					}
				}
			}

			goAwayIfNoStation();
		}
	}

	private void goAwayIfNoStation() {
		if (actionIfNoStation) {
			actionIfNoStation = false;
			centerMapFarAway();
		}
	}

	@SuppressWarnings("unchecked")
	public void resetAllMarkers() {
		resetMarkers(tinyMarkers, midMarkers, normalMarkers);
	}

	private void resetMarkers(SparseArray<MarkerWrapper>... arguments) {
		for (SparseArray<MarkerWrapper> markerWrappers : arguments) {
			for (int i = 0, nsize = markerWrappers.size(); i < nsize; i++) {
				markerWrappers.valueAt(i).getMarker().remove();
			}
			markerWrappers.clear();
		}
	}

	private Marker addMarker(Station station, MarkerSize markerSize) {

		MarkerOptions markerOptions = new MarkerOptions().position(station.getPosition()).title(String.valueOf(station.getNumber()));

		BitmapDescriptor descriptor = null;
		switch (markerSize) {
		case TINY:
			descriptor = resourceFactory.getTinyMarkerBitmapDescriptor(station);
			break;
		case MID:
			descriptor = resourceFactory.getMidMarkerBitmapDescriptor(station);
			break;
		case NORMAL:
			descriptor = resourceFactory.getNormalMarkerBitmapDescriptor(station);
			break;
		case BIG:
			descriptor = resourceFactory.getBigMarkerBitmapDescriptor(station);
			break;
		}
		markerOptions.icon(descriptor);
		if (detailing && detailedStationNumber != station.getNumber()) {
			markerOptions.alpha(HIGHLIGHT_ALPHA);
		}

		return map.addMarker(markerOptions);
	}

	public void highlightMarker(int stationNumber) {
		resetToNormalMarker();
		detailing = true;
		detailedStationNumber = stationNumber;

		Station station = StationManager.INSTANCE.getStationMap().get(detailedStationNumber);
		MarkerWrapper markerWrapper = getMarkerWrapper(detailedStationNumber);
		if (markerWrapper != null) {
			markerWrapper.getMarker().remove();
		}

		Marker marker = null;
		if (map.getCameraPosition().zoom > MID_ZOOM_LEVEL) {
			marker = addMarker(station, MarkerSize.BIG);
			normalMarkers.put(detailedStationNumber, new MarkerWrapper(marker, station));
			for (int i = 0, nsize = normalMarkers.size(); i < nsize; i++)
				normalMarkers.valueAt(i).getMarker().setAlpha(HIGHLIGHT_ALPHA);
		} else if (map.getCameraPosition().zoom > TINY_ZOOM_LEVEL) {
			marker = addMarker(station, MarkerSize.NORMAL);
			midMarkers.put(detailedStationNumber, new MarkerWrapper(marker, station));
			for (int i = 0, nsize = midMarkers.size(); i < nsize; i++)
				midMarkers.valueAt(i).getMarker().setAlpha(HIGHLIGHT_ALPHA);
		} else {
			marker = addMarker(station, MarkerSize.NORMAL);
			tinyMarkers.put(detailedStationNumber, new MarkerWrapper(marker, station));
			for (int i = 0, nsize = tinyMarkers.size(); i < nsize; i++)
				tinyMarkers.valueAt(i).getMarker().setAlpha(HIGHLIGHT_ALPHA);
		}

		marker.setAlpha(NORMAL_ALPHA);
	}

	public void resetToNormalMarker() {
		if (detailing) {
			MarkerWrapper markerWrapper = getMarkerWrapper(detailedStationNumber);
			if (markerWrapper != null) {
				markerWrapper.getMarker().remove();
			}

			Station station = StationManager.INSTANCE.getStationMap().get(detailedStationNumber);

			if (map.getCameraPosition().zoom > MID_ZOOM_LEVEL) {
				normalMarkers.put(detailedStationNumber, new MarkerWrapper(addMarker(station, MarkerSize.NORMAL), station));
			} else if (map.getCameraPosition().zoom > TINY_ZOOM_LEVEL) {
				midMarkers.put(detailedStationNumber, new MarkerWrapper(addMarker(station, MarkerSize.MID), station));
			} else if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
				tinyMarkers.put(detailedStationNumber, new MarkerWrapper(addMarker(station, MarkerSize.TINY), station));
			}
		}
	}

	public void unhighlightMarker() {
		resetToNormalMarker();
		detailing = false;

		for (int i = 0, nsize = normalMarkers.size(); i < nsize; i++)
			normalMarkers.valueAt(i).getMarker().setAlpha(NORMAL_ALPHA);
		for (int i = 0, nsize = midMarkers.size(); i < nsize; i++)
			midMarkers.valueAt(i).getMarker().setAlpha(NORMAL_ALPHA);
		for (int i = 0, nsize = tinyMarkers.size(); i < nsize; i++)
			tinyMarkers.valueAt(i).getMarker().setAlpha(NORMAL_ALPHA);
	}

	private MarkerWrapper getMarkerWrapper(int number) {
		MarkerWrapper markerWrapper = tinyMarkers.get(number);
		if (markerWrapper == null) {
			markerWrapper = midMarkers.get(number);
			if (markerWrapper == null) {
				markerWrapper = normalMarkers.get(number);
			}
		}
		return markerWrapper;
	}

	public void updateMarker(Station station) {
		MarkerSize size = null;
		SparseArray<MarkerWrapper> current = null;
		if (map.getCameraPosition().zoom > MID_ZOOM_LEVEL) {
			size = detailing ? MarkerSize.BIG : MarkerSize.NORMAL;
			current = normalMarkers;
		} else if (map.getCameraPosition().zoom > TINY_ZOOM_LEVEL) {
			size = detailing ? MarkerSize.NORMAL :MarkerSize.MID;
			current = midMarkers;
		} else if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
			size = detailing ? MarkerSize.NORMAL :MarkerSize.TINY;
			current = tinyMarkers;
		}

		if (size != null) {
			MarkerWrapper markerWrapper = getMarkerWrapper(station.getNumber());
			if (markerWrapper != null)
				markerWrapper.getMarker().remove();

			current.put(station.getNumber(), new MarkerWrapper(addMarker(station, size), station));
		}
	}

	private void centerMapFarAway() {
		SparseArray<Station> stations = StationManager.INSTANCE.getStationMap();
		if (stations.size() != 0) {
			Station station = stations.valueAt(0);
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getPosition(), TINY_ZOOM_LEVEL));
		}
	}

}
