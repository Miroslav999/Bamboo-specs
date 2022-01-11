package bamboo.plugins.validate.specs.rule;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import bamboo.plugins.validate.specs.network.BambooPlan;

public class AddPackageDeclarationRule implements RefactoringRule {

	@Override
	public void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode) {
		parsedJavaCode.getResult().get().setPackageDeclaration(plan.getPackageName());
	}

}
