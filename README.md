Описание
========================
Плагин предназначен для получения и перегенерации исходников планов Bamboo.

# Этап получения исходного кода проектов:
1. С помощью Rest API Bamboo выкачиваются исходники планов
2. Из полученных исходников удаляется строка package и методы oid(new BambooOid("...")) и сохраняются в указанной папке
3. Исходники планов парсятся м модифицируются
    3.1 Переменные выносятся в константы, если константа с определенным значением уже существует, то она будет использоваться в других классах, если нет, то создается.
    Список методов, из которых выносятся переменные в константы:
    - pluginConfigurations 
    - artifacts 
    - tasks 
    - finalTasks 
    - requirements 
    - linkedRepositories 
    - planRepositories 
    - variables 
    - planBranchManagement 
    - notifications 
    - labels
 3.2 добавляется package
    3.3 добавляется extends <указанный класс>
    3.4 удаляется метод planPermission, т.к. он должн бтыь в родительском классе
    3.5 создаются методы createPlanKey и createProjectKey и переменные в исходнике заменяются на вызов соответствующего метода
    3.6 в родительском классе должна быть основная реализация метода main, поэтому в исходнике плана main пересоздается
    и в качестве тела делается вызов родительского метода, в качестве аргумента передается объект класса, в котором запускается main
4. Исходный код после модификации сохраняется в указанный пакет

# Этап валидации

При запуске валидации сравниваются исходники планов, которые сохранены в качестве эталонов и искодный код, полученный через REST Api Bamboo.

# Этап публикации

При запуске создается отдельный проект publish-spec, в проект копируются константы, базовый класс, файл .credentials и указанные исходники планов.
После копирования в указанном проекте запускается стандартный профиль Bamboo для публикации.

```sh
mvn -Ppublish-specs
```

Для программного создания плагина использовалось API http://maven.apache.org/shared/maven-invoker/usage.html

Команда для создания плагина maven:

```sh
mvn archetype:generate\
    -DarchetypeGroupId=com.atlassian.bamboo -DarchetypeArtifactId=bamboo-specs-archetype \
    -DarchetypeVersion=7.2.3 \
    -DgroupId=com.atlassian.bamboo -DartifactId=bamboo-specs -Dversion=1.0.0-SNAPSHOT \
    -Dpackage=tutorial -Dtemplate=minimal
```




# Пример настройки профиля для получения исходного кода планов Bamboo и для валидации
```sh
 <properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <url>host</url>
  <resourcePath>resource</resourcePath>
  <token>api_token</token> 
  <constantPath>\src\main\java\bamboo\specs\constants\</constantPath>
  <baseClass>bamboo.specs.utils.BambooSpecBaseClass</baseClass>
  <overwriteEtalon>false</overwriteEtalon>
  <!-- Имя проекта задается так, как на Bamboo, пробелы не удаляются, например, Tests on OpenJDK 11 -->
  <PROJECT_NAME>PROJECT_NAME</PROJECT_NAME>
 </properties>
 
 <profile>
  <id>generate-spec</id>
   <build>
    <defaultGoal>validate.specs:refactoring-source-code-bamboo-plans
    </defaultGoal>
    <plugins>
     <plugin>
      <groupId>bamboo.plugins</groupId>
      <artifactId>validate.specs</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <configuration>
          <!-- URL для подключения к Bamboo --> 
       <url>${url}</url>
       <!-- Путь до эталонов указывается относительно проекта --> 
       <resourcePath>${resourcePath}</resourcePath>
       <!-- Для получения исходного кода планов требуется токен с правами администратора --> 
       <token>${token}</token>
       <!-- Родительский класс для всех спеков, в котором должны быть реализованы методы main и planPermission -->
       <baseClass>${baseClass}</baseClass>
       <!-- если true, то эталоны будут перезаписаны, иначе оставляем в исходном состоянии -->
       <overwriteEtalon>${overwriteEtalon}</overwriteEtalon>

       <!-- Указывается имя проекта (Project Name). Если в списке планов указать All, то плагин выгрузить все планы, игнорируя указанные -->
       <projects>
         <project>${PROJECT_NAME}</project>
         <project>ALL</project>
       </projects>
       
       <!-- Также список проектов можно прописать в файле, а путь до файла указать в соответствующем параметре -->
       <pathToProjects>projects.txt</pathToProjects>
     </configuration>
    </plugin>
   </plugins>
  </build>
 </profile>
    <profile>
   <id>validate-spec</id>
   <build>
    <defaultGoal>validate.specs:check-bamboo-plans
    </defaultGoal>
    <plugins>
     <plugin>
      <groupId>bamboo.plugins</groupId>
      <artifactId>validate.specs</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <configuration>
       <url>${url}</url>
       <resourcePath>${resourcePath}</resourcePath>
       <token>${token}</token>
       <constantPath>${constantPath}</constantPath>
       <overwriteEtalon>${overwriteEtalon}</overwriteEtalon>
       <projects>
         <!--Проверяет только проект, отдельный план не умеет. 
			 Если Build success, значит эталоны все совпадают, если Build failure,
             тогда рядом с эталонным файлом будет лежать класс с актуальным кодом, 
             имя класса формируется так {имя_эталонного_класса}_actual.java -->
        <project>${PROJECT_NAME}</project>
       </projects>
      </configuration>
     </plugin>
    </plugins>
   </build>
 </profile>
 <profile>
   <id>publish-bamboo-spec</id>
     <build>
       <defaultGoal>validate.specs:publish-bamboo-spec
       </defaultGoal>
       <plugins>
         <plugin>
          <groupId>bamboo.plugins</groupId>
          <artifactId>validate.specs</artifactId>
          <version>${version}</version>
          <configuration>
            <!--Путь до временного проекта, в данном случае указан родительской каталог --> 
            <pathToTemporaryProject>..</pathToTemporaryProject>
            <!--Путь до файла .credentials, который содержит данные для авторизации при публикации --> 
            <credentials>.credentials</credentials>
            <!-- Путь до базового класса, указывается относительно корневой папки --> 
            <baseClassFile>src\main\java\bamboo\specs\utils\BambooSpecBaseClass.java</baseClassFile>
            <!-- Если true, то при публикации будет использован код эталонных файлов, путь до которого указан в переменной resourcePath в профиле refactoring-source-code-bamboo-plans --> 
            <!-- По умолчанию false --> 
            <useSavedSources>true</useSavedSources>
            <!-- Список проектов, исходники которых будут скопированы во временный проект -->
              <projects> -->
                <project>${PROJECT_NAME}</project>
              </projects>
              <!-- Список планов, исходники которых будут скопированы во временный проект, задаются по такому шаблону {PROJECT_NAME}-{PLAN_NAME} -->
              <plans> 
              <!-- Поскольку некоторые имена для планов были изменены из-за наличия
              цифры в качестве первого символа, то в данном случае в качестве имени
              плана выступает имя соответствующего java-класса. -->
                <plan>
                  ${PROJECT_NAME}-PLAN_NAME
                </plan>
              </plans>
              <!-- Удалять или не удалять временный проект. Расположен на том же уровне, что и текущий проект -->
            <deleteTemporaryProject>true</deleteTemporaryProject>
          </configuration>
        </plugin>
       </plugins>
     </build>
 </profile>
 
```
