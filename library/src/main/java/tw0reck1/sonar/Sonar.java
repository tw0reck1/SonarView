package tw0reck1.sonar;

import java.util.Collection;

public interface Sonar {

    void setPoints(Collection<SonarPoint> points);

    void addPoints(SonarPoint...points);

    void setColor(int color);

}