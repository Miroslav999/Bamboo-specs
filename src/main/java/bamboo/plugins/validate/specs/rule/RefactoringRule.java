package bamboo.plugins.validate.specs.rule;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import bamboo.plugins.validate.specs.network.BambooPlan;

public interface RefactoringRule {
	void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode);
}
