package tw0reck1.sonar

interface Sonar {
    fun setPoints(points: Iterable<SonarPoint>)
    fun addPoints(vararg points: SonarPoint)
    fun setColor(color: Int)
    fun setStrokeWidth(strokeWidth: Float)
    fun setThinStrokeWidth(thinStrokeWidth: Float)
    fun setFontSize(fontSize: Float)
    fun setThinFontSize(thinFontSize: Float)
    fun setPointSize(pointSize: Float)
    fun setSizes(
        strokeWidth: Float,
        thinStrokeWidth: Float,
        fontSize: Float,
        thinFontSize: Float,
        pointSize: Float
    )
    fun startAnimation()
    fun stopAnimation()
}
