package bamboo.plugins.validate.specs.mojo;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import bamboo.plugins.validate.specs.network.BambooProject;
import bamboo.plugins.validate.specs.network.BambooRest;
import bamboo.plugins.validate.specs.utils.CreatorSpecs;
import bamboo.plugins.validate.specs.utils.Utils;

@Mojo(name = "refactoring-source-code-bamboo-plans")
public class RefactoringSourceCodeMojo extends AbstractMojo {

	@Parameter(property = "url", required = true)
	private String url;

	@Parameter(property = "token", required = true)
	private String token;

	@Parameter(property = "resourcePath", required = true)
	private File resourcePath;

	@Parameter(property = "projects", required = false)
	private String[] projects;

	@Parameter(property = "overwriteEtalon", required = false)
	private boolean overwrite;

	@Parameter(property = "pathToProjects", required = false)
	private File pathToProjects;

	private String baseClass = "BambooSpecBaseClass";
	private String packageBaseClass = "bamboo.specs.utils";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().info("Start generate bamboo spec. URL " + url);

		if (pathToProjects != null) {
			projects = Utils.getProjects(pathToProjects);
		}

		List<BambooProject> bambooProjects = new BambooRest(url, token, resourcePath, getLog()).getProjectsByList(projects);

		if (bambooProjects.isEmpty()) {
			throw new MojoFailureException("Согласно переданному списку проектов не найдены соответствующие проекты на Bamboo");
		}

		CreatorSpecs creatorSpecs = new CreatorSpecs(packageBaseClass + "." + baseClass, getLog());

		creatorSpecs.saveEtalons(bambooProjects, resourcePath, overwrite);

		creatorSpecs.createBaseClass(packageBaseClass, baseClass);

		creatorSpecs.createClasses(bambooProjects);

		// TODO пока отключил вынос текстов скриптов в отдельный файл в ресурсы
		// Когда парсим файл java, то строка, содежращая спец. символы типа \n сохранится в файл с таким символом.
		// При раскатке такого плана в теле скрипта тоже получим \n, скрипт становится невалидым
		// creatorSpecs.saveBodyScriptsInResource(Paths.get(userDirectory, constantPath, "ScriptTaskConstant.java"));
	}

}
