package services;


import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBus;
import it.frunzioluigi.metricsCalculator.ReducedCK;
import com.github.mauricioaniche.ck.CKNumber;
import com.github.mauricioaniche.ck.CKReport;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;

import com.intellij.openapi.project.Project;
import configuration.gui.Settings;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by giggiux on 26/05/2017.
 */
public class MetricsHandlerService {
	private Project myProject;

	private ScheduledExecutorService executorService;

	private Collection<CKNumber> metrics = new ArrayList<>();

	private String currentFilePath;
	private int fileOriginalWordCount;

	private HashMap<String, Double> latestFileMetrics;

	public MetricsHandlerService(Project myProject) {
		this.myProject = myProject;

		executorService = Executors.newScheduledThreadPool(1);

		this.setFileChangesListener();
	}


	void startExecution() {

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
				RadarChartSetterService RadarChartSetter = ServiceManager.getService(myProject, RadarChartSetterService.class);
				RadarChartSetter.updateCharts();
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

	private void stop() {
		executorService.shutdownNow();
	}

	private void restart() {
		this.stop();
		this.startExecution();
	}


	HashMap<String, Double> getProjectMetrics() {

		Collection<CKNumber> allReport = metrics;

		HashMap<String, Double> projectMetrics = new HashMap<>();

		double c3 = 0, cbo = 0, lcom = 0, ccbc = 0, cr = 0, loc = 0, wmc = 0, cd = 0;

		for (CKNumber result : allReport) {

			c3 += ((double) result.getSpecific("C3")) / 1000;
			cbo += result.getCbo();
			lcom += result.getLcom();
			ccbc += ((double) result.getSpecific("CCBC")) / 1000;
			cr += ((double) result.getSpecific("CR")) / 1000;
			loc += result.getLoc();
			wmc += result.getWmc();
			cd += ((double) result.getSpecific("CD")) / 1000;

		}

		int size = allReport.size();

		if (size != 0) {
			projectMetrics.put("C3", c3 / size);
			projectMetrics.put("CBO", cbo / size);
			projectMetrics.put("LCOM", lcom / size);
			projectMetrics.put("CCBC", ccbc / size);
			projectMetrics.put("CR", cr / size);
			projectMetrics.put("LOC", loc);
			projectMetrics.put("WMC", wmc / size);
			projectMetrics.put("CD", cd / size);
		}

		return projectMetrics;
	}

	HashMap<String, Double> getFileMetrics(String file) {
		Collection<CKNumber> allReport = metrics;

		HashMap<String, Double> fileMetrics = new HashMap<>();

		for (CKNumber result : allReport) {
			if (result.getFile().equals(file)) {
				fileMetrics.put("C3", ((double) result.getSpecific("C3")) / 1000);
				fileMetrics.put("CBO", (double) result.getCbo());
				fileMetrics.put("LCOM", (double) result.getLcom());
				fileMetrics.put("CCBC", ((double) result.getSpecific("CCBC")) / 1000);
				fileMetrics.put("CR", ((double) result.getSpecific("CR")) / 1000);
				fileMetrics.put("LOC", (double) result.getLoc());
				fileMetrics.put("WMC", (double) result.getWmc());
				fileMetrics.put("CD", ((double) result.getSpecific("CD")) / 1000);
			}
		}

		latestFileMetrics = fileMetrics;
		return fileMetrics;
	}

	HashMap<String, Double> getLatestFileMetrics() {
		return latestFileMetrics;
	}

	private int countWordsInFile(VirtualFile file) {
		int count = 0;
		try {
			String fileContentToString = new String(file.contentsToByteArray());
			count = fileContentToString.trim().split("\\s+").length;
		} catch (Exception e) {
		}
		return count;
	}

	private void setFileChangesListener() {
		VirtualFileManager vfm = VirtualFileManager.getInstance();
		vfm.addVirtualFileListener(new VirtualFileListener() {
			@Override
			public void contentsChanged(@NotNull VirtualFileEvent event) {

				VirtualFile eventFile = event.getFile();

				String eventFileExtention = eventFile.getExtension();


				if (currentFilePath == null && eventFileExtention != null && eventFileExtention.equals("java")) {
					currentFilePath = eventFile.getPath();
					fileOriginalWordCount = countWordsInFile(eventFile);

				} else if (currentFilePath != null && eventFile.getPath().equals(currentFilePath)) {
					int currentWordCount = countWordsInFile(eventFile);

					PropertiesComponent component = PropertiesComponent.getInstance(myProject);
					int wordsToBeDifferent = component.getInt(Settings.WordSettingLabel, 300);

					if (Math.abs(currentWordCount - fileOriginalWordCount) >= wordsToBeDifferent) {
						MetricsHandlerService metricsHandlerService = ServiceManager.getService(myProject, MetricsHandlerService.class);
						metricsHandlerService.restart();

						fileOriginalWordCount = currentWordCount;
					}
				}
			}
		});


		MessageBus messageBus = myProject.getMessageBus();
		messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			@Override
			public void selectionChanged(@NotNull FileEditorManagerEvent event) {
				VirtualFile eventNewFile = event.getNewFile();

				if (eventNewFile != null && eventNewFile.getExtension() != null && eventNewFile.getExtension().equals("java")) {
					currentFilePath = eventNewFile.getPath();

					fileOriginalWordCount = countWordsInFile(eventNewFile);
				}

				HTTPPostRequestService httpPostRequestService = com.intellij.openapi.components.ServiceManager.getService(myProject, HTTPPostRequestService.class);
				httpPostRequestService.fileIsChanged();
			}
		});


	}
}
