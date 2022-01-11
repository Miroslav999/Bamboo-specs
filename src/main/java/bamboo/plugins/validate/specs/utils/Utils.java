package bamboo.plugins.validate.specs.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.base.Strings;

public class Utils {
	private Utils() {
	}

	public static String[] getProjects(File pathToProjects) throws MojoExecutionException {
		List<String> listOfProjects = new ArrayList<>();
		if (!pathToProjects.exists()) {
			throw new MojoExecutionException("Файл не существует " + pathToProjects.getAbsolutePath());
		}

		try (Stream<String> stream = Files.lines(pathToProjects.toPath())) {

			stream.forEach(line -> {
				if (!Strings.isNullOrEmpty(line)) {
					listOfProjects.add(line.trim());
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Во время чтения файла произошла ошибка " + pathToProjects.getAbsolutePath());
		}
		String[] projects = new String[listOfProjects.size()];
		return listOfProjects.toArray(projects);
	}

	public static void clearFolder(File file) {
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Не удалось удалить директорию " + file.getPath());
		}
	}
}
