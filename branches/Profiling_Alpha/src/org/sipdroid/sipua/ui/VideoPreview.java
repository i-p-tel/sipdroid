package org.sipdroid.sipua.ui;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

class VideoPreview extends SurfaceView {
    private float mAspectRatio;
    private int mHorizontalTileSize = 1;
    private int mVerticalTileSize = 1;

    /**
     * Setting the aspect ratio to this value means to not enforce an aspect ratio.
     */
    public static float DONT_CARE = 0.0f;

    public VideoPreview(Context context) {
        super(context);
    }

    public VideoPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTileSize(int horizontalTileSize, int verticalTileSize) {
        if ((mHorizontalTileSize != horizontalTileSize)
                || (mVerticalTileSize != verticalTileSize)) {
            mHorizontalTileSize = horizontalTileSize;
            mVerticalTileSize = verticalTileSize;
            requestLayout();
            invalidate();
        }
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio(((float) width) / ((float) height));
    }

    public void setAspectRatio(float aspectRatio) {
        if (mAspectRatio != aspectRatio) {
            mAspectRatio = aspectRatio;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspectRatio != DONT_CARE) {
            int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

            int width = widthSpecSize;
            int height = heightSpecSize;

            if (width > 0 && height > 0) {
                float defaultRatio = ((float) width) / ((float) height);
                if (defaultRatio < mAspectRatio) {
                    // Need to reduce height
                    height = (int) (width / mAspectRatio);
                } else if (defaultRatio > mAspectRatio) {
                    width = (int) (height * mAspectRatio);
                }
                width = roundUpToTile(width, mHorizontalTileSize, widthSpecSize);
                height = roundUpToTile(height, mVerticalTileSize, heightSpecSize);
                Log.i("VideoPreview", "ar " + mAspectRatio + " setting size: " + width + 'x' + height);
                setMeasuredDimension(width, height);
                return;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int roundUpToTile(int dimension, int tileSize, int maxDimension) {
        return Math.min(((dimension + tileSize - 1) / tileSize) * tileSize, maxDimension);
    }
}