package bamboo.plugins.validate.specs.utils;

public class Constant {

	public static final String userDirectory = System.getProperty("user.dir");
	public static final String srcMain = "\\src\\main\\java\\";
	public static final String srcTest = "\\src\\test\\java\\";
	public static final String packagePath = srcMain + "bamboo\\specs\\projects\\";
	public static final String constantPath = srcMain + "bamboo\\specs\\constants\\";
	public static final String postfixConstant = "Constant";
	public static final String utils = srcMain + "\\bamboo\\specs\\utils";
	public static final String ext = ".java";
	public static final String javaPackagePath = "bamboo.specs.projects";
	public static final String CONSTANT_PACKAGE = "bamboo.specs.constants.";
	
	public static final String SRC_BASE_CLASS = "package %s;\r\n"
			+
			"\r\n" + 
			"import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;\r\n" + 
			"import com.atlassian.bamboo.specs.api.builders.permission.Permissions;\r\n" + 
			"import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;\r\n" + 
			"import com.atlassian.bamboo.specs.api.builders.plan.Plan;\r\n" + 
			"import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;\r\n" + 
			"import com.atlassian.bamboo.specs.util.BambooServer;\r\n" + 
			"\r\n" + 
			"abstract public class %s {\r\n"
			+
			"	\r\n" + 
			"	protected PlanPermissions planPermission() {\r\n" + 
			"		final PlanPermissions planPermission = new PlanPermissions(new PlanIdentifier(getProjectKey(),\r\n" + 
			"				getPlanKey()))\r\n" + 
			"				.permissions(new Permissions()\r\n" + 
			"						.groupPermissions(\"bamboo-admin\", PermissionType.ADMIN, PermissionType.BUILD, PermissionType.CLONE,\r\n" + 
			"								PermissionType.VIEW, PermissionType.EDIT)\r\n" + 
			"						.groupPermissions(\"ui-team\", PermissionType.CLONE, PermissionType.BUILD, PermissionType.VIEW,\r\n" + 
			"								PermissionType.EDIT)\r\n" + 
			"						.groupPermissions(\"orm-tomsk-team\", PermissionType.CLONE, PermissionType.BUILD, PermissionType.VIEW,\r\n" + 
			"								PermissionType.EDIT)\r\n" + 
			"						.groupPermissions(\"uadmin-team\", PermissionType.CLONE, PermissionType.BUILD, PermissionType.VIEW,\r\n" + 
			"								PermissionType.EDIT)\r\n" + 
			"						.groupPermissions(\"mt-team\", PermissionType.CLONE, PermissionType.BUILD, PermissionType.VIEW,\r\n" + 
			"								PermissionType.EDIT)\r\n" + 
			"						.groupPermissions(\"pdt-team\", PermissionType.CLONE, PermissionType.BUILD, PermissionType.VIEW,\r\n" + 
			"								PermissionType.EDIT)\r\n" + 
			"						.loggedInUserPermissions(PermissionType.VIEW)\r\n" + 
			"						.anonymousUserPermissionView());\r\n" + 
			"		return planPermission;\r\n" + 
			"	}\r\n" + 
			"\r\n" + 
			"	public abstract Plan plan();\r\n" + 
			"\r\n" + 
			"	public abstract String getProjectKey();\r\n" + 
			"\r\n" + 
			"	public abstract String getPlanKey();\r\n" + 
			"\r\n" + 
			"	public static void main(BambooSpecBaseClass spec) {\r\n" + 
			"		BambooServer bambooServer = new BambooServer(\"http://upr3.ftc.ru:8080/bamboo\");\r\n" + 
			"\r\n" + 
			"		final Plan plan = spec.plan();\r\n" + 
			"\r\n" + 
			"		if (plan == null) {\r\n" + 
			"			throw new NullPointerException();\r\n"
			+
			"		}\r\n" + 
			"\r\n" + 
			"		bambooServer.publish(plan);\r\n" + 
			"		final PlanPermissions planPermission = spec.planPermission();\r\n" + 
			"		bambooServer.publish(planPermission);\r\n" + 
			"	}\r\n" + 
			"\r\n" + 
			"}\r\n" + 
			"";
	
}
