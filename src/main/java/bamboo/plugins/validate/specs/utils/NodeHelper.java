package bamboo.plugins.validate.specs.utils;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

public class NodeHelper {


	private NodeHelper() {
	}

	public static ClassOrInterfaceDeclaration getClassNode(ParseResult<CompilationUnit> java) {

		if (java.getResult().isPresent()) {
			for (Node node : java.getResult().get().getChildNodes()) {
				if (node instanceof ClassOrInterfaceDeclaration) {
					return (ClassOrInterfaceDeclaration) node;
				}
			}
		} else {
			throw new NullPointerException("Not present");
		}
		throw new NullPointerException("Node ClassOrInterfaceDeclaration not found");
	}

	/** 
	 * Поиск метода с определенным названием
	 * */
	public static Node searchMethod(Node planNode, String name) {
		Node searchedNode = null;
		for (Node node : planNode.getChildNodes()) {

			if (node instanceof MethodDeclaration && ((MethodDeclaration) node).getName().toString().equals(name)) {
				searchedNode = node;
				break;
			}

			if (!node.getChildNodes().isEmpty()) {
				searchedNode = searchMethod(node, name);
				if (searchedNode != null) {
					break;
				}
			}
		}

		return searchedNode;
	}

	/**
	 * Получаем список узлов, имена которых содержатся в списке attribute
	 */
	public static List<MethodCallExpr> getReplacementList(List<Node> nodes, List<String> attribute) {
		List<MethodCallExpr> list = new ArrayList<>();
		for (Node node : nodes) {

			if (node instanceof MethodCallExpr &&
					attribute.contains(((MethodCallExpr) node).getNameAsString())) {
				list.add((MethodCallExpr) node);
			}

			if (!node.getChildNodes().isEmpty()) {
				list.addAll(getReplacementList(node.getChildNodes(), attribute));
			}
		}

		return list;
	}

	/**
	 * Собирает все объекты с типом ObjectCreationExpr, включая из дочерних объектов
	 */
	public static List<Node> getObjectList(Node exp) {

		List<Node> list = new ArrayList<>();

		if (exp instanceof ObjectCreationExpr) {
			list.add((ObjectCreationExpr) exp);
		}

		for (Node node : exp.getChildNodes()) {

			if (node instanceof MethodCallExpr || node instanceof ObjectCreationExpr) {
				list.addAll(getObjectList(node));
			}

			if (node instanceof ObjectCreationExpr) {
				list.add((ObjectCreationExpr) node);
			} else if (node instanceof FieldAccessExpr) {
				list.add((FieldAccessExpr) node);
			}
		}

		return list;
	}

	
	/**
	 * В MethodCallExpr ищется первый попавшийся объект ObjectCreationExpr
	 */
	public static ObjectCreationExpr getType(MethodCallExpr arg) {

		ObjectCreationExpr type = null;

		if (arg.getScope().get() instanceof MethodCallExpr) {
			type = getType((MethodCallExpr) arg.getScope().get());
		} else if (arg.getScope().get() instanceof ObjectCreationExpr) {
			return (ObjectCreationExpr) arg.getScope().get();
		}

		return type;
	}

	/**
	 * Удаление метода по имени
	 */
	public static void removeMethod(ParseResult<CompilationUnit> parsedJavaCode, String methodName) {
		Node classNode = NodeHelper.getClassNode(parsedJavaCode);
		Node method = NodeHelper.searchMethod(classNode, methodName);
		if (method != null) {
			method.removeForced();
		}
	}
}
