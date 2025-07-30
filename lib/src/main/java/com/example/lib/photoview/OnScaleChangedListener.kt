package com.example.lib.photoview

interface OnScaleChangedListener {
    /**
     * Callback for when the scale changes
     *
     * @param scaleFactor the scale factor (less than 1 for zoom out, greater than 1 for zoom in)
     * @param focusX      focal point X position
     * @param focusY      focal point Y position
     */
    fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float)
}