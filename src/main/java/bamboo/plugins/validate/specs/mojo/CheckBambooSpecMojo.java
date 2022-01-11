package bamboo.plugins.validate.specs.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import bamboo.plugins.validate.specs.errors.ErrorSpec;
import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.network.BambooProject;
import bamboo.plugins.validate.specs.network.BambooRest;
import bamboo.plugins.validate.specs.utils.Utils;

@Mojo(name = "check-bamboo-plans")
public class CheckBambooSpecMojo extends AbstractMojo {

	@Parameter(property = "url", required = true)
	private String url;

	@Parameter(property = "token", required = true)
	private String token;

	@Parameter(property = "resourcePath", required = true)
	private String resourcePath;

	@Parameter(property = "projects", required = false)
	private String[] projects;

	@Parameter(property = "pathToProjects", required = false)
	private File pathToProjects;

	private File resourceFolder;

	private List<ErrorSpec> errorSpecs = new ArrayList<>();;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		resourceFolder = Paths.get(System.getProperty("user.dir"), resourcePath).toFile();

		BambooRest rest = new BambooRest(url, token, resourceFolder, getLog());

		if (pathToProjects != null) {
			projects = Utils.getProjects(pathToProjects);
		}

		List<BambooProject> bambooProjects = rest.getProjectsByList(projects);

		if (bambooProjects.size() == 0) {
			throw new MojoFailureException("Согласно переданному списку проектов не найдены соответствующие проекты на Bamboo");
		}

		validateActualProjects(bambooProjects);

		checkError();

	}

	private void validateActualProjects(List<BambooProject> projects) {

		for (BambooProject project : projects) {

			File projectEtalons = new File(resourceFolder.getAbsolutePath() + "/" + project.getPackageName());

			if (!projectEtalons.exists()) {
				continue;
			}

			getLog().info("Проверяю проект " + project.getProjectName());

			Map<String, File> etalons = readEtalons(projectEtalons.listFiles());

			comparePlans(project.getPlans(), etalons);
		}

	}

	private void checkError() throws MojoFailureException {
		if (errorSpecs != null && errorSpecs.size() > 0) {
			for (ErrorSpec errorSpec : errorSpecs) {

				if (errorSpec.getEtalon() != null) {
					getLog().error(errorSpec.getReason() + " " + errorSpec.getEtalon().getAbsolutePath());
					File actualCodeFile = new File(errorSpec.getEtalon().getAbsolutePath().replace(".java", "_actual.java"));

					try {
						FileUtils.write(actualCodeFile, errorSpec
								.getActualCode(),
								"UTF-8",
								false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					getLog().error(errorSpec.getReason());
				}

			}

			throw new MojoFailureException("В процессе сравнения спецификаций планов выявлено несоответствие");
		}

	}

	private void comparePlans(List<BambooPlan> plans, Map<String, File> etalons) {

		for (BambooPlan plan : plans) {

			String className = StringUtils.substringBetween(plan.getCode(), "class", "{").trim().toLowerCase();

			File etalon = etalons.get(className);

			if (etalon != null) {

				String etalonCode = null;

				try {
					etalonCode = new String(Files.readAllBytes(Paths.get(etalon.getAbsolutePath())),
							StandardCharsets.UTF_8);
					etalonCode = removePackage(etalonCode);
				} catch (IOException e) {
					errorSpecs.add(new ErrorSpec(etalon, e.getMessage()));
					e.printStackTrace();
				}
				
				String actualCode = removePackage(plan.getCode());
				
				if (!compare(etalonCode, actualCode)) {
					errorSpecs.add(new ErrorSpec(etalon, actualCode, "Эталон не совпадает"));
				}

			}

		}

	}

	private String removePackage(String source) {
		return source.substring(source.indexOf('\n') + 1);
	}

	private boolean compare(String etalon, String code) {

		String codeWithoutFormat = removeFormatting(code);

		String etalonWithoutFormat = removeFormatting(etalon);

		return codeWithoutFormat.equals(etalonWithoutFormat);

	}

	private String removeFormatting(String str) {
		return str.replaceAll("\n|\t| |\r", "").trim().toLowerCase();
	}

	private Map<String, File> readEtalons(File[] files) {

		Map<String, File> etalons = new HashMap<>();

		for (File file : files) {
			if (file.isDirectory()) {
				etalons.putAll(readEtalons(file.listFiles()));
			} else if (file.isFile()) {
				etalons.put(file.getName().replaceFirst("[.][^.]+$", "").toLowerCase(), file);
			}
		}

		return etalons;
	}
}