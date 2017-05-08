import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.JBColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by giggiux on 05/05/2017.
 */
public class RadarChartSetter {
	private static RadarChartSetter ourInstance = new RadarChartSetter();
	private Boolean CBCBool;
	private Boolean C3Bool;
	private Boolean LCOMBool;
	private Boolean CCBCBool;
	private Boolean CRBool;
	private Boolean LOCBool;
	private Boolean WMCBool;
	private Boolean CDBool;
	private Project myProject;

	private RadarChartSetter() {
	}

	public static RadarChartSetter getInstance() {
		return ourInstance;
	}

	public void setMyProject(Project myProject) {
		this.myProject = myProject;
	}

	private DefaultCategoryDataset getDefaultDataset() {

		final HashMap<String, Boolean> keys = new HashMap<>();

		keys.put("CBO", CBCBool);
		keys.put("LCOM", LCOMBool);
		keys.put("CCBC", CCBCBool);
		keys.put("C3", C3Bool);
		keys.put("CR", CRBool);
		keys.put("LOC", LOCBool);
		keys.put("WMC", WMCBool);
		keys.put("CD", CDBool);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		keys.forEach((String key, Boolean bool) -> {
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

		plot.setSeriesOutlinePaint(5, JBColor.blue);
		plot.setSeriesPaint(5, JBColor.blue);

		plot.setSeriesOutlineStroke(5, new BasicStroke(2));

		plot.setAxisLinePaint(JBColor.BLACK);
		plot.setLabelPaint(JBColor.BLACK);

		plot.setBackgroundAlpha(0);

		return plot;
	}

	private JFreeChart createJFreeChart(DefaultCategoryDataset dataset) {

		SpiderWebPlot plot = createSpiderWebPlot(dataset);

		JFreeChart chart = new JFreeChart(plot);

		chart.removeLegend();

		return chart;
	}

	private DefaultCategoryDataset setPercentileValuesToDataset(DefaultCategoryDataset dataset,
	                                                            Double CBC, Double LCOM, Double CCBC, Double C3, Double CR, Double LOC, Double WMC, Double CD) {
		if (CBCBool) {
			dataset.setValue(CBC, "1", "CBO");
		}
		if (LCOMBool) {
			dataset.setValue(LCOM, "1", "LCOM");
		}
		if (CCBCBool) {
			dataset.setValue(CCBC, "1", "CCBC");
		}
		if (C3Bool) {
			dataset.setValue(C3, "1", "C3");
		}
		if (CRBool) {
			dataset.setValue(CR, "1", "CR");
		}
		if (LOCBool) {
			dataset.setValue(LOC, "1", "LOC");
		}
		if (WMCBool) {
			dataset.setValue(WMC, "1", "WMC");
		}
		if (CDBool) {
			dataset.setValue(CD, "1", "CD");
		}

		return dataset;
	}

	public void setChartPlots() {

		ProjectService service = ServiceManager.getService(myProject, ProjectService.class);
		HTTPPostRequestService httpService = ServiceManager.getService(myProject, HTTPPostRequestService.class);

		MetricsToolWindowForm form = service.getForm();

		PropertiesComponent component = PropertiesComponent.getInstance(myProject);

		CBCBool = component.getBoolean(Settings.CBCSettingLabel, true);
		C3Bool = component.getBoolean(Settings.C3SettingLabel, true);
		LCOMBool = component.getBoolean(Settings.LCOMSettingLabel, true);
		CCBCBool = component.getBoolean(Settings.CCBCSettingLabel, true);
		CRBool = component.getBoolean(Settings.CRSettingLabel, true);
		LOCBool = component.getBoolean(Settings.LOCSettingLabel, true);
		WMCBool = component.getBoolean(Settings.WMCSettingLabel, true);
		CDBool = component.getBoolean(Settings.CDSettingLabel, true);

		DefaultCategoryDataset dataset = getDefaultDataset();

		Map<String, Double> percentileValues = httpService.getProjectPercentileValues();

		if (!percentileValues.isEmpty()) {
			dataset = setPercentileValuesToDataset(dataset, percentileValues.get("CBC"), percentileValues.get("LCOM"), percentileValues.get("CCBC"), percentileValues.get("C3"), percentileValues.get("CR"), percentileValues.get("LOC"), percentileValues.get("WMC"), percentileValues.get("CD"));
		}

		form.setProjectOverviewJFreeChart(createJFreeChart(dataset));

		dataset = getDefaultDataset();

		percentileValues = httpService.getFilePercentileValues();

		if (!percentileValues.isEmpty()) {
			dataset = setPercentileValuesToDataset(dataset, percentileValues.get("CBC"), percentileValues.get("LCOM"), percentileValues.get("CCBC"), percentileValues.get("C3"), percentileValues.get("CR"), percentileValues.get("LOC"), percentileValues.get("WMC"), percentileValues.get("CD"));
		}

		form.setFileOverviewJFreeChart(createJFreeChart(dataset));
	}

	public void updateCharts() {
		HTTPPostRequestService service = ServiceManager.getService(myProject, HTTPPostRequestService.class);
		service.sendRequestForPercentileValues();
	}

}


