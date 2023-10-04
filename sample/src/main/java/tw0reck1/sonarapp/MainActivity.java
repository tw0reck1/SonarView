package tw0reck1.sonarapp;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import tw0reck1.sonar.Sonar;
import tw0reck1.sonar.SonarPoint;

/** @author Adrian Tworkowski */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<Sonar> sonarsList = Arrays.asList(
            findViewById(R.id.plainsonarview1),
            findViewById(R.id.plainsonarview2),
            findViewById(R.id.sonarview1),
            findViewById(R.id.sonarview2),
            findViewById(R.id.sonarview3),
            findViewById(R.id.sonarview4)
        );

        for (Sonar sonar : sonarsList) {
            sonar.setPoints(getRandomSonarPoints());
            sonar.startAnimation();
        }
    }

    protected List<SonarPoint> getRandomSonarPoints() {
        Random random = new Random();
        int count = random.nextInt(10) + 2;

        List<SonarPoint> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new SonarPoint(random.nextInt(360), random.nextFloat()));
        }

        return result;
    }
}
