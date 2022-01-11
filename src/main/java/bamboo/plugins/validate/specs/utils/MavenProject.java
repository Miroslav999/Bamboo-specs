package bamboo.plugins.validate.specs.utils;

import static bamboo.plugins.validate.specs.utils.Constant.constantPath;
import static bamboo.plugins.validate.specs.utils.Constant.packagePath;
import static bamboo.plugins.validate.specs.utils.Constant.userDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import javassist.NotFoundException;

public class MavenProject {

	private File folder;
	private File project;
	private File srcMainFolder;
	private String artifactId = "publish-spec";
	private String packageTutorial = "tutorial";
	private File credentialsFile;
	private Log log;

	public MavenProject(Log log, File folder) {
		this.folder = folder;
		this.project = Paths.get(folder.getPath(), artifactId).toFile();
		this.credentialsFile = Paths.get(project.getPath(), ".credentials").toFile();
		this.srcMainFolder = Paths.get(project.getPath(), Constant.srcMain).toFile();
		this.log = log;
	}

	public void create(File file) throws MojoExecutionException {

		if (project.exists()) {
			Utils.clearFolder(project);
		}

		project.mkdirs();

		try {
			generateMavenProject();
		} catch (NotFoundException e) {
			throw new MojoExecutionException(e.getMessage());
		}

		/* Приходится удалять папку src/main/java/tutorial, т.к. в результате создания мавен плагина в ней лежит пример спеки */
		Utils.clearFolder(Paths.get(srcMainFolder.getPath(), packageTutorial).toFile());

		/* По умолчанию тест компилируется с ошибкой, поэтому тоже удаляем */
		Utils.clearFolder(Paths.get(project.getPath(), Constant.srcTest, packageTutorial).toFile());

	}

	private void generateMavenProject() throws NotFoundException {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setBaseDirectory(folder);
		request.setGoals(Collections.singletonList("archetype:generate"));
		request.setBatchMode(true);
		Properties properties = new Properties();
		properties.setProperty("groupId", "com.maven");
		properties.setProperty("artifactId", artifactId);
		properties.setProperty("archetypeVersion", "7.2.3");
		properties.setProperty("archetypeGroupId", "com.atlassian.bamboo");
		properties.setProperty("archetypeArtifactId", "bamboo-specs-archetype");
		properties.setProperty("version", "1.0.0-SNAPSHOT");
		properties.setProperty("package", packageTutorial);
		request.setProperties(properties);
		executeRequest(request);
	}

	private void executeRequest(InvocationRequest request) throws NotFoundException {
		Invoker invoker = new DefaultInvoker();
		try {

			String m2Home = System.getenv("M2_HOME");

			if (m2Home == null || m2Home.isEmpty()) {
				throw new NotFoundException("M2_HOME is empty");
			}

			invoker.setMavenHome(new File(m2Home));
			invoker.execute(request);

		} catch (MavenInvocationException e) {
			e.printStackTrace();
		}
	}

	public void copyProjects(String[] projects, File folder) throws MojoExecutionException {

		if (projects == null || projects.length == 0) {
			return;
		}

		if (Arrays.stream(projects).anyMatch("ALL"::equalsIgnoreCase)) {
			projects = new String[folder.listFiles().length];
			for (int i = 0; i < folder.listFiles().length; i++) {
				projects[i] = folder.listFiles()[i].getName();
			}

		}

		for (String projectName : projects) {
			log.info("Копирование папки проекта " + projectName);
			File projectFolder = Paths.get(folder.toString(), projectName).toFile();
			File dest = Paths.get(project.getPath(), packagePath, projectName).toFile();
			try {
				FileUtils.copyDirectory(projectFolder, dest);
			} catch (IOException e) {
				e.printStackTrace();
				throw new MojoExecutionException("Во время копирования папки проекта " + projectName + " произошла ошибка");
			}
		}
	}

	public void copyPlans(String[] plans, File parentProjectFolder) throws MojoExecutionException {
		if (plans == null || plans.length == 0) {
			return;
		}

		for (String fullName : plans) {

			if (fullName == null) {
				continue;
			}

			log.info("Копирование плана " + fullName);
			String[] str = fullName.split("-");

			if (str.length != 2) {
				throw new MojoExecutionException("Неправильно указано имя плана " + fullName
						+ ". Должно соответствовать шаблону {PROJECT_NAME}-{PLAN_NAME}");
			}

			String projectName = str[0];
			String planName = str[1] + ".java";

			File projectFolder = Paths.get(parentProjectFolder.toString(), projectName, planName).toFile();
			File dest = Paths.get(project.getPath(), packagePath, projectName, planName).toFile();
			try {
				FileUtils.copyFile(projectFolder, dest);
			} catch (IOException e) {
				e.printStackTrace();
				throw new MojoExecutionException("Во время копирования плана " + fullName + " произошла ошибка");
			}
		}
	}

	public void runPublishSpecs() {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setBaseDirectory(project);
		request.setGoals(Collections.singletonList("-Ppublish-specs"));
		request.setBatchMode(true);
		try {
			executeRequest(request);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	public void replaceCredentials(File credentials) throws MojoExecutionException {
		try {
			FileUtils.copyFile(credentials, credentialsFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Во время копирования файла " + credentials.toPath() + " произошла ошибка");
		}
	}

	public void copyConstans() throws MojoExecutionException {
		log.info("Копирование пакета с константами");
		File projectFolder = Paths.get(userDirectory, constantPath).toFile();
		File dest = Paths.get(project.getPath(), constantPath).toFile();
		try {
			FileUtils.copyDirectory(projectFolder, dest);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Во время копирования констант произошла ошибка");
		}
	}

	public void copyBaseClass(String baseClass) throws MojoExecutionException {
		log.info("Копирование базового класса");
		File projectFolder = Paths.get(userDirectory, baseClass).toFile();
		File dest = Paths.get(project.getPath(), baseClass).toFile();
		try {
			FileUtils.copyFile(projectFolder, dest);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Во время копирования базового класса произошла ошибка");
		}
	}

	public void delete() throws MojoExecutionException {
		if (project == null) {
			return;
		}

		try {
			FileUtils.deleteDirectory(project);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Во время удаления временного проекта произошла ошибка " + project.getPath());
		}
	}
}
