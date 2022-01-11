package bamboo.plugins.validate.specs.errors;

import java.io.File;

public class ErrorSpec {
	private File etalon;
	private String actualCode;
	private String reason;

	public ErrorSpec(File etalon, String reason) {
		this.etalon = etalon;
		this.reason = reason;
	}

	public ErrorSpec(File etalon, String actualCode, String reason) {
		this.etalon = etalon;
		this.actualCode = actualCode;
		this.reason = reason;
	}

	public File getEtalon() {
		return etalon;
	}

	public void setEtalon(File etalon) {
		this.etalon = etalon;
	}

	public String getActualCode() {
		return actualCode;
	}

	public void setActualCode(String actualCode) {
		this.actualCode = actualCode;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
