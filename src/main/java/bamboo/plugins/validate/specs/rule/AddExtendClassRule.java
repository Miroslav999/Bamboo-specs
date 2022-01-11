package bamboo.plugins.validate.specs.rule;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.utils.NodeHelper;

public class AddExtendClassRule implements RefactoringRule {

	private String baseClass;

	public AddExtendClassRule(String baseClass) {
		this.baseClass = baseClass;
	}

	@Override
	public void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode) {

		parsedJavaCode.getResult().get().addImport(baseClass);

		ClassOrInterfaceDeclaration classNode = NodeHelper.getClassNode(parsedJavaCode);

		classNode.addExtendedType(baseClass);
	}

}
