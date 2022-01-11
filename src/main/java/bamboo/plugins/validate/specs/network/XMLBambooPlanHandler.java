package bamboo.plugins.validate.specs.network;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLBambooPlanHandler extends DefaultHandler {

	private BambooProject project;

	public XMLBambooPlanHandler(BambooProject project) {
		super();
		this.project = project;
	}

	private List<BambooPlan> plans = new ArrayList<>(50);

	private String projectKey, buildKey, lastElementName;
	private StringBuilder code;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		lastElementName = qName;

		if (qName.equals("spec")) {
			projectKey = attributes.getValue("projectKey");
			buildKey = attributes.getValue("buildKey");
			code = new StringBuilder();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String information = new String(ch, start, length);

		if (("code").equals(lastElementName)) {
			code.append(information);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ((projectKey != null && !projectKey.isEmpty()) && (code != null && !code.toString().isEmpty())) {
			// Удаляем все методы .oid, пока это самый простой способ
			plans.add(new BambooPlan(project,
					buildKey,
					code.toString().replaceAll(".oid\\([a-zA-z| ]+\\(\"[a-zA-Z0-9]+\"\\)\\)", "")));
		}
		projectKey = null;
		buildKey = null;
		code = null;
		lastElementName = null;
	}

	public List<BambooPlan> getPlans() {
		return plans;
	}

}