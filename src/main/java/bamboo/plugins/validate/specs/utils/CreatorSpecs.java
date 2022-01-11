package bamboo.plugins.validate.specs.utils;

import static bamboo.plugins.validate.specs.utils.Constant.SRC_BASE_CLASS;
import static bamboo.plugins.validate.specs.utils.Constant.constantPath;
import static bamboo.plugins.validate.specs.utils.Constant.javaPackagePath;
import static bamboo.plugins.validate.specs.utils.Constant.packagePath;
import static bamboo.plugins.validate.specs.utils.Constant.userDirectory;
import static bamboo.plugins.validate.specs.utils.Constant.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.network.BambooProject;
import bamboo.plugins.validate.specs.rule.AddExtendClassRule;
import bamboo.plugins.validate.specs.rule.AddPackageDeclarationRule;
import bamboo.plugins.validate.specs.rule.CreateGetMethods;
import bamboo.plugins.validate.specs.rule.CreateMainMethod;
import bamboo.plugins.validate.specs.rule.RefactoringRule;
import bamboo.plugins.validate.specs.rule.RemoveMethodsRule;
import bamboo.plugins.validate.specs.rule.ReplaceWithConstantRule;

public class CreatorSpecs {

	private Log log;
	private JavaParser parser;
	private List<RefactoringRule> listRules;

	public CreatorSpecs(String strBaseClass, Log log) {
		this.log = log;
		parser = new JavaParser();
		listRules = new ArrayList<>();
		listRules.add(new ReplaceWithConstantRule());
		listRules.add(new RemoveMethodsRule());
		listRules.add(new AddExtendClassRule(strBaseClass));
		listRules.add(new CreateMainMethod());
		listRules.add(new CreateGetMethods());
		listRules.add(new AddPackageDeclarationRule());
	}

	/**
	 * Для каждого плана из списка будет проведен рефакторинг кода и в папку
	 * \\src\\main\\java\\bamboo\\specs\\projects\\{PROJECT_KEY}
	 * записан результат. Если в папке уже есть класс, то исходник перезаписываться не будет
	 */
	public void createClasses(List<BambooProject> projects) {

		for (BambooProject project : projects) {

			if (project.getPlans() == null) {
				continue;
			}

			for (BambooPlan plan : project.getPlans()) {

				log.info("Обработка класса " + plan.getActualFile().getAbsolutePath());

				if (plan.getActualFile().exists()) {
					log.info("Сгенерированный класс уже существует " + plan.getActualFile().getAbsolutePath());
					continue;
				}

				ParseResult<CompilationUnit> parsedJavaCode = null;
				try {
					parsedJavaCode = parser.parse(plan.getEtalonFile());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					continue;
				}
				String result = null;
				// TODO javaparser не может распарсить пакет, который начинается с цифры, например, 2mca
				if (parsedJavaCode.getProblems() != null && parsedJavaCode.getProblems().size() > 0) {
					log.warn("Исходник не сохранен " + plan.getActualFile()
							.toString()
							+ " \r\nПроблемы при парсе исходника: " + parsedJavaCode.getProblems().stream()
									.map(a -> String.valueOf(a
											.getMessage()))
									.collect(Collectors.joining("|")));

					result = parsedJavaCode.getResult().get()
							.toString();
				} else {

					for (RefactoringRule rule : listRules) {
						rule.refactoring(plan, parsedJavaCode);
					}

					// TODO пока это самый простой способ заменить методы
					result = parsedJavaCode.getResult().get()
							.toString()
							.replace("new BambooKey(\"" + plan.getProject().getProjectKey() + "\")", "getProjectKey()")
							.replace("new BambooKey(\"" + plan.getBuildKey() + "\")", "getPlanKey()");
				}

				if (result == null) {
					log.error("Java код пустой " + plan.getActualFile().toString());
					continue;
				}

				try {
					Formatter formatter = new Formatter();
					FileUtils.write(plan.getActualFile(), formatter.formatSource(result), "UTF-8", false);
					log.info("Класс успешно обработан " + plan.getActualFile().getAbsolutePath());
				} catch (IOException | FormatterException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Сохраняет исходный код планов в папку resources/etalons
	 */
	public void saveEtalons(List<BambooProject> projects, File folder, boolean overwrite) {

		for (BambooProject project : projects) {

			if (project.getPlans() == null) {
				continue;
			}

			for (BambooPlan plan : project.getPlans()) {

				File etalonFolderPath = new File(
						Paths.get(folder.getAbsolutePath(), plan.getProject().getPackageName()).toString());

				if (!etalonFolderPath.exists()) {
					etalonFolderPath.mkdir();
				}

				String className = StringUtils.substringBetween(plan.getCode(), "class", "{").trim();

				// Bamboo генерирует имя java класса без проверки валидное оно или нет, если впереди стоит цифра, то перед ней
				// вставляем букву
				if (className.matches("^[0-9].*")) {
					String oldClassName = className;
					className = new StringBuilder(className).insert(0, "N").toString();
					plan.setCode(plan.getCode().replaceAll(oldClassName, className));
				}

				File etalonFile = Paths.get(etalonFolderPath.getAbsolutePath().toString(), className + ".java").toFile();

				plan.setClassName(className);

				plan.setEtalonFile(etalonFile);

				plan.setActualFile(
						Paths.get(userDirectory, packagePath, plan.getProject().getPackageName(), className + ".java")
								.toFile());

				plan.setPackageName(javaPackagePath + "." + plan.getProject().getPackageName());

				if (etalonFile.exists() && !overwrite) {
					log.info("Эталон уже существует " + etalonFile.getAbsolutePath().toString());
					continue;
				}

				try {

					// TODO это надо было для удаления пакета
					String srcCode = plan.getCode().replaceFirst("package [a-zA-Z0-9].*",
							"package " + plan.getPackageName() + ";");
					FileUtils.write(etalonFile,
							srcCode,
							"UTF-8",
							false);

					log.info("Сохранен эталон " + etalonFile.getAbsolutePath().toString());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}

	public void saveBodyScriptsInResource(Path path) {
		try {
			ParseResult<CompilationUnit> parsedJavaCode = parser.parse(path);
			String parentClassName = "ParentScriptTaskConstant";
			createParentScriptConstantClass(parentClassName);
			addExtendClass(parsedJavaCode, parentClassName);
			List<FieldDeclaration> fieldList = getScriptFields(parsedJavaCode);
			replaceInScripts(parsedJavaCode, fieldList);
			replaceScriptConstantScript(parsedJavaCode);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void replaceScriptConstantScript(ParseResult<CompilationUnit> parsedJavaCode) {
		String className = "ScriptTaskConstant";

		File classFile = Paths.get(userDirectory, constantPath, className + ".java").toFile();

		try {
			FileUtils.write(
					classFile,
					parsedJavaCode.getResult().get().toString(),
					"UTF-8",
					false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addExtendClass(ParseResult<CompilationUnit> parsedJavaCode, String parentClassName) {

		ClassOrInterfaceDeclaration classNode = NodeHelper.getClassNode(parsedJavaCode);

		classNode.addExtendedType(parentClassName);

	}

	private void createParentScriptConstantClass(String parentClassName) {

		File parentClassNameFile = Paths.get(userDirectory, constantPath, parentClassName + ".java").toFile();

		String sourceCode = "package bamboo.specs.constants;\r\n" +
				"\r\n" +
				"import java.io.IOException;\r\n" +
				"\r\n" +
				"import org.apache.commons.io.IOUtils;\r\n" +
				"\r\n" +
				"public class ParentScriptTaskConstant {\r\n" +
				"	protected static String getScriptText(String scriptName) {\r\n" +
				"		try {\r\n" +
				"			return IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptName\r\n"
				+
				"					+ \".txt\"),\r\n" +
				"					\"UTF-8\");\r\n" +
				"		} catch (IOException e) {\r\n" +
				"			e.printStackTrace();\r\n" +
				"		}\r\n" +
				"		throw new NullPointerException(\"Попытка получить текст скрипта \" + scriptName + \" закончилась неудачно\");\r\n"
				+
				"	}\r\n" +
				"}\r\n" +
				"";

		try {
			FileUtils.write(
					parentClassNameFile,
					sourceCode,
					"UTF-8",
					false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void replaceInScripts(ParseResult<CompilationUnit> parsedJavaCode, List<FieldDeclaration> fieldList) {
		for (FieldDeclaration field : fieldList) {
			String variableName = getVariableName(field);
			StringLiteralExpr scriptTextNode = getScriptText(field);
			String scriptText = scriptTextNode.getValue();
			File script = Paths.get(userDirectory, "src/main/resources", variableName + ".txt").toFile();
			
			MethodCallExpr method = new MethodCallExpr();
			method.setName("getScriptText");
			StringLiteralExpr arg = new StringLiteralExpr();
			arg.setValue(script.getName());
			NodeList<Expression> argList = new NodeList<Expression>();
			argList.add(arg);
			method.setArguments(argList);

			Node parentNode = scriptTextNode.getParentNode().get();
			parentNode.replace(scriptTextNode, method);

			if (script.exists()) {
				script.delete();
			}
			
			try {
				FileUtils.write(
						script,
						scriptText,
						"UTF-8",
						false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private String getVariableName(FieldDeclaration field) {
		return field.getVariable(0).getNameAsString();
	}

	private StringLiteralExpr getScriptText(FieldDeclaration field) {
		MethodCallExpr inlineBody = getInlineBody(field);
		return (StringLiteralExpr) inlineBody.getChildNodes().stream().filter(c -> c instanceof StringLiteralExpr).findFirst()
				.get();
	}

	private MethodCallExpr getInlineBody(Node field) {

		MethodCallExpr inlineBody = null;
		for (Node node : field.getChildNodes()) {
			if (node instanceof MethodCallExpr) {
				MethodCallExpr methodCallExpr = (MethodCallExpr) node;
				if (methodCallExpr.getNameAsString().equals("inlineBody")) {
					inlineBody = methodCallExpr;
					break;
				}
			}

			if (node.getChildNodes().size() > 0) {
				inlineBody = getInlineBody(node);
			}
		}
		return inlineBody;
	}

	private List<FieldDeclaration> getScriptFields(ParseResult<CompilationUnit> parsedJavaCode) {
		ClassOrInterfaceDeclaration className = NodeHelper.getClassNode(parsedJavaCode);
		List<FieldDeclaration> fields = new ArrayList<>();
		for (Node node : className.getChildNodes()) {
			if (node instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) node;
				if (field.toString().contains("inlineBody")) {
					fields.add(field);
				}
			}
		}
		return fields;
	}

	public void createBaseClass(String packageName, String baseClass) {

		File packageFolder = Paths.get(userDirectory, utils).toFile();

		packageFolder.mkdirs();

		File baseClassFile = Paths.get(packageFolder.getPath(), baseClass + ".java").toFile();
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(baseClassFile))) {
			writer.write(String.format(SRC_BASE_CLASS, packageName, baseClass));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
