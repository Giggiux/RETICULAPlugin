package toolWindow;

import com.intellij.openapi.project.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.Content;

import org.jetbrains.annotations.NotNull;
import toolWindow.gui.MetricsToolWindowForm;

import javax.swing.*;

/**
 * Created by giggiux on 27/04/2017.
 */
public class MetricsToolWindowFactory implements ToolWindowFactory {


	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

		MetricsToolWindowForm toolWindowUI = new MetricsToolWindowForm(project);

		JPanel rootPanel = toolWindowUI.getRootPanel();

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(rootPanel, "", true);
		toolWindow.getContentManager().addContent(content);
	}

}
