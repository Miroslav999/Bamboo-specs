package bamboo.plugins.validate.specs.rule;

import static bamboo.plugins.validate.specs.utils.NodeHelper.getReplacementList;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import bamboo.plugins.validate.specs.network.BambooPlan;
import bamboo.plugins.validate.specs.utils.ConstantHelper;

public class ReplaceWithConstantRule implements RefactoringRule {

	private ConstantHelper constantHelper;
	private List<String> attribute;

	public ReplaceWithConstantRule() {
		attribute = new ArrayList<>();
		constantHelper = new ConstantHelper();
		attribute.add("pluginConfigurations");
		attribute.add("artifacts");
		attribute.add("tasks");
		attribute.add("finalTasks");
		attribute.add("requirements");
		attribute.add("linkedRepositories");
		attribute.add("planRepositories");
		attribute.add("variables");
		attribute.add("planBranchManagement");
		attribute.add("notifications");
		attribute.add("labels");
	}

	@Override
	public void refactoring(BambooPlan plan, ParseResult<CompilationUnit> parsedJavaCode) {

		List<MethodCallExpr> nodesForReplace = getReplacementList(parsedJavaCode.getResult().get().getChildNodes(),
				attribute);

		constantHelper.replaceWithConstants(parsedJavaCode, nodesForReplace);
	}

}
