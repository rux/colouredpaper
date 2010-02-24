/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lastminute.labs.colouredpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class ColouredPaper extends WallpaperService {

    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new ColouredPaperEngine();
    }

    class ColouredPaperEngine extends Engine  implements SensorEventListener {

    	private int SMOOTHING = 10;
    	private float ANGLESMOOTHING = 7.5f;
        private final Paint mPaint = new Paint();
        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        // private long mStartTime;
        private float mCenterX;
        private float mCenterY;
        
        private SensorManager mSensorManager;
        
        private int red;
        private int green;
        private int blue;
        
        private float orientation;
        
        private int sensorRed;
        private int sensorGreen;
        private int sensorBlue;
        
        private float sensorOrientation;
        
       
        private float xa;
        private float ya;
        private float za;
        
        

        private final Runnable mDrawBackground = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;

        ColouredPaperEngine() {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawBackground);
            mSensorManager.unregisterListener(this);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawBackground);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // store the center of the surface, so we can draw the cube in the right spot
            mCenterX = width/2.0f;
            mCenterY = height/2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawBackground);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            mOffset = xOffset;
            drawFrame();
        }
        
        
        
        

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mTouchX = event.getX();
                mTouchY = event.getY();
            } else {
                mTouchX = -1;
                mTouchY = -1;
            }
            super.onTouchEvent(event);
        }

        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here. This example draws a wireframe cube.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
                	drawBackground(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawBackground);
            if (mVisible) {
                mHandler.postDelayed(mDrawBackground, 1000 / 30);
            }
        }

        
        float getAngleDifference(float from, float to) {
            // Get the two distances in size order
        	float d1 = (from>to)?to:from;
        	float d2 = (from<to)?to:from;
            
            // Get the two distances
        	float distA = d2-d1;
        	float distB = d1 + 360-d2;
            
            // Get the shortest distance
        	float minDistance = Math.min(distA, distB);
            
            // Which way are we going?
            boolean direction=distA>distB;
            if (from>to) direction=!direction;
            
            return (direction?-1:1) * minDistance;
        }
        
        
        
        void drawBackground(Canvas c) {
        	
            
            
        	red = (SMOOTHING*red + sensorRed) / (SMOOTHING+1);
        	green = (SMOOTHING*green + sensorGreen) / (SMOOTHING+1);
        	blue = (SMOOTHING*blue + sensorBlue) / (SMOOTHING+1);
        	
        	float angleDiff = getAngleDifference(orientation, sensorOrientation);
        	
        	orientation = (orientation + angleDiff/ANGLESMOOTHING);
        	
        	// Log.d("angle diff", String.valueOf(angleDiff));
        	
        	float hsv[] = new float[3];
        	
        	Color.RGBToHSV(red, green, blue, hsv);
        	
        	float hueMod = (orientation + hsv[0]) % 360;
        	
        	float vals[] = {hueMod, hsv[1], hsv[2]};
        	c.drawColor(Color.HSVToColor(vals));
        	
        	// c.drawColor(col.rgb(red , green, blue));
        	
        }
        
        
        
        
        
     

        /*
         * Draw a circle around the current touch point, if any.
         */
        void drawTouchPoint(Canvas c) {
            if (mTouchX >=0 && mTouchY >= 0) {
                c.drawCircle(mTouchX, mTouchY, 80, mPaint);
            }
        }

        
        
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		public void onSensorChanged(SensorEvent event) {
			Sensor mySensor = event.sensor;
			
			if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			
				xa = event.values[0];
				ya = event.values[1];
				za = event.values[2];
	
				sensorRed   = (int)  Math.min( 255, Math.abs(-xa*255/10.0) ) ;
				sensorGreen = (int)  Math.min( 255, Math.abs(-ya*255/10.0) ) ;
				sensorBlue  = (int)  Math.min( 255, Math.abs(-za*255/10.0) ) ;
			}
			
			else if (mySensor.getType() == Sensor.TYPE_ORIENTATION) {
				sensorOrientation = event.values[0];
			}
		}

    }
}
