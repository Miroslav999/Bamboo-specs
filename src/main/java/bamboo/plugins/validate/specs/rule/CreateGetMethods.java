package bamboo.plugins.validate.specs.rule;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.utils.NodeHelper;

public class CreateGetMethods implements RefactoringRule {

	@Override
	public void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode) {

		createGetProjectMethod(parsedJavaCode, "getProjectKey", plan.getProject().getProjectKey());

		createGetProjectMethod(parsedJavaCode, "getPlanKey", plan.getBuildKey());
	}

	private void createGetProjectMethod(ParseResult<CompilationUnit> parsedJavaCode,
			String name,
			String returnValue) {

		ClassOrInterfaceDeclaration classNode = NodeHelper.getClassNode(parsedJavaCode);

		MethodDeclaration getProj = classNode.addMethod(name, Modifier.Keyword.PUBLIC);

		BlockStmt stmt = new BlockStmt();

		ReturnStmt expr = new ReturnStmt();
		expr.setExpression(new StringLiteralExpr(returnValue));

		stmt.addStatement(expr);
		getProj.setBody(stmt);

		AnnotationExpr ann = new MarkerAnnotationExpr();
		ann.setName("Override");
		NodeList<AnnotationExpr> annList = new NodeList<AnnotationExpr>();
		annList.add(ann);
		getProj.setAnnotations(annList);

		getProj.setType(String.class);
	}
}
