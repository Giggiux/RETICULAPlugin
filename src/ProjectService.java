import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Created by giggiux on 04/05/2017.
 */

/**
 * TODO: if tot words are changed stop MyTaskExecutor and restart it recomputing metrics
 **/

// TODO: Create service that computes differences in file (???)

// TODO: Give filters a functionality: save the "true"/"false" in properties and if they change -> HTTPPostRequestService.sendRequestForPercentileValues();



public class ProjectService {


	private final Project myProject;
	private final MyTaskExecutor myTaskExecutor;
	private final RadarChartSetter myRadarChartSetter;
	private MetricsToolWindowForm form;

	public ProjectService(@NotNull Project myProject) {
		this.myProject = myProject;
		this.myTaskExecutor = MyTaskExecutor.getInstance();

		this.myRadarChartSetter = RadarChartSetter.getInstance();

		myTaskExecutor.setMyProject(myProject);
		myRadarChartSetter.setMyProject(myProject);

		ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Compute Metrics") {
			public void run(@NotNull ProgressIndicator pi) {
				myTaskExecutor.startExecution();
			}
		});

	}

	public Project getMyProject() {
		return myProject;
	}

	public MetricsToolWindowForm getForm() {
		return form;
	}

	public void setForm(MetricsToolWindowForm form) {
		this.form = form;
	}

	public Boolean isFormSet() {
		return form != null;
	}

}
