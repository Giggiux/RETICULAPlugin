import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;

/**
 * Created by giggiux on 04/05/2017.
 */

/** TODO: Create background process (http://stackoverflow.com/questions/20387881/how-to-run-certain-task-every-day-at-a-particular-time-using-scheduledexecutorse/20388073#20388073) to compute metrics
 *  every tot seconds or, if tot words are changed reset scheduledExecutor task and compute metrics then restart seconds task
 *  **/

// TODO: Create service that computes differences in file (???)

// TODO: Give filters a functionality.

// TODO: After metrics are computed send them to the WS -> (compute percentile, for every single metrics both for current file and project) -> send received data to ChartPlot

public class ProjectService {
	private final Project myProject;

	public ProjectService(@NotNull Project myProject) {
		this.myProject = myProject;
	}

	public Project getMyProject() {
		return myProject;
	}

	private Boolean CBCBool;
	private Boolean C3Bool;
	private Boolean LCOMBool;
	private Boolean CCBCBool;
	private Boolean CRBool;
	private Boolean LOCBool;
	private Boolean WMCBool;
	private Boolean CDBool;

	private MetricsToolWindowFactory factory;

	private MetricsToolWindowForm form;

	public void setForm(MetricsToolWindowForm form) {
		this.form = form;
	}

	public void setFactory(MetricsToolWindowFactory factory) {
		this.factory = factory;
	}

	private DefaultCategoryDataset getDefaultDataset() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		if (CBCBool) {
			dataset.setValue(33, "33", "CBC");
			dataset.setValue(66, "66", "CBC");
			dataset.setValue(100, "100", "CBC");
		}
		if (LCOMBool) {
			dataset.setValue(33, "33", "LCOM");
			dataset.setValue(66, "66", "LCOM");
			dataset.setValue(100, "100", "LCOM");
		}
		if (CCBCBool) {
			dataset.setValue(33, "33", "CCBC");
			dataset.setValue(66, "66", "CCBC");
			dataset.setValue(100, "100", "CCBC");
		}
		if (C3Bool) {
			dataset.setValue(33, "33", "C3");
			dataset.setValue(66, "66", "C3");
			dataset.setValue(100, "100", "C3");
		}
		if (CRBool) {
			dataset.setValue(33, "33", "CR");
			dataset.setValue(66, "66", "CR");
			dataset.setValue(100, "100", "CR");
		}
		if (LOCBool) {
			dataset.setValue(33, "33", "LOC");
			dataset.setValue(66, "66", "LOC");
			dataset.setValue(100, "100", "LOC");
		}
		if (WMCBool) {
			dataset.setValue(33, "33", "WMC");
			dataset.setValue(66, "66", "WMC");
			dataset.setValue(100, "100", "WMC");
		}
		if (CDBool) {
			dataset.setValue(33, "33", "CD");
			dataset.setValue(66, "66", "CD");
			dataset.setValue(100, "100", "CD");
		}

		return dataset;
	}

	private SpiderWebPlot createSpiderWebPlot(DefaultCategoryDataset dataset) {
		SpiderWebPlot plot = new SpiderWebPlot(dataset);
		plot.setMaxValue(100);
		plot.setWebFilled(true);

		plot.setSeriesOutlinePaint(0, JBColor.red);
		plot.setSeriesPaint(0, JBColor.red);

		plot.setSeriesOutlinePaint(1, JBColor.yellow);
		plot.setSeriesPaint(1, JBColor.yellow);

		plot.setSeriesOutlinePaint(2, JBColor.green);
		plot.setSeriesPaint(2, JBColor.green);

		plot.setSeriesOutlinePaint(3, JBColor.blue);
		plot.setSeriesPaint(3, JBColor.blue);

		plot.setSeriesOutlineStroke(3, new BasicStroke(2));

		return plot;
	}

	private JFreeChart createJFreeChart(DefaultCategoryDataset dataset) {

		SpiderWebPlot plot = createSpiderWebPlot(dataset);

		JFreeChart chart = new JFreeChart(plot);

		chart.removeLegend();

		return chart;
	}

	private ChartPanel createChartPanel(JFreeChart chart) {
		return new ChartPanel(chart);
	}
	private DefaultCategoryDataset setPercentileValuesToDataset(DefaultCategoryDataset dataset,
	                                                            Double CBC, Double LCOM, Double CCBC, Double C3, Double CR, Double LOC, Double WMC, Double CD) {
		if (CBCBool) {
			dataset.setValue(CBC, "1", "CBC");
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

		// TODO: Merge these two methods (setChartPLots and SetEmptyPlots) so that if there are no metrics computed it displays chart without data (or display all zeros)

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

		dataset = setPercentileValuesToDataset(dataset, 77., 12., 55., 74., 100., 9., 33.3, 3.);


		form.setFileOverviewJFreeChart(createJFreeChart(dataset));

		dataset = getDefaultDataset();

		dataset = setPercentileValuesToDataset(dataset, 11., 56., 100., 13., 0., 45., 66.6, 30.);

		form.setProjectOverviewJFreeChart(createJFreeChart(dataset));

	}

	public void setEmptyPlots() {
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

		JFreeChart chart = createJFreeChart(dataset);

		form.setFileOverviewChart(createChartPanel(chart));
		form.setProjectOverviewChart(createChartPanel(chart));

	}

	public Boolean isFormSet(){
		return form != null;
	}

}
