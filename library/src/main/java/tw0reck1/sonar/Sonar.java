package tw0reck1.sonar;

import java.util.Collection;

public interface Sonar {

    void setPoints(Collection<SonarPoint> points);

    void addPoints(SonarPoint...points);

    void setColor(int color);

    void setStrokeWidth(float strokeWidth);

    void setThinStrokeWidth(float thinStrokeWidth);

    void setStrokeWidths(float strokeWidth, float thinStrokeWidth);

    void setPointSize(float pointSize);

}