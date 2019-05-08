package tw0reck1.sonarapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import tw0reck1.sonar.PlainSonarView;
import tw0reck1.sonar.SonarPoint;
import tw0reck1.sonar.SonarView;

/** @author Adrian Tworkowski */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<PlainSonarView> plainSonarsList = Arrays.asList(
                (PlainSonarView) findViewById(R.id.plainsonarview1),
                (PlainSonarView) findViewById(R.id.plainsonarview2)
        );

        for (PlainSonarView sonar : plainSonarsList) {
            sonar.setPoints(getRandomSonarPoints());
        }

        List<SonarView> sonarsList = Arrays.asList(
                (SonarView) findViewById(R.id.sonarview1),
                (SonarView) findViewById(R.id.sonarview2),
                (SonarView) findViewById(R.id.sonarview3),
                (SonarView) findViewById(R.id.sonarview4)
        );

        for (SonarView sonar : sonarsList) {
            sonar.setPoints(getRandomSonarPoints());
        }
    }

    protected List<SonarPoint> getRandomSonarPoints() {
        Random random = new Random();
        int count = random.nextInt(12) + 6;

        List<SonarPoint> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new SonarPoint(random.nextInt(360), random.nextFloat()));
        }

        return result;
    }

}