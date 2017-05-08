import com.github.mauricioaniche.ck.CKNumber;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by giggiux on 05/05/2017.
 */
public class MetricsUser {
	private static MetricsUser ourInstance = new MetricsUser();

	private MetricsUser() {
	}

	public static MetricsUser getInstance() {
		return ourInstance;
	}

	public HashMap<String, Double> getProjectMetrics() {

		Collection<CKNumber> allReport = MyTaskExecutor.getInstance().getMetrics();

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

	public HashMap<String, Double> getFileMetrics(String file) {
		Collection<CKNumber> allReport = MyTaskExecutor.getInstance().getMetrics();

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

		return fileMetrics;
	}
}
