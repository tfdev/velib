package trouve.mon.velib;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public class ResourceDelegate {

	//-----------------  Class Fields ------------------
	
	public static final int RED 	= Color.rgb(181, 12, 22);
	public static final int ORANGE 	= Color.rgb(215, 119, 34);
	public static final int GREEN 	= Color.rgb(133, 161, 82);
	
	//-----------------  Instance Fields ------------------
	
	private Resources resources;
	
	private Bitmap markerGreen;
	private Bitmap markerOrange;
	private BitmapDescriptor markerRed;
	private BitmapDescriptor markerGreenMid;
	private BitmapDescriptor markerOrangeMid;
	private BitmapDescriptor markerRedMid;
	private BitmapDescriptor markerGreenTiny;
	private BitmapDescriptor markerOrangeTiny;
	private BitmapDescriptor markerRedTiny;
	
	//-----------------  Instance Methods ------------------
	
	public ResourceDelegate(Resources resources) {
		this.resources = resources;
	}

	private Resources getResources() {
		return resources;
	}
	
	public BitmapDescriptor getMarkerGreenMid() {
		if(markerGreenMid == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_green, null);
			markerGreenMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerGreenMid;
	}
	public BitmapDescriptor getMarkerOrangeMid() {
		if(markerOrangeMid == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_orange, null);
			markerOrangeMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerOrangeMid;
	}
	public BitmapDescriptor getMarkerRedMid() {
		if(markerRedMid == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_red, null);
			markerRedMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedMid;
	}
	public BitmapDescriptor getMarkerGreenTiny() {
		if(markerGreenTiny == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_green, null);
			markerGreenTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerGreenTiny;
	}
    public BitmapDescriptor getMarkerOrangeTiny() {
		if(markerOrangeTiny == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_orange, null);
			markerOrangeTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerOrangeTiny;
	}
	public BitmapDescriptor getMarkerRedTiny() {
		if(markerRedTiny == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_red, null);
			markerRedTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedTiny;
	}
	public Bitmap getMarkerGreen() {
		if(markerGreen == null){
			markerGreen = BitmapFactory.decodeResource(getResources(), R.drawable.markergreen, null);
		}
		return markerGreen;
	}
	public Bitmap getMarkerOrange() {
		if(markerOrange == null){
			markerOrange = BitmapFactory.decodeResource(getResources(), R.drawable.markerorange, null);
		}
		return markerOrange;
	}
	public BitmapDescriptor getMarkerRedBitmapDescriptor() {
		if(markerRed == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.markerred, null)
									.copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(bitmap);		
			Paint textPaint = new Paint();
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setTextSize(getBigTextSize());
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);
			textPaint.setColor(RED);
			canvas.drawText("0", bitmap.getWidth()/2, bitmap.getHeight()/2 - getCenterClosed(), textPaint);

			markerRed = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRed;
	}
	private int getCenterClosed(){
		return getResources().getDimensionPixelSize(R.dimen.center_closed);
	}
	private int getTextSize(){
		return getResources().getDimensionPixelSize(R.dimen.text_size);
	}
	private int getBigTextSize(){
		return getResources().getDimensionPixelSize(R.dimen.big_text_size);
	}
	private int getCenterStand(){
		return getResources().getDimensionPixelSize(R.dimen.center_stand);
	}
	private int getCenterBike(){
		return getResources().getDimensionPixelSize(R.dimen.center_bike);
	}
	public BitmapDescriptor getOpenMarkerBitmapDescriptor(int bikes, int stands) {
		int color;
		Bitmap bitmap = null;
		if (bikes <= 3 || stands <= 3){
			bitmap = getMarkerOrange().copy(Bitmap.Config.ARGB_8888, true);
			color = ORANGE;
		}else{
			bitmap = getMarkerGreen().copy(Bitmap.Config.ARGB_8888, true);
			color = GREEN;
		}

		Canvas canvas = new Canvas(bitmap);		
		Paint textPaint = new Paint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(getTextSize());
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setColor(color);
		canvas.drawText(String.valueOf(bikes), bitmap.getWidth()/2, bitmap.getHeight()/2 - getCenterBike(), textPaint);
		canvas.drawText(String.valueOf(stands), bitmap.getWidth()/2, bitmap.getHeight()/2 + getCenterStand(), textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	public BitmapDescriptor getTinyMarkerBitmapDescriptor(Station station) {
		int bikes = station.getAvailableBikes(), 
				stands=	station.getAvailableBikeStands();
		if(bikes == 0 && stands == 0){
			return getMarkerRedTiny();
		}else if (bikes <= 3 || stands <= 3){
			return getMarkerOrangeTiny();
		}else{
			return getMarkerGreenTiny();
		}
	}
	public BitmapDescriptor getMidMarkerBitmapDescriptor(Station station) {
		int bikes = station.getAvailableBikes(), 
			stands=	station.getAvailableBikeStands();
		if(bikes == 0 && stands == 0){
			return getMarkerRedMid();
		}else if (bikes <= 3 || stands <= 3){
			return getMarkerOrangeMid();
		}else{
			return getMarkerGreenMid();
		}
	}
	public BitmapDescriptor getBigMarkerBitmapDescriptor(Station station) {								            
    	if(station.getStatus() == Status.OPEN){
    		return getOpenMarkerBitmapDescriptor(station.getAvailableBikes(), station.getAvailableBikeStands());
    	}else{
		    return getMarkerRedBitmapDescriptor();
    	}
	}
    
	
}
