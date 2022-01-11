package bamboo.plugins.validate.specs.mojo;

import static bamboo.plugins.validate.specs.utils.Constant.packagePath;
import static bamboo.plugins.validate.specs.utils.Constant.userDirectory;

import java.io.File;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import bamboo.plugins.validate.specs.utils.MavenProject;

@Mojo(name = "publish-bamboo-spec")
public class PublishSpecMojo extends AbstractMojo {

	@Parameter(property = "pathToTemporaryProject", required = true)
	private File pathToTemporaryProject;

	@Parameter(property = "credentials", required = true)
	private File credentials;

	@Parameter(property = "baseClassFile", required = true)
	private String baseClassFile;

	@Parameter(property = "projects", required = false)
	private String[] projects;

	@Parameter(property = "plans", required = false)
	private String[] plans;

	@Parameter(property = "deleteTemporaryProject", required = false)
	private boolean deleteTemporaryProject = true;

	@Parameter(property = "useSavedSources", required = false)
	private boolean useSavedSources;

	@Parameter(property = "resourcePath", required = true)
	private File resourcePath;

	private MavenProject mavenProject;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		mavenProject = new MavenProject(getLog(), pathToTemporaryProject);

		mavenProject.create(pathToTemporaryProject);

		mavenProject.replaceCredentials(credentials);

		if (useSavedSources) {
			mavenProject.copyProjects(projects, resourcePath);

			mavenProject.copyPlans(plans, resourcePath);
		} else {
			mavenProject.copyConstans();

			mavenProject.copyBaseClass(baseClassFile);

			mavenProject.copyProjects(projects, Paths.get(userDirectory, packagePath).toFile());

			mavenProject.copyPlans(plans, Paths.get(userDirectory, packagePath).toFile());
		}

		mavenProject.runPublishSpecs();
	
		if (deleteTemporaryProject) {
			mavenProject.delete();
		}

	}

}
