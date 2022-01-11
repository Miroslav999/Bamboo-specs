package bamboo.plugins.validate.specs.network;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLBambooProjectHandler extends DefaultHandler {

	private List<BambooProject> projects = new ArrayList<>(100);

	private String projectKey, projectName;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (qName.equals("project")) {
			projectKey = attributes.getValue("key");
			projectName = attributes.getValue("name");
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ((projectKey != null && !projectKey.isEmpty())) {
			projects.add(new BambooProject(projectName, projectKey));
		}
		projectKey = null;
		projectName = null;

	}

	public List<BambooProject> getProjects() {
		return projects;
	}

}
