package services;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import configuration.gui.Settings;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import toolWindow.gui.MetricsToolWindowForm;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by giggiux on 26/05/2017.
 */
public class RadarChartSetterService {

	private static final HashMap<Integer, String> metricsOrder = new HashMap<>(7);
	private static final HashMap<String, Boolean> metricsBoolean = new HashMap<>(7);
	private Project myProject;
	private MetricsToolWindowForm form;


	public RadarChartSetterService(@NotNull Project myProject) {
		this.myProject = myProject;

		metricsOrder.put(0, "C3");
		metricsOrder.put(1, "CBO");
		metricsOrder.put(2, "CD");
		metricsOrder.put(3, "LCOM");
		metricsOrder.put(4, "WMC");
		metricsOrder.put(5, "CCBC");
		metricsOrder.put(6, "CR");

		ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Compute Metrics") {
			public void run(@NotNull ProgressIndicator pi) {
				ServiceManager.getService(myProject, MetricsHandlerService.class).startExecution();
			}
		});
	}

	private DefaultCategoryDataset getDefaultDataset() {


		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		metricsBoolean.forEach((String key, Boolean bool) -> {
			if (bool) {
				dataset.setValue(100, "100", key);
				dataset.setValue(80, "80", key);
				dataset.setValue(60, "60", key);
				dataset.setValue(40, "40", key);
				dataset.setValue(20, "20", key);
			}
		});


		return dataset;
	}

	private SpiderWebPlot createSpiderWebPlot(DefaultCategoryDataset dataset) {

		final JBColor red = new JBColor(new Color(255, 0, 0), new Color(255, 25, 25));
		final JBColor lightGreen = new JBColor(new Color(0, 178, 0), new Color(0, 255, 72));
		final JBColor darkGreen = new JBColor(new Color(0, 204, 0), new Color(64, 212, 64));
		final JBColor orange = new JBColor(new Color(204, 86, 6), new Color(212, 104, 32));
		final JBColor yellow = new JBColor(new Color(202, 202, 0), new Color(252, 255, 25));
		final JBColor blue = new JBColor(Color.blue, Color.blue);


		SpiderWebPlot plot = new SpiderWebPlot(dataset);
		plot.setMaxValue(100);
		plot.setWebFilled(true);

		plot.setSeriesOutlinePaint(4, red);
		plot.setSeriesPaint(4, red);

		plot.setSeriesOutlinePaint(3, orange);
		plot.setSeriesPaint(3, orange);

		plot.setSeriesOutlinePaint(2, yellow);
		plot.setSeriesPaint(2, yellow);

		plot.setSeriesOutlinePaint(1, darkGreen);
		plot.setSeriesPaint(1, darkGreen);

		plot.setSeriesOutlinePaint(0, lightGreen);
		plot.setSeriesPaint(0, lightGreen);

		plot.setSeriesOutlinePaint(5, blue);
		plot.setSeriesPaint(5, blue);

		plot.setSeriesOutlineStroke(5, new BasicStroke(2));

		plot.setAxisLinePaint(JBColor.BLACK);
		plot.setLabelPaint(JBColor.BLACK);

		plot.setBackgroundAlpha(0);


		return plot;
	}

	private JFreeChart createJFreeChart(DefaultCategoryDataset dataset, HashMap<String, Double> metrics) {

		SpiderWebPlot plot = createSpiderWebPlot(dataset);

		plot.setToolTipGenerator((CategoryDataset categoryDataset, int i, int i1) -> {
			String toolTip;
			if (i == 5) {
				try {
					toolTip = "percentile value: " + categoryDataset.getValue(i, i1) + ", metric value: " + metrics.get(metricsOrder.get(i1));
				} catch (Exception e) {
					toolTip = "";
				}
			} else
				toolTip = categoryDataset.getValue(i, i1).toString();
			return toolTip;
		});

		JFreeChart chart = new JFreeChart(plot);

		chart.removeLegend();

		return chart;
	}

	private DefaultCategoryDataset setPercentileValuesToDataset(DefaultCategoryDataset dataset, Double[] metrics) {

		metricsOrder.forEach((Integer i, String metricsLabel) -> {
			boolean boolMetrics = metricsBoolean.get(metricsLabel);
			if (boolMetrics) {
				dataset.setValue(metrics[i], "1", metricsLabel);
			}
		});


		return dataset;
	}

	public void setChartPlots() {

		HTTPPostRequestService httpService = ServiceManager.getService(myProject, HTTPPostRequestService.class);
		MetricsHandlerService metricsHandlerService = ServiceManager.getService(myProject, MetricsHandlerService.class);

		PropertiesComponent component = PropertiesComponent.getInstance(myProject);

		boolean CBCBool = component.getBoolean(Settings.CBOSettingLabel, true);
		boolean C3Bool = component.getBoolean(Settings.C3SettingLabel, true);
		boolean LCOMBool = component.getBoolean(Settings.LCOMSettingLabel, true);
		boolean CCBCBool = component.getBoolean(Settings.CCBCSettingLabel, true);
		boolean CRBool = component.getBoolean(Settings.CRSettingLabel, true);
		boolean WMCBool = component.getBoolean(Settings.WMCSettingLabel, true);
		boolean CDBool = component.getBoolean(Settings.CDSettingLabel, true);

		metricsBoolean.put("CBO", CBCBool);
		metricsBoolean.put("LCOM", LCOMBool);
		metricsBoolean.put("CCBC", CCBCBool);
		metricsBoolean.put("C3", C3Bool);
		metricsBoolean.put("CR", CRBool);
		metricsBoolean.put("WMC", WMCBool);
		metricsBoolean.put("CD", CDBool);

		DefaultCategoryDataset dataset = getDefaultDataset();

		Map<String, Double> percentileValues = httpService.getProjectPercentileValues();

		if (!percentileValues.isEmpty()) {
			Double[] orderedMetrics = metricsMapToArray(percentileValues);
			dataset = setPercentileValuesToDataset(dataset, orderedMetrics);
		}

		HashMap<String, Double> actualMetrics = metricsHandlerService.getProjectMetrics();

		form.setProjectOverviewJFreeChart(createJFreeChart(dataset, actualMetrics));

		dataset = getDefaultDataset();

		percentileValues = httpService.getFilePercentileValues();

		if (!percentileValues.isEmpty()) {
			Double[] orderedMetrics = metricsMapToArray(percentileValues);
			dataset = setPercentileValuesToDataset(dataset, orderedMetrics);
		}

		actualMetrics = metricsHandlerService.getLatestFileMetrics();

		form.setFileOverviewJFreeChart(createJFreeChart(dataset, actualMetrics));
	}

	private Double[] metricsMapToArray(Map<String, Double> percentileValues) {
		Double[] metrics = new Double[percentileValues.size()];

		metricsOrder.forEach((Integer i, String metricsLabel) -> metrics[i] = percentileValues.get(metricsLabel));

		return metrics;
	}

	public void updateCharts() {
		HTTPPostRequestService service = ServiceManager.getService(myProject, HTTPPostRequestService.class);
		service.sendRequestForPercentileValues();
	}

	public Boolean isFormSet() {
		return form != null;
	}

	public void setForm(MetricsToolWindowForm form) {
		this.form = form;
	}

}