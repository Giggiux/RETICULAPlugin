import com.github.mauricioaniche.ck.metric.WMC;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by giggiux on 13/04/2017.
 */
public class Settings {
	private JPanel rootPanel;
	private JCheckBox CBCCheckBox;
	private JCheckBox c3CheckBox;
	private JCheckBox LCOMCheckBox;
	private JCheckBox CCBCCheckBox;
	private JCheckBox CRCheckBox;
	private JCheckBox LOCCheckBox;
	private JCheckBox WMCCheckBox;
	private JCheckBox CDCheckBox;
	private JPanel projectSizePanel;
	private JPanel timePanel;
	private JPanel metricsPanel;
	private JSlider seconds;
	private JSlider words;
	private JTextField teamSize;
	private JTextField projectAge;
	private Project myProject;

	// TODO: Add +-percentage to add to filters

	static final public String CBCSettingLabel = "githubMetricsCalculatorPlugin_CBC";
	static final public String C3SettingLabel = "githubMetricsCalculatorPlugin_C3";
	static final public String LCOMSettingLabel = "githubMetricsCalculatorPlugin_LCOM";
	static final public String CCBCSettingLabel = "githubMetricsCalculatorPlugin_CCBC";
	static final public String CRSettingLabel = "githubMetricsCalculatorPlugin_CR";
	static final public String LOCSettingLabel = "githubMetricsCalculatorPlugin_LOC";
	static final public String WMCSettingLabel = "githubMetricsCalculatorPlugin_WMC";
	static final public String CDSettingLabel = "githubMetricsCalculatorPlugin_CD";
	static final public String SecSettingLabel = "githubMetricsCalculatorPlugin_RecomputeMetrics_Sec";
	static final public String WordSettingLabel = "githubMetricsCalculatorPlugin_RecomputeMetrics_Word";
	static final public String SizeSettingLabel = "githubMetricsCalculatorPlugin_Filters_TeamSize";
	static final public String AgeSettingLabel = "githubMetricsCalculatorPlugin_Filters_ProjectAge";

	public Settings(Project project) {
		myProject = project;
		$$$setupUI$$$();


		// Only numbers in Text Fileds:

		((AbstractDocument) teamSize.getDocument()).setDocumentFilter(new DocumentFilter() {
			Pattern regEx = Pattern.compile("\\d+");

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
				Matcher matcher = regEx.matcher(text);
				if (!matcher.matches()) {
					return;
				}
				super.replace(fb, offset, length, text, attrs);
			}
		});

		((AbstractDocument) projectAge.getDocument()).setDocumentFilter(new DocumentFilter() {
			Pattern regEx = Pattern.compile("\\d+");

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
				Matcher matcher = regEx.matcher(text);
				if (!matcher.matches()) {
					return;
				}
				super.replace(fb, offset, length, text, attrs);
			}
		});

		// Set previous settings
		setSettings();

	}

	public JPanel getRootPanel() {
		return rootPanel;
	}


	public boolean isModified() {
		PropertiesComponent component = PropertiesComponent.getInstance(myProject);

		Boolean CBC = component.getBoolean(CBCSettingLabel, true);
		Boolean C3 = component.getBoolean(C3SettingLabel, true);
		Boolean LCOM = component.getBoolean(LCOMSettingLabel, true);
		Boolean CCBC = component.getBoolean(CCBCSettingLabel, true);
		Boolean CR = component.getBoolean(CRSettingLabel, true);
		Boolean LOC = component.getBoolean(LOCSettingLabel, true);
		Boolean WMC = component.getBoolean(WMCSettingLabel, true);
		Boolean CD = component.getBoolean(CDSettingLabel, true);

		int sec = component.getInt(SecSettingLabel, 300);
		int word = component.getInt(WordSettingLabel, 300);
		int size = component.getInt(SizeSettingLabel, 1);
		int age = component.getInt(AgeSettingLabel, 0);

		return CBC != CBCCheckBox.isSelected()
				|| C3 != c3CheckBox.isSelected()
				|| LCOM != LCOMCheckBox.isSelected()
				|| CCBC != CCBCCheckBox.isSelected()
				|| CR != CRCheckBox.isSelected()
				|| LOC != LOCCheckBox.isSelected()
				|| WMC != WMCCheckBox.isSelected()
				|| CD != CDCheckBox.isSelected()
				|| sec != seconds.getValue()
				|| word != words.getValue()
				|| size != Integer.parseInt(teamSize.getText())
				|| age != Integer.parseInt(projectAge.getText());



	}

	public void apply() throws ConfigurationException {
		if (isModified()) {
			PropertiesComponent component = PropertiesComponent.getInstance(myProject);

			component.setValue(CBCSettingLabel, CBCCheckBox.isSelected(), true);
			component.setValue(C3SettingLabel, c3CheckBox.isSelected(), true);
			component.setValue(LCOMSettingLabel, LCOMCheckBox.isSelected(), true);
			component.setValue(CCBCSettingLabel, CCBCCheckBox.isSelected(), true);
			component.setValue(CRSettingLabel, CRCheckBox.isSelected(), true);
			component.setValue(LOCSettingLabel, LOCCheckBox.isSelected(), true);
			component.setValue(WMCSettingLabel, WMCCheckBox.isSelected(), true);
			component.setValue(CDSettingLabel, CDCheckBox.isSelected(), true);

			component.setValue(SecSettingLabel, seconds.getValue(), 300);
			component.setValue(WordSettingLabel, words.getValue(), 300);
			component.setValue(SizeSettingLabel, Integer.parseInt(teamSize.getText()), 1);
			component.setValue(AgeSettingLabel, Integer.parseInt(projectAge.getText()), 0);


			ProjectService service = ServiceManager.getService(myProject, ProjectService.class);
			if (service.isFormSet()) RadarChartSetter.getInstance().setChartPlots();
		}

	}

	public void reset() {
		setSettings();
	}

	private void setSettings() {
		PropertiesComponent component = PropertiesComponent.getInstance(myProject);

		Boolean CBC = component.getBoolean(CBCSettingLabel, true);
		Boolean C3 = component.getBoolean(C3SettingLabel, true);
		Boolean LCOM = component.getBoolean(LCOMSettingLabel, true);
		Boolean CCBC = component.getBoolean(CCBCSettingLabel, true);
		Boolean CR = component.getBoolean(CRSettingLabel, true);
		Boolean LOC = component.getBoolean(LOCSettingLabel, true);
		Boolean WMC = component.getBoolean(WMCSettingLabel, true);
		Boolean CD = component.getBoolean(CDSettingLabel, true);

		int sec = component.getInt(SecSettingLabel, 300);
		int word = component.getInt(WordSettingLabel, 300);
		int size = component.getInt(SizeSettingLabel, 1);
		int age = component.getInt(AgeSettingLabel, 0);

		CBCCheckBox.setSelected(CBC);
		c3CheckBox.setSelected(C3);
		LCOMCheckBox.setSelected(LCOM);
		CCBCCheckBox.setSelected(CCBC);
		CRCheckBox.setSelected(CR);
		LOCCheckBox.setSelected(LOC);
		WMCCheckBox.setSelected(WMC);
		CDCheckBox.setSelected(CD);
		seconds.setValue(sec);
		words.setValue(word);
		teamSize.setText(Integer.toString(size));
		projectAge.setText(Integer.toString(age));

	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		rootPanel = new JPanel();
		rootPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
		metricsPanel = new JPanel();
		metricsPanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
		rootPanel.add(metricsPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		metricsPanel.setBorder(BorderFactory.createTitledBorder("Metrics to be computed"));
		CBCCheckBox = new JCheckBox();
		CBCCheckBox.setSelected(true);
		CBCCheckBox.setText("CBC");
		CBCCheckBox.setToolTipText("Coupling Between Objects");
		metricsPanel.add(CBCCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		c3CheckBox = new JCheckBox();
		c3CheckBox.setSelected(true);
		c3CheckBox.setText("C3");
		c3CheckBox.setToolTipText("Conceptual Cohesion of Classes");
		metricsPanel.add(c3CheckBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		LCOMCheckBox = new JCheckBox();
		LCOMCheckBox.setSelected(true);
		LCOMCheckBox.setText("LCOM");
		LCOMCheckBox.setToolTipText("Lack of Cohesion of Methods");
		metricsPanel.add(LCOMCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		CCBCCheckBox = new JCheckBox();
		CCBCCheckBox.setSelected(true);
		CCBCCheckBox.setText("CCBC");
		CCBCCheckBox.setToolTipText("Conceptual Coupling Between Classes");
		metricsPanel.add(CCBCCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		CRCheckBox = new JCheckBox();
		CRCheckBox.setSelected(true);
		CRCheckBox.setText("CR");
		CRCheckBox.setToolTipText("Code redability");
		metricsPanel.add(CRCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		LOCCheckBox = new JCheckBox();
		LOCCheckBox.setSelected(true);
		LOCCheckBox.setText("LOC");
		LOCCheckBox.setToolTipText("Lines of code");
		metricsPanel.add(LOCCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		WMCCheckBox = new JCheckBox();
		WMCCheckBox.setSelected(true);
		WMCCheckBox.setText("WMC");
		WMCCheckBox.setToolTipText("McCabe Cyclomatic Complexity");
		metricsPanel.add(WMCCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		CDCheckBox = new JCheckBox();
		CDCheckBox.setSelected(true);
		CDCheckBox.setText("CD");
		CDCheckBox.setToolTipText("Comment Density");
		metricsPanel.add(CDCheckBox, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		timePanel = new JPanel();
		timePanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		rootPanel.add(timePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		timePanel.setBorder(BorderFactory.createTitledBorder("Recompute Metrics every:"));
		final JLabel label1 = new JLabel();
		label1.setText("Seconds");
		timePanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText("Words");
		timePanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		seconds = new JSlider();
		seconds.setMajorTickSpacing(300);
		seconds.setMaximum(3600);
		seconds.setMinimum(60);
		seconds.setMinorTickSpacing(10);
		seconds.setPaintLabels(true);
		seconds.setPaintTicks(true);
		seconds.setPaintTrack(true);
		seconds.setSnapToTicks(false);
		seconds.setValue(300);
		seconds.setValueIsAdjusting(false);
		seconds.putClientProperty("JSlider.isFilled", Boolean.FALSE);
		seconds.putClientProperty("Slider.paintThumbArrowShape", Boolean.FALSE);
		seconds.putClientProperty("html.disable", Boolean.FALSE);
		timePanel.add(seconds, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		words = new JSlider();
		words.setMajorTickSpacing(100);
		words.setMaximum(2000);
		words.setMinimum(10);
		words.setMinorTickSpacing(10);
		words.setPaintLabels(true);
		words.setPaintTicks(true);
		timePanel.add(words, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		projectSizePanel = new JPanel();
		projectSizePanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		rootPanel.add(projectSizePanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		projectSizePanel.setBorder(BorderFactory.createTitledBorder("Project Settings:"));
		final JLabel label3 = new JLabel();
		label3.setText("Team Size:");
		projectSizePanel.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("Project Age (Months)");
		projectSizePanel.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		teamSize = new JTextField();
		teamSize.setText("1");
		projectSizePanel.add(teamSize, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		projectAge = new JTextField();
		projectAge.setText("0");
		projectSizePanel.add(projectAge, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return rootPanel;
	}
}
