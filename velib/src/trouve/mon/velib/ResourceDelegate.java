package trouve.mon.velib;

import trouve.mon.velib.station.Station;
import trouve.mon.velib.station.Status;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class ResourceDelegate {

	// ----------------- Class Fields ------------------

	public static final int RED = Color.rgb(181, 12, 22);
	public static final int ORANGE = Color.rgb(215, 119, 34);
	public static final int GREEN = Color.rgb(133, 161, 82);

	// ----------------- Instance Fields ------------------

	private Resources resources;

	private Bitmap markerGreenFav;
	private Bitmap markerOrangeFav;
	private BitmapDescriptor markerRedFav;
	private BitmapDescriptor markerMidFav;
	private BitmapDescriptor markerTinyFav;

	private Bitmap markerGreen;
	private Bitmap markerOrange;
	private BitmapDescriptor markerRed;
	private BitmapDescriptor markerGreenMid;
	private BitmapDescriptor markerOrangeMid;
	private BitmapDescriptor markerRedMid;
	private BitmapDescriptor markerGreenTiny;
	private BitmapDescriptor markerOrangeTiny;
	private BitmapDescriptor markerRedTiny;

	private Bitmap markerGreenBig;
	private Bitmap markerOrangeBig;
	private Bitmap markerRedBig;
	
	private Bitmap markerGreenBigFav;
	private Bitmap markerOrangeBigFav;
	private Bitmap markerRedBigFav;

	private BitmapDescriptor fav;

	// ----------------- Instance Methods ------------------

	public ResourceDelegate(Resources resources) {
		this.resources = resources;
	}

	private Resources getResources() {
		return resources;
	}

	public BitmapDescriptor getFav() {
		if (fav == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_important, null);
			fav = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return fav;
	}

	private BitmapDescriptor getMarkerGreenMid() {
		if (markerGreenMid == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_green, null);
			markerGreenMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerGreenMid;
	}

	private BitmapDescriptor getMarkerOrangeMid() {
		if (markerOrangeMid == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_orange, null);
			markerOrangeMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerOrangeMid;
	}

	private BitmapDescriptor getMarkerRedMid() {
		if (markerRedMid == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_red, null);
			markerRedMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedMid;
	}

	private BitmapDescriptor getMarkerGreenTiny() {
		if (markerGreenTiny == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_green, null);
			markerGreenTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerGreenTiny;
	}

	private BitmapDescriptor getMarkerOrangeTiny() {
		if (markerOrangeTiny == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_orange, null);
			markerOrangeTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerOrangeTiny;
	}

	private BitmapDescriptor getMarkerRedTiny() {
		if (markerRedTiny == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_red, null);
			markerRedTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedTiny;
	}

	private Bitmap getMarkerGreen() {
		if (markerGreen == null) {
			markerGreen = BitmapFactory.decodeResource(getResources(), R.drawable.marker_green, null);
		}
		return markerGreen;
	}

	private Bitmap getMarkerOrange() {
		if (markerOrange == null) {
			markerOrange = BitmapFactory.decodeResource(getResources(), R.drawable.marker_orange, null);
		}
		return markerOrange;
	}

	private Bitmap getMarkerGreenBig() {
		if (markerGreenBig == null) {
			markerGreenBig = BitmapFactory.decodeResource(getResources(), R.drawable.big_marker_green, null);
		}
		return markerGreenBig;
	}

	private Bitmap getMarkerOrangeBig() {
		if (markerOrangeBig == null) {
			markerOrangeBig = BitmapFactory.decodeResource(getResources(), R.drawable.big_marker_orange, null);
		}
		return markerOrangeBig;
	}

	private Bitmap getMarkerRedBig() {
		if (markerRedBig == null) {
			markerRedBig = BitmapFactory.decodeResource(getResources(), R.drawable.big_marker_red, null);
		}
		return markerRedBig;
	}
	private Bitmap getMarkerGreenBigFav() {
		if (markerGreenBigFav == null) {
			markerGreenBigFav = BitmapFactory.decodeResource(getResources(), R.drawable.big_marker_fav_green, null);
		}
		return markerGreenBigFav;
	}

	private Bitmap getMarkerOrangeBigFav() {
		if (markerOrangeBigFav == null) {
			markerOrangeBigFav = BitmapFactory.decodeResource(getResources(), R.drawable.big_marker_fav_orange, null);
		}
		return markerOrangeBigFav;
	}

	private Bitmap getMarkerRedBigFav() {
		if (markerRedBigFav == null) {
			markerRedBigFav = BitmapFactory.decodeResource(getResources(), R.drawable.big_marker_fav_red, null);
		}
		return markerRedBigFav;
	}

	private BitmapDescriptor getMarkerRedBitmapDescriptor() {
		if (markerRed == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_red, null).copy(Bitmap.Config.ARGB_8888, true);
			markerRed = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRed;
	}

	private Bitmap getMarkerGreenFav() {
		if (markerGreenFav == null) {
			markerGreenFav = BitmapFactory.decodeResource(getResources(), R.drawable.fav_marker_green, null);
		}
		return markerGreenFav;
	}

	private Bitmap getMarkerOrangeFav() {
		if (markerOrangeFav == null) {
			markerOrangeFav = BitmapFactory.decodeResource(getResources(), R.drawable.fav_marker_orange, null);
		}
		return markerOrangeFav;
	}

	private BitmapDescriptor getMarkerRedFavBitmapDescriptor() {
		if (markerRedFav == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fav_marker_red, null).copy(Bitmap.Config.ARGB_8888, true);
			markerRedFav = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedFav;
	}

	private BitmapDescriptor getMarkerMidFav() {
		if (markerMidFav == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fav_marker_mid, null);
			markerMidFav = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerMidFav;
	}

	private BitmapDescriptor getMarkerTinyFav() {
		if (markerTinyFav == null) {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fav_marker_tiny, null);
			markerTinyFav = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerTinyFav;
	}

	private int getTextSize() {
		return getResources().getDimensionPixelSize(R.dimen.text_size);
	}

	private int getBigTextSize() {
		return getResources().getDimensionPixelSize(R.dimen.big_text_size);
	}

	private int getCenterStand() {
		return getResources().getDimensionPixelSize(R.dimen.center_stand);
	}

	private int getCenterBike() {
		return getResources().getDimensionPixelSize(R.dimen.center_bike);
	}

	private int getCenterBigStand() {
		return getResources().getDimensionPixelSize(R.dimen.center_big_stand);
	}

	private int getCenterBigBike() {
		return getResources().getDimensionPixelSize(R.dimen.center_big_bike);
	}

	private int getCenterBigX() {
		return getResources().getDimensionPixelSize(R.dimen.center_big_x);
	}

	public BitmapDescriptor getOpenNormalMarkerBitmapDescriptor(Station station) {
		int color, bikes = station.getAvailableBikes(), stands = station.getAvailableBikeStands();
		Bitmap bitmap = null;
		if (bikes <= 3 || stands <= 3) {
			if (station.isFavorite())
				bitmap = getMarkerOrangeFav().copy(Bitmap.Config.ARGB_8888, true);
			else
				bitmap = getMarkerOrange().copy(Bitmap.Config.ARGB_8888, true);
			color = ORANGE;
		} else {
			if (station.isFavorite())
				bitmap = getMarkerGreenFav().copy(Bitmap.Config.ARGB_8888, true);
			else
				bitmap = getMarkerGreen().copy(Bitmap.Config.ARGB_8888, true);
			color = GREEN;
		}

		Canvas canvas = new Canvas(bitmap);
		Paint textPaint = new Paint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(getTextSize());
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setColor(color);
		canvas.drawText(String.valueOf(bikes), bitmap.getWidth() / 2, bitmap.getHeight() / 2 - getCenterBike(), textPaint);
		canvas.drawText(String.valueOf(stands), bitmap.getWidth() / 2, bitmap.getHeight() / 2 + getCenterStand(), textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

	public BitmapDescriptor getTinyMarkerBitmapDescriptor(Station station) {
		int bikes = station.getAvailableBikes(), stands = station.getAvailableBikeStands();
		if (station.isFavorite()) {
			return getMarkerTinyFav();
		} else if (bikes == 0 && stands == 0) {
			return getMarkerRedTiny();
		} else if (bikes <= 3 || stands <= 3) {
			return getMarkerOrangeTiny();
		} else {
			return getMarkerGreenTiny();
		}
	}

	public BitmapDescriptor getMidMarkerBitmapDescriptor(Station station) {
		int bikes = station.getAvailableBikes(), stands = station.getAvailableBikeStands();
		if (station.isFavorite()) {
			return getMarkerMidFav();
		} else if (bikes == 0 && stands == 0) {
			return getMarkerRedMid();
		} else if (bikes <= 3 || stands <= 3) {
			return getMarkerOrangeMid();
		} else {
			return getMarkerGreenMid();
		}
	}

	public BitmapDescriptor getNormalMarkerBitmapDescriptor(Station station) {
		if (station.getStatus() == Status.OPEN) {
			return getOpenNormalMarkerBitmapDescriptor(station);
		} else if (station.isFavorite()) {
			return getMarkerRedFavBitmapDescriptor();
		} else {
			return getMarkerRedBitmapDescriptor();
		}
	}

	public BitmapDescriptor getBigMarkerBitmapDescriptor(Station station) {
		int color, bikes = station.getAvailableBikes(), stands = station.getAvailableBikeStands();
		Bitmap bitmap = null;
		if (station.getStatus() == Status.CLOSED) {
			if (station.isFavorite())
				bitmap = getMarkerRedBigFav().copy(Bitmap.Config.ARGB_8888, true);
			else
				bitmap = getMarkerRedBig().copy(Bitmap.Config.ARGB_8888, true);
			color = RED;
		} else if (bikes <= 3 || stands <= 3) {
			if (station.isFavorite())
				bitmap = getMarkerOrangeBigFav().copy(Bitmap.Config.ARGB_8888, true);
			else
				bitmap = getMarkerOrangeBig().copy(Bitmap.Config.ARGB_8888, true);
			color = ORANGE;
		} else {
			if (station.isFavorite())
				bitmap = getMarkerGreenBigFav().copy(Bitmap.Config.ARGB_8888, true);
			else
				bitmap = getMarkerGreenBig().copy(Bitmap.Config.ARGB_8888, true);
			color = GREEN;
		}

		Canvas canvas = new Canvas(bitmap);
		Paint textPaint = new Paint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(getBigTextSize());
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setColor(color);
		canvas.drawText(String.valueOf(bikes), bitmap.getWidth() / 2 - getCenterBigX(), bitmap.getHeight() / 2 - getCenterBigBike(), textPaint);
		canvas.drawText(String.valueOf(stands), bitmap.getWidth() / 2 - getCenterBigX(), bitmap.getHeight() / 2 + getCenterBigStand(), textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

}
