package tw0reck1.sonarapp

import android.app.Activity
import android.os.Bundle
import tw0reck1.sonar.Sonar
import tw0reck1.sonar.SonarPoint
import kotlin.random.Random

/** @author Adrian Tworkowski
 */
class MainActivity : Activity() {

    private val randomSonarPoints: List<SonarPoint>
        get() = (1..Random.nextInt(2, 10))
            .map {
                SonarPoint(Random.nextInt(360), Random.nextFloat())
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listOf<Sonar>(
            findViewById(R.id.plainsonarview1),
            findViewById(R.id.plainsonarview2),
            findViewById(R.id.sonarview1),
            findViewById(R.id.sonarview2),
            findViewById(R.id.sonarview3),
            findViewById(R.id.sonarview4)
        )
            .forEach { sonar ->
                sonar.setPoints(randomSonarPoints)
                sonar.startAnimation()
            }
    }
}
