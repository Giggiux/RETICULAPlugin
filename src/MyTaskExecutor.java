import com.github.mauricioaniche.ck.CKNumber;
import com.github.mauricioaniche.ck.CKReport;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by giggiux on 05/05/2017.
 */
public class MyTaskExecutor {


	private static MyTaskExecutor ourInstance = new MyTaskExecutor();

	private ScheduledExecutorService executorService;

	private Project myProject;

	private Collection<CKNumber> metrics = new ArrayList<>();


	private MyTaskExecutor() {
		executorService = Executors.newScheduledThreadPool(1);
	}

	public static MyTaskExecutor getInstance() {
		return ourInstance;
	}

	public void setMyProject(Project myProject) {
		this.myProject = myProject;
	}

	public Collection<CKNumber> getMetrics() {
		return metrics;
	}

	public void startExecution() {

		if (executorService.isShutdown()) {
			executorService = Executors.newScheduledThreadPool(1);
		}

		String path = myProject.getBaseDir().toString().replace("file://", "");
		PropertiesComponent component = PropertiesComponent.getInstance(myProject);
		int seconds = component.getInt(Settings.SecSettingLabel, 300);


		Runnable taskWrapper = () -> {

			ReducedCK myCK = new ReducedCK();
			CKReport report = myCK.calculate(path);
			metrics = report.all();

			ApplicationManager.getApplication().invokeLater(() -> {
				RadarChartSetter.getInstance().updateCharts();
			});
		};

		long delay = computeDelay(seconds);
		executorService.scheduleAtFixedRate(taskWrapper, 0, delay, TimeUnit.SECONDS);
	}

	private long computeDelay(int targetSec) {
		LocalDateTime localNow = LocalDateTime.now();
		ZoneId currentZone = ZoneId.systemDefault();
		ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
		ZonedDateTime zonedNextTarget = zonedNow.plusSeconds(targetSec);
		if (zonedNow.compareTo(zonedNextTarget) > 0)
			zonedNextTarget = zonedNextTarget.plusDays(1);

		Duration duration = Duration.between(zonedNow, zonedNextTarget);
		return duration.getSeconds();
	}

	public void stop() {
		executorService.shutdownNow();
	}
}
