package services;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import configuration.gui.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.httpclient.params.HttpMethodParams.USER_AGENT;


public class HTTPPostRequestService {

	private Project myProject;

	private Map<String, Double> projectPercentileValues = new HashMap<>();
	private Map<String, Double> filePercentileValues = new HashMap<>();

	public HTTPPostRequestService(@NotNull Project myProject) {
		this.myProject = myProject;
	}


	public void fileIsChanged() {
		filePercentileValues = new HashMap<>();
		sendRequestForPercentileValues();
	}

	public void sendRequestForPercentileValues() {


		MetricsHandlerService metricsHandlerService = ServiceManager.getService(myProject, MetricsHandlerService.class);

		HashMap<String, Double> projectMetrics = metricsHandlerService.getProjectMetrics();

		HashMap<String, Double> fileMetrics = new HashMap<>();

		Editor e = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
		if (e != null) {
			PsiFile file = PsiUtilBase.getPsiFileInEditor(e, myProject);
			if (file != null) {
				fileMetrics = metricsHandlerService.getFileMetrics(file.getVirtualFile().getPath().replace("file://", ""));
			}
		}


		if (!projectMetrics.isEmpty()) {

			PropertiesComponent component = PropertiesComponent.getInstance(myProject);


			String server = component.getValue(Settings.ServerAddressLabel, "");

			String url = server.isEmpty() ? "http://rio.inf.usi.ch:38100/" : server;

			HttpClient client = HttpClients.createDefault();
			HttpPost post = new HttpPost(url);

			// add header
			post.setHeader("User-Agent", USER_AGENT);


			double devnumber = (double) component.getInt(Settings.SizeSettingLabel, 1);
			double classnumber = projectMetrics.size();
			double loc = projectMetrics.get("LOC");
			double c3 = projectMetrics.get("C3");
			double cr = projectMetrics.get("CR");
			double cd = projectMetrics.get("CD");
			double cbo = projectMetrics.get("CBO");
			double lcom = projectMetrics.get("LCOM");
			double ccbc = projectMetrics.get("CCBC");
			double wmc = projectMetrics.get("WMC");


			double percentage = (double) component.getInt(Settings.PercentageSettingLabel, 30)/100;

			List<NameValuePair> urlParameters = new ArrayList<>();


			if (component.getBoolean(Settings.TeamSizeCheckLabel, false)) {
				urlParameters.add(new BasicNameValuePair("devnumberMax", Double.toString(Math.ceil(devnumber + devnumber * percentage))));
				urlParameters.add(new BasicNameValuePair("devnumberMin", Double.toString(Math.floor(devnumber - devnumber * percentage))));
			}
			if (component.getBoolean(Settings.SimilarSizeCheckLabel, false)) {
				urlParameters.add(new BasicNameValuePair("classnumberMax", Double.toString(Math.ceil(classnumber + classnumber * percentage))));
				urlParameters.add(new BasicNameValuePair("classnumberMin", Double.toString(Math.floor(classnumber - classnumber * percentage))));

				urlParameters.add(new BasicNameValuePair("locMax", Double.toString(Math.ceil(loc + loc * percentage))));
				urlParameters.add(new BasicNameValuePair("locMin", Double.toString(Math.floor(loc - loc * percentage))));
			}

			urlParameters.add(new BasicNameValuePair("c3", Double.toString(c3)));
			urlParameters.add(new BasicNameValuePair("cr", Double.toString(cr)));
			urlParameters.add(new BasicNameValuePair("cd", Double.toString(cd)));
			urlParameters.add(new BasicNameValuePair("cbo", Double.toString(cbo)));
			urlParameters.add(new BasicNameValuePair("lcom", Double.toString(lcom)));
			urlParameters.add(new BasicNameValuePair("ccbc", Double.toString(ccbc)));
			urlParameters.add(new BasicNameValuePair("wmc", Double.toString(wmc)));


			if (!fileMetrics.isEmpty()) {
				loc = fileMetrics.get("LOC");
				c3 = fileMetrics.get("C3");
				cr = fileMetrics.get("CR");
				cd = fileMetrics.get("CD");
				cbo = fileMetrics.get("CBO");
				lcom = fileMetrics.get("LCOM");
				ccbc = fileMetrics.get("CCBC");
				wmc = fileMetrics.get("WMC");

				if (component.getBoolean(Settings.LineOfCodeCheckLabel, false)) {
					urlParameters.add(new BasicNameValuePair("locFileMax", Double.toString(Math.ceil(loc + loc * percentage))));
					urlParameters.add(new BasicNameValuePair("locFileMin", Double.toString(Math.floor(loc - loc * percentage))));
				}

				urlParameters.add(new BasicNameValuePair("file_c3", Double.toString(c3)));
				urlParameters.add(new BasicNameValuePair("file_cr", Double.toString(cr)));
				urlParameters.add(new BasicNameValuePair("file_cbo", Double.toString(cd)));
				urlParameters.add(new BasicNameValuePair("file_cd", Double.toString(cbo)));
				urlParameters.add(new BasicNameValuePair("file_lcom", Double.toString(lcom)));
				urlParameters.add(new BasicNameValuePair("file_ccbc", Double.toString(ccbc)));
				urlParameters.add(new BasicNameValuePair("file_wmc", Double.toString(wmc)));
			}

			try {
				post.setEntity(new UrlEncodedFormEntity(urlParameters, "UTF-8"));


				ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "HTTPRequestToMetricsServer") {
					@Override
					public void run(@NotNull ProgressIndicator indicator) {
						try {
							HttpResponse response = client.execute(post);
							HttpEntity entity = response.getEntity();

							if (entity != null) {
								InputStream instream = entity.getContent();
								try {
									String result = IOUtils.toString(instream);
									Map<String, Object> parameters = new HashMap<String, Object>();
									parseQuery(result, parameters);
									double countTot = paramToDouble("counttot", parameters);
									double countTotFile = paramToDouble("counttotfile", parameters);

									countTot = (countTot != 0) ? countTot : 1;
									countTotFile = (countTotFile != 0) ? countTotFile : 1;


									projectPercentileValues.put("CBO", paramToDouble("countcbo", parameters) * 100 / countTot);
									projectPercentileValues.put("CCBC", paramToDouble("countccbc", parameters) * 100 / countTot);
									projectPercentileValues.put("C3", paramToDouble("countc3", parameters) * 100 / countTot);
									projectPercentileValues.put("CR", paramToDouble("countcr", parameters) * 100 / countTot);
									projectPercentileValues.put("CD", paramToDouble("countcd", parameters) * 100 / countTot);
									projectPercentileValues.put("WMC", paramToDouble("countwmc", parameters) * 100 / countTot);
									projectPercentileValues.put("LCOM", paramToDouble("countlcom", parameters) * 100 / countTot);

									filePercentileValues.put("CBO", paramToDouble("countcbofile", parameters) * 100 / countTotFile);
									filePercentileValues.put("CCBC", paramToDouble("countccbcfile", parameters) * 100 / countTotFile);
									filePercentileValues.put("C3", paramToDouble("countc3file", parameters) * 100 / countTotFile);
									filePercentileValues.put("CR", paramToDouble("countcrfile", parameters) * 100 / countTotFile);
									filePercentileValues.put("CD", paramToDouble("countcdfile", parameters) * 100 / countTotFile);
									filePercentileValues.put("WMC", paramToDouble("countwmcfile", parameters) * 100 / countTotFile);
									filePercentileValues.put("LCOM", paramToDouble("countlcomfile", parameters) * 100 / countTotFile);


									RadarChartSetterService RadarChartSetter = ServiceManager.getService(myProject, RadarChartSetterService.class);

									RadarChartSetter.setChartPlots();

								} finally {
									instream.close();
								}
							}
						} catch (Exception e3) {
							e3.printStackTrace();
						}
					}
				});

			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	public Map<String, Double> getProjectPercentileValues() {
		return projectPercentileValues;
	}

	public Map<String, Double> getFilePercentileValues() {
		return filePercentileValues;
	}

	private void parseQuery(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {

		if (query != null) {
			String pairs[] = query.split("[&]");
			for (String pair : pairs) {
				String param[] = pair.split("[=]");
				String key = null;
				String value = null;
				if (param.length > 0) {
					key = URLDecoder.decode(param[0],
							System.getProperty("file.encoding"));
				}

				if (param.length > 1) {
					value = URLDecoder.decode(param[1],
							System.getProperty("file.encoding"));
				}

				if (parameters.containsKey(key)) {
					Object obj = parameters.get(key);
					if (obj instanceof List<?>) {
						List<String> values = (List<String>) obj;
						values.add(value);

					} else if (obj instanceof String) {
						List<String> values = new ArrayList<>();
						values.add((String) obj);
						values.add(value);
						parameters.put(key, values);
					}
				} else {
					parameters.put(key, value);
				}
			}
		}
	}

	private double paramToDouble(String key, Map<String, Object> parameters) {
		double value = -1;

		try {
			if (parameters.containsKey(key)) {
				Object obj = parameters.get(key);
				String objString = (String) obj;
				value = Double.parseDouble(objString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;

	}

}
