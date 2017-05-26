package configuration;

import configuration.gui.Settings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 * Created by giggiux on 14/04/2017.
 */
public class SettingsConfigurable implements Configurable {
	private JComponent myComponent;
	private Settings myPanel;

	public SettingsConfigurable(Project project) {
		myPanel = new Settings(project);
		myComponent = myPanel.getRootPanel();
	}

	@Nls
	@Override
	public String getDisplayName() {
		return "Scala";
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		return myComponent;
	}

	@Override
	public boolean isModified() {
		return myPanel.isModified();
	}

	@Override
	public void apply() throws ConfigurationException {
		myPanel.apply();
	}

	@Override
	public void reset() {
		myPanel.reset();

	}
}
