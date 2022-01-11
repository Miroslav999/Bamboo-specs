package bamboo.plugins.validate.specs.utils;

import static bamboo.plugins.validate.specs.utils.Constant.constantPath;
import static bamboo.plugins.validate.specs.utils.Constant.userDirectory;
import static bamboo.plugins.validate.specs.utils.NodeHelper.getClassNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;

public class ConstantHelper {

	public static final String POST_PREFIX_CLASS = "Constant";
	private String template = "\n|\t| |\r";
	private Map<String, Map<String, Node>> allVariables = new HashMap<>();
	private JavaParser parser = new JavaParser();
	private String[] constantPathnames;

	// TODO в шаблоне заменить название пакета на переменную
	private String templateJavaClass = "package bamboo.specs.constants;\r\n" +
			"\r\n" +
			"public class %s {\r\n" +
			"}";

	private Map<String, Node> variables;

	public ConstantHelper() {
		uploadConstants();
	}

	/**
	 * Создание Java класса
	 */
	public void createClass(String className) {
		File classJava = Paths.get(userDirectory, constantPath, className + ".java").toFile();

		if (!classJava.exists()) {
			try {
				classJava.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(classJava))) {
			writer.write(String.format(templateJavaClass, className));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Загрузка информации о константах в проекте
	 */
	public void uploadConstants() {

		File f = new File(userDirectory + constantPath);

		constantPathnames = f.list();

		if (!f.exists()) {
			f.mkdirs();
			return;
		}

		for (String pathname : constantPathnames) {

			File file = new File(userDirectory + constantPath + pathname);

			ParseResult<CompilationUnit> result = null;

			variables = null;

			try {
				result = parser.parse(file);

				if (!result.getResult().isPresent()) {
					throw new RuntimeException("result is null");
				}

				if (!result.getResult().get().getTypes().getFirst().isPresent()) {
					throw new RuntimeException("getFirst() is null");
				}

				variables = getVariables(result.getResult().get().getTypes().getFirst().get().getMembers());

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			String className = ((ClassOrInterfaceDeclaration) getClassNode(result)).getNameAsString();

			allVariables.put(className, variables);
		}
	}

	public Map<String, Node> getVariables(NodeList<BodyDeclaration<?>> members) {

		Map<String, Node> map = new HashMap<>();
		for (int i = 0; i < members.size(); i++) {

			if (members.get(i) instanceof FieldDeclaration) {
				FieldDeclaration body = (FieldDeclaration) members.get(i);
				String variableName = body.getVariable(0).getNameAsString();
				Node value = (Node) body.getVariable(0).getInitializer().get();
				map.put(variableName, value);
			}

		}

		return map;
	}

	/**
	 * 
	 */
	public void replaceWithConstants(ParseResult<CompilationUnit> parsedJavaCode, List<MethodCallExpr> nodesForReplace) {
		for (MethodCallExpr node : nodesForReplace) {
			for (Node arg : node.getArguments()) {
				handlerObjectConstant(parsedJavaCode, arg);
			}
		}
	}

	/**
	 * Обработка объектов
	 */
	private void handlerObjectConstant(ParseResult<CompilationUnit> parsedJavaCode, Node arg) {

		String classname = null;

		if (arg instanceof MethodCallExpr) {
			ObjectCreationExpr type = NodeHelper.getType((MethodCallExpr) arg);
			classname = type.getTypeAsString() + POST_PREFIX_CLASS;
		} else if (arg instanceof StringLiteralExpr) {
			classname = "String" + POST_PREFIX_CLASS;
		} else if (arg instanceof ObjectCreationExpr) {
			classname = ((ObjectCreationExpr) arg).getTypeAsString() + POST_PREFIX_CLASS;
		}

		Map<String, Node> classVariable = allVariables.get(classname);

		if (classVariable == null) {
			createClass(classname);
		}

		String constant = saveConstant(classname, parsedJavaCode, classVariable, arg);

		parsedJavaCode.getResult().get().addImport(Constant.CONSTANT_PACKAGE + classname);

		replaceWithConstant(classname, constant, arg);
	}

	/**
	 * Находит константу, если нет, то создаем ее
	 */
	private String saveConstant(String classNameString, ParseResult<CompilationUnit> parsedJavaCode,
			Map<String, Node> classVariable, Node str) {

		String variableName = findVariable(classVariable, str);

		if (variableName == null) {
			variableName = addConstantInClass(classNameString, parsedJavaCode, str);
		}

		uploadConstants();

		return variableName;
	}

	/**
	 * Добаление новой константы в Java файл
	 */
	// TODO подумать, как уменьшить
	private String addConstantInClass(String classNameString, ParseResult<CompilationUnit> parsedJavaCode, Node str) {

		File classJava = Paths.get(userDirectory, constantPath, classNameString + ".java").toFile();

		String variableName = null;

		try {
			ParseResult<CompilationUnit> result = parser.parse(classJava);

			ClassOrInterfaceDeclaration javaClass = null;

			if (str instanceof StringLiteralExpr) {

				javaClass = ((ClassOrInterfaceDeclaration) result.getResult().get().getChildNodes()
						.get(1));

				String strValue = ((StringLiteralExpr) str).getValue();

				if (strValue.matches("\\\\u.*")) {

					int index = allVariables.get(classNameString) != null ? allVariables.get(classNameString).size() : 0;

					variableName = "stringConstant_" + index;

				} else {
					variableName = strValue.replaceAll("\\.| |-|/", "_").replaceAll("[0-9]", "").toUpperCase();
				}

				Map<String, Node> vars = allVariables.get(classNameString);

				if (vars != null) {
					if (vars.get(variableName) != null) {
						variableName += allVariables.size() + new Random(999999).nextInt();
					}
				}

				FieldDeclaration field = javaClass.addField("String",
						variableName,
						Modifier.Keyword.PUBLIC,
						Modifier.Keyword.STATIC);

				field.getVariable(0).setInitializer((StringLiteralExpr) str.clone());

			} else if (str instanceof MethodCallExpr) {

				javaClass = (ClassOrInterfaceDeclaration) result.getResult().get().getTypes().getFirst()
						.get();

				int index = allVariables.get(classNameString) != null ? allVariables.get(classNameString).size() : 0;

				variableName = classNameString.toLowerCase() + "_" + index;

				Map<String, Node> vars = allVariables.get(classNameString);

				if (vars != null) {
					if (vars.get(variableName) != null) {
						variableName += allVariables.size() + new Random(999999).nextInt();
					}
				}

				FieldDeclaration field = javaClass.addField(
						NodeHelper.getType((MethodCallExpr) str)
								.getTypeAsString(),
						variableName,
						Modifier.Keyword.PUBLIC,
						Modifier.Keyword.STATIC);

				field.getVariable(0).setInitializer((MethodCallExpr) str.clone());

				List<Node> objs = NodeHelper.getObjectList(str);

				addImports(parsedJavaCode, result, objs);

			} else if (str instanceof ObjectCreationExpr) {
				javaClass = (ClassOrInterfaceDeclaration) result.getResult().get().getTypes().getFirst()
						.get();

				int index = allVariables.get(classNameString) != null ? allVariables.get(classNameString).size() + 1 : 0;

				variableName = classNameString.toLowerCase() + "_" + index;

				Map<String, Node> vars = allVariables.get(classNameString);

				if (vars != null) {
					if (vars.get(variableName) != null) {
						variableName += allVariables.size() + new Random(999999).nextInt();
					}
				}

				FieldDeclaration field = javaClass.addField(
						((ObjectCreationExpr) str)
								.getTypeAsString(),
						variableName,
						Modifier.Keyword.PUBLIC,
						Modifier.Keyword.STATIC);

				field.getVariable(0).setInitializer((ObjectCreationExpr) str.clone());

				List<Node> objs = NodeHelper.getObjectList(str);

				addImports(parsedJavaCode, result, objs);
			}

			try {
				FileUtils.write(classJava, result.getResult().get().toString(),
						"UTF-8",
						false);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		if (variableName == null) {
			throw new NullPointerException("Variable name is null. Classname " + classJava);
		}

		return variableName;
	}

	/**
	 * Добавляем импортируемые классы
	 */
	private void addImports(ParseResult<CompilationUnit> parsedJavaCode, ParseResult<CompilationUnit> result,
			List<Node> objs) {

		for (ImportDeclaration importDeclaration : parsedJavaCode.getResult().get().getImports()) {

			for (Node obj : objs) {
				if (obj instanceof ObjectCreationExpr) {
					if (importDeclaration.getNameAsString().contains(((ObjectCreationExpr) obj).getTypeAsString())) {
						ImportDeclaration importDecCopy = importDeclaration.clone();
						result.getResult().get().addImport(importDecCopy);
					}
				} else if (obj instanceof FieldAccessExpr) {
					Expression fieldExpr = ((FieldAccessExpr) obj).getScope();
					if (fieldExpr instanceof NameExpr) {
						String className = ((NameExpr) fieldExpr).getNameAsString();

						if (importDeclaration.getNameAsString().contains(className)) {
							ImportDeclaration importDecCopy = importDeclaration.clone();
							result.getResult().get().addImport(importDecCopy);
						}
					}	
				}
			}
		}

	}

	/**
	 * Строковую переменную заменяем на константу
	 */
	private void replaceWithConstant(String className, String variable, Node str) {

		Node parentNode = str.getParentNode().get();

		FieldAccessExpr field = new FieldAccessExpr(new NameExpr(new SimpleName(className)), variable);

		parentNode.replace(str, field);
	}

	/**
	 * В классе ищет переменную с похожим значением
	 */



	private String findVariable(Map<String, Node> classVariable, Node str) {

		if (classVariable == null) {
			return null;
		}

		for (String key : classVariable.keySet()) {
			Node valueNode = classVariable.get(key);
			if (valueNode instanceof StringLiteralExpr && str instanceof StringLiteralExpr) {
				if (((StringLiteralExpr) valueNode).getValue().toLowerCase()
						.equals(((StringLiteralExpr) str).getValue().toLowerCase())) {
					return key;
				}
			} else if (valueNode instanceof MethodCallExpr && str instanceof MethodCallExpr) {
				String valueNodeStr = ((MethodCallExpr) valueNode).getTokenRange().get().toString().trim()
						.replaceAll(template, "");
				String valueStr = ((MethodCallExpr) str).getTokenRange().get().toString().trim().replaceAll(template, "");
				if (valueNodeStr.equals(valueStr)) {
					return key;
				}
			} else if (valueNode instanceof ObjectCreationExpr && str instanceof ObjectCreationExpr) {
				String valueNodeStr = ((ObjectCreationExpr) valueNode).getTokenRange().get().toString().trim()
						.replaceAll(template, "");
				String valueStr = ((ObjectCreationExpr) str).getTokenRange().get().toString().trim().replaceAll(
						template,
						"");
				if (valueNodeStr.equals(valueStr)) {
					return key;
				}
			}

		}

		return null;
	}

}
