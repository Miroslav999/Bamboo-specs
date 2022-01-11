package bamboo.plugins.validate.specs.rule;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.utils.NodeHelper;

public class CreateMainMethod implements RefactoringRule {

	@Override
	public void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode) {

		ClassOrInterfaceDeclaration classNode = NodeHelper.getClassNode(parsedJavaCode);

		MethodDeclaration main = classNode.addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);

		NodeList<Parameter> parameters = new NodeList<Parameter>();
		Parameter param = new Parameter();
		param.setName("argv");
		param.setType("String");
		param.setVarArgs(true);

		parameters.add(param);
		main.setParameters(parameters);

		BlockStmt stmt = new BlockStmt();

		ExpressionStmt expr = new ExpressionStmt();
		expr.setExpression("BambooSpecBaseClass.main(new " + classNode.getNameAsString() + "())");

		stmt.addStatement(expr);
		main.setBody(stmt);
	}

}
