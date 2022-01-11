package bamboo.plugins.validate.specs.network;

import java.io.File;

public class BambooPlan {
	private BambooProject project;
	private String buildKey;
	private String code;
	private File etalonFile;
	private File actualFile;
	private String className;
	private String packageName;

	public BambooPlan(BambooProject project, String buildKey, String code) {
		this.project = project;
		this.buildKey = buildKey;
		this.code = code;
	}

	public BambooProject getProject() {
		return project;
	}

	public void setProject(BambooProject project) {
		this.project = project;
	}

	public String getBuildKey() {
		return buildKey;
	}

	public void setBuildKey(String buildKey) {
		this.buildKey = buildKey;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public File getEtalonFile() {
		return etalonFile;
	}

	public void setEtalonFile(File etalonFile) {
		this.etalonFile = etalonFile;
	}

	public File getActualFile() {
		return actualFile;
	}

	public void setActualFile(File actualFile) {
		this.actualFile = actualFile;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
}
