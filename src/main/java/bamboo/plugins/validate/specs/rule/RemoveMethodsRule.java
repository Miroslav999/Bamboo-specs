package bamboo.plugins.validate.specs.rule;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.utils.NodeHelper;

public class RemoveMethodsRule implements RefactoringRule {

	@Override
	public void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode) {

		NodeHelper.removeMethod(parsedJavaCode, "main");

		NodeHelper.removeMethod(parsedJavaCode, "planPermission");

	}

}
