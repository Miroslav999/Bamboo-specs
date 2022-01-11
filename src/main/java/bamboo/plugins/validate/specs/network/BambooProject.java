package bamboo.plugins.validate.specs.network;

import java.util.List;

public class BambooProject {

	private String projectName;
	private String projectKey;
	private List<BambooPlan> plans;
	private String packageName;

	public BambooProject(String projectName, String projectKey) {
		this.projectName = projectName;
		this.projectKey = projectKey;
		this.packageName = projectName.replaceAll(" ", "");
		
		if (packageName.matches("^[0-9].*")) {
			packageName = new StringBuilder(packageName).insert(0, "P").toString();
		}
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public List<BambooPlan> getPlans() {
		return plans;
	}

	public void setPlans(List<BambooPlan> plans) {
		this.plans = plans;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

}
