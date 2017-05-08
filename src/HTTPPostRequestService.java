import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
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

/**
 * Created by giggiux on 07/05/2017.
 */
public class HTTPPostRequestService {

	private Project myProject;

	private Map<String, Double> projectPercentileValues = new HashMap<>();
	private Map<String, Double> filePercentileValues = new HashMap<>();

	public HTTPPostRequestService(@NotNull  Project myProject) {
		this.myProject = myProject;
	}


	public void fileIsChanged() {
		filePercentileValues = new HashMap<>();
		sendRequestForPercentileValues();
	}

	public void sendRequestForPercentileValues() {


		HashMap<String, Double> projectMetrics = MetricsUser.getInstance().getProjectMetrics();

		HashMap<String, Double> fileMetrics = new HashMap<>();

		Editor e = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
		if (e != null) {
			PsiFile file = PsiUtilBase.getPsiFileInEditor(e, myProject);
			if (file != null) {
				fileMetrics = MetricsUser.getInstance().getFileMetrics(file.getVirtualFile().getPath().replace("file://", ""));
			}
		}



		if (!projectMetrics.isEmpty()) {
			// TODO: make url as setting
			String url = "http://localhost:9000/";

			HttpClient client = HttpClients.createDefault();
			HttpPost post = new HttpPost(url);

			// add header
			post.setHeader("User-Agent", USER_AGENT);


			PropertiesComponent component = PropertiesComponent.getInstance(myProject);

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


			double percentage = 0.5;

			List<NameValuePair> urlParameters = new ArrayList<>();


			// TODO: This only if the filter is checked
			urlParameters.add(new BasicNameValuePair("devnumberMax",  Double.toString(Math.ceil(devnumber + devnumber * percentage )) ));
			urlParameters.add(new BasicNameValuePair("devnumberMin", Double.toString(Math.floor(devnumber - devnumber * percentage )) ));

			urlParameters.add(new BasicNameValuePair("classnumberMax", Double.toString(Math.ceil(classnumber + classnumber * percentage )) ));
			urlParameters.add(new BasicNameValuePair("classnumberMin", Double.toString(Math.floor(classnumber - classnumber * percentage )) ));

			urlParameters.add(new BasicNameValuePair("locMax", Double.toString(Math.ceil(loc + loc * percentage )) ));
			urlParameters.add(new BasicNameValuePair("locMin", Double.toString(Math.floor(loc - loc * percentage )) ));

			urlParameters.add(new BasicNameValuePair("c3", Double.toString(c3)) );
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

				// TODO: This only if the filter is checked
				urlParameters.add(new BasicNameValuePair("locFileMax", Double.toString(Math.ceil(loc + loc * percentage )) ));
				urlParameters.add(new BasicNameValuePair("locFileMin", Double.toString(Math.floor(loc - loc * percentage )) ));

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

				HttpResponse response = client.execute(post);
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					InputStream instream = entity.getContent();
					try {
						String result = IOUtils.toString(instream);
						System.out.println(result);
						// TODO: parse result and add them to the Maps to be saved -> RadarChartSetter.setChartPlots()
					} finally {
						instream.close();
					}
				}
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
						List<String> values = new ArrayList<String>();
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

}
