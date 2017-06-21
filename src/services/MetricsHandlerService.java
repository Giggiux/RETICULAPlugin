package services;


import com.github.mauricioaniche.ck.CKNumber;
import com.github.mauricioaniche.ck.CKReport;
import com.intellij.concurrency.JobScheduler;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.messages.MessageBus;
import configuration.gui.Settings;
import it.frunzioluigi.metricsCalculator.ReducedCK;
import it.frunzioluigi.metricsCalculator.metrics.CCBC;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by giggiux on 26/05/2017.
 */
public class MetricsHandlerService {
	private Project myProject;
	private ScheduledExecutorService executorService;
	private HashMap<String, Double> latestFileMetrics;
	private HashMap<String, Integer> editedFilesWordsCount;
	private HashMap<String, CKNumber> report = new HashMap<>();
	private ReducedCK CKMetricsCalculator;
	private boolean firstMetricsComputed = false;

	public MetricsHandlerService(Project myProject) {
		this.myProject = myProject;

		executorService = JobScheduler.getScheduler();

		this.setFileChangesListener();

		CKMetricsCalculator = new ReducedCK();

		ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Compute metrics") {
			@Override
			public void run(@NotNull ProgressIndicator progressIndicator) {
				progressIndicator.start();
				progressIndicator.setFraction(0.);

				Collection<VirtualFile> javaVirtualFiles = FileBasedIndex.getInstance().getContainingFiles(ID.create("filetypes"),JavaFileType.INSTANCE, GlobalSearchScope.projectScope(myProject));
				ArrayList<String> javaFilesArray = new ArrayList<>();

				javaVirtualFiles.forEach((VirtualFile vf) -> {
					javaFilesArray.add(vf.getPath().replace("file://", ""));
				});

				String[] javaFiles = new String[javaFilesArray.size()];
				javaFiles = javaFilesArray.toArray(javaFiles);

				computeMetricsFromFileList(javaFiles, progressIndicator);

				ApplicationManager.getApplication().invokeLater(() -> {
					RadarChartSetterService RadarChartSetter = ServiceManager.getService(myProject, RadarChartSetterService.class);
					RadarChartSetter.updateCharts();
				});
				progressIndicator.stop();
				firstMetricsComputed = true;
			}
		});
	}


	private HashMap<String, CKNumber> getFileReport(String path) {
		HashMap<String, CKNumber> report = new HashMap<>();

		CKReport reportSingleFile = CKMetricsCalculator.calculate(path);
		Collection<CKNumber> CKArray = reportSingleFile.all();
		for (CKNumber rep : CKArray) {
			report.put(path, rep);
		}
		return report;
	}


	private void computeMetricsFromFileList(String[] javaFiles, ProgressIndicator pi) {
		int size = javaFiles.length;
		for (int i = 0; i<size; i++) {
			report.putAll(getFileReport(javaFiles[i]));
			if (pi != null) pi.setFraction((double) i / (size * 2));
		}

		Collection<CKNumber> allReport = report.values();
		new CCBC().calculate(allReport);
	}


	void startExecution() {

		if (executorService.isShutdown()) {
			executorService = JobScheduler.getScheduler();
		}

		PropertiesComponent component = PropertiesComponent.getInstance(myProject);
		int seconds = component.getInt(Settings.SecSettingLabel, 300);

		Runnable taskWrapper = () -> {
			if (firstMetricsComputed) {
				Set<String> javaFilesSet = editedFilesWordsCount.keySet();
				String[] javaFiles = new String[javaFilesSet.size()];
				javaFiles = javaFilesSet.toArray(javaFiles);

				computeMetricsFromFileList(javaFiles, null);

				editedFilesWordsCount = new HashMap<>();

				HTTPPostRequestService httpPostRequestService = com.intellij.openapi.components.ServiceManager.getService(myProject, HTTPPostRequestService.class);
				httpPostRequestService.fileIsChanged();
			}
		};

		long delay = computeDelay(seconds);
		executorService.scheduleWithFixedDelay(taskWrapper, 0, delay, TimeUnit.SECONDS);
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

		Collection<CKNumber> allReport = report.values();

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
		Collection<CKNumber> allReport = report.values();

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

				String filePath = eventFile.getPath().replace("file://", "");

				if (eventFileExtention != null && eventFileExtention.equals("java")) {
					if (editedFilesWordsCount.containsKey(filePath)) {
						int currentWordCount = countWordsInFile(eventFile);
						int originalWorkCount = editedFilesWordsCount.get(filePath);

						PropertiesComponent component = PropertiesComponent.getInstance(myProject);
						int wordsToBeDifferent = component.getInt(Settings.WordSettingLabel, 300);

						if (Math.abs(currentWordCount - originalWorkCount) >= wordsToBeDifferent) {
//							MetricsHandlerService metricsHandlerService = ServiceManager.getService(myProject, MetricsHandlerService.class);
//							metricsHandlerService.restart();
							restart();
						}
					} else {
						editedFilesWordsCount.put(filePath, countWordsInFile(eventFile));
					}
				}
			}
		});


		MessageBus messageBus = myProject.getMessageBus();
		messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			@Override
			public void selectionChanged(@NotNull FileEditorManagerEvent event) {
//				VirtualFile eventNewFile = event.getNewFile();
//
//				if (eventNewFile != null && eventNewFile.getExtension() != null && eventNewFile.getExtension().equals("java")) {
//					currentFilePath = eventNewFile.getPath();
//				}

				HTTPPostRequestService httpPostRequestService = com.intellij.openapi.components.ServiceManager.getService(myProject, HTTPPostRequestService.class);
				httpPostRequestService.fileIsChanged();
			}
		});


	}
}
