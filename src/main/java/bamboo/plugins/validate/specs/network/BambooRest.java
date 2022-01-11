package bamboo.plugins.validate.specs.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.SAXException;

public class BambooRest {

	private final String getSourceCodeProject = "rest/api/latest/project/{projectKey}/specs";
	private final String getProjectInfo = "rest/api/latest/project";
	private String token;
	private String baseUrl;
	private File resourceFolder;
	private Log log;

	public BambooRest(String baseUrl, String token, File folder, Log log) {
		this.token = "Bearer " + token;
		this.baseUrl = baseUrl;
		this.resourceFolder = folder;
		this.log = log;
	}

	/**
	 * Возвращает список всех проектов, для каждого проекта формируется список планов и исходный код планов
	 * 
	 * @throws MojoFailureException
	 */
	public List<BambooProject> getAllProjectSpecs(List<BambooProject> projects) throws MojoFailureException {

		for (BambooProject project : projects) {

			project.setPlans(getPlans2(project));
		}

		return projects;
	}

	/**
	 * Возвращает список проектов согласно списку
	 * 
	 * @throws MojoFailureException
	 */

	public List<BambooProject> getProjectsByList(String[] projects) throws MojoFailureException {

		int maxProjects = getMaxProjects();

		Set<String> projetSet = new HashSet<>(projects.length);

		Collections.addAll(projetSet, projects);

		if (projetSet.contains("all")) {
			return getAllProjectSpecs(getProjects(maxProjects));
		}

		List<BambooProject> projetsAfterFilters = new ArrayList<>();

		for (BambooProject project : getProjects(maxProjects)) {

			if (projetSet.contains(project.getProjectName())) {
				projetsAfterFilters.add(project);
			}
		}

		return getAllProjectSpecs(projetsAfterFilters);
	}

	/**
	 * Возвращает список всем проектов
	 * 
	 * @throws MojoFailureException
	 */

//	public List<BambooProject> getAllProjects() throws MojoFailureException {
//
//		int maxProjects = getMaxProjects();
//
//		return getProjects(maxProjects);
//	}

	/**
	 * Возвращает информацию о всех проектах
	 * 
	 * @throws MojoFailureException
	 */

	private List<BambooProject> getProjects(int maxProjects) throws MojoFailureException {

		XMLBambooProjectHandler handler = new XMLBambooProjectHandler();

		HttpGet request = new HttpGet(baseUrl + getProjectInfo + "?max-result=" + maxProjects);

		request.addHeader("authorization", token);

		try (CloseableHttpResponse resp = executeRequest(request)) {

			if (resp == null) {
				throw new MojoFailureException("Сервер недоступен");
			}

			HttpEntity entity = resp.getEntity();

			File file = new File(resourceFolder.getAbsolutePath() + "\\projects.xml");

			try {
				FileUtils.copyInputStreamToFile(entity.getContent(), file);

				SAXParserFactory factory = SAXParserFactory.newInstance();

				SAXParser parser = factory.newSAXParser();

				parser.parse(file, handler);

			

			} catch (UnsupportedOperationException | ParserConfigurationException | SAXException e) {
				e.printStackTrace();
			} finally {
					if (file != null && file.exists()) {
						file.delete();
					}
				}

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return handler.getProjects();
	}

	/**
	 * Возвращает количество проектов
	 */
	// TODO Должен получать из запроса rest/api/latest/project, параметр max-result
	public int getMaxProjects() {
		return 96;
	}

	/**
	 * Получаем исходный код планов проекта
	 * 
	 */

	public List<BambooPlan> getPlans2(BambooProject project) {
		log.info("Получение исходников планов для проекта " + project.getProjectName());
		XMLBambooPlanHandler handler = new XMLBambooPlanHandler(project);

		HttpGet request = new HttpGet(baseUrl + getSourceCodeProject.replace("{projectKey}", project.getProjectKey()));

		request.addHeader("authorization", token);

		try (CloseableHttpResponse resp = executeRequest(request)) {

			HttpEntity entity = resp.getEntity();

			if (resp.getStatusLine().getStatusCode() != 200) {
				log.error("При получении с сервера возникла ошибка. Проект "
						+ project
						.getProjectName()
						+ ". Error: "
						+ IOUtils.toString(resp.getEntity().getContent(), "UTF-8"));
				return null;
			}

			if (entity != null) {
				File file = new File(resourceFolder.getAbsolutePath() + "\\response.xml");

				try {
					FileUtils.copyInputStreamToFile(entity.getContent(), file);

					SAXParserFactory factory = SAXParserFactory.newInstance();

					SAXParser parser = factory.newSAXParser();

					parser.parse(file, handler);

				} catch (UnsupportedOperationException | ParserConfigurationException | SAXException e) {
					log.error("Не удалось распарсить исходники планов для проекта " + project.getProjectName());
					e.printStackTrace();
				} finally {
					if (file != null && file.exists()) {
						file.delete();
					}
				}

			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return handler.getPlans();
	}

	/**
	 * Выполняет запрос
	 */
	public CloseableHttpResponse executeRequest(HttpRequestBase request) {

		CloseableHttpClient httpClient = HttpClients.createDefault();

		CloseableHttpResponse response = null;

		try {

			response = httpClient.execute(request);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return response;
	}

}
