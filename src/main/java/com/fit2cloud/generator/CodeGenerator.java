package com.fit2cloud.generator;

import com.sun.tools.javac.comp.Lower;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Properties;

public class CodeGenerator {

    private static final String DEFAULT_PROJECT_NAME = "fit2cloud2.0-demo";
    private static final String DEFAULT_GROUP_ID = "com.fit2cloud";
    private static final String DEFAULT_VERSION = "2.0.0";

    private String projectName;
    private String projectPath;
    private String groupId;
    private String version;
    private String packagePath;

    private static GroupTemplate gt;
    private static final String TEMPLATE_DIR = "template";
    private String TEMPLATE_RESOURCE = this.getClass().getClassLoader().getResource(TEMPLATE_DIR).getPath();

    static {
        ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader(
                TEMPLATE_DIR);
        Configuration cfg;
        try {
            cfg = Configuration.defaultConfiguration();
            cfg.setPlaceholderStart("#{");
            cfg.setPlaceholderEnd("}");
            gt = new GroupTemplate(resourceLoader, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    CodeGenerator() {
        Properties properties = readConfig();
        this.projectName = properties.getProperty("projectName", DEFAULT_PROJECT_NAME);
        this.groupId = properties.getProperty("groupId", DEFAULT_GROUP_ID);
        this.version = properties.getProperty("version", DEFAULT_VERSION);
        this.projectPath = properties.getProperty("projectPath",
                System.getProperty("user.home"));
        this.packagePath = properties.getProperty("packagePath", "com.fit2cloud." + capturePackageName(projectName));
        System.out.println("init config param success");
    }

    void build() {
        System.out.println("begin code generate...");
        try {
            createProject();
        } catch (Exception e) {
            throw new RuntimeException("code build error", e);
        }
        System.out.println("code generate success");
    }

    private File createProject() throws Exception {
        File rootDir = new File(this.projectPath + File.separator + projectName);
        rootDir.mkdir();
        generate(TEMPLATE_RESOURCE);
        return rootDir;
    }

    private void generate(String templatePath) throws Exception {
        File file = new File(templatePath);
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                if (listFile.isDirectory()) {
                    if (listFile.getName().equals("com")) {
                        this.generateDirectory(capturePackage(listFile.getAbsolutePath().replace(TEMPLATE_RESOURCE, "")));
                        this.generate(templatePath + "/com/fit2cloud/demo");
                    } else {
                        this.generateDirectory(capturePath(listFile.getAbsolutePath().replace(TEMPLATE_RESOURCE, "")));
                        this.generate(templatePath + "/" + listFile.getName());
                    }
                } else {
                    this.generateFile(listFile.getAbsolutePath().replace(TEMPLATE_RESOURCE, ""));
                }
            }
        }
    }

    private String capturePackage(String path) {
        return path.substring(0, path.length() - 3) + this.packagePath.replace(".", "/");
    }

    private String capturePath(String path) {
        return path.replace("com/fit2cloud/demo", this.packagePath.replace(".", "/"));
    }

    private void generateDirectory(String templateDir) {
        File targetFile = new File(this.projectPath + File.separator + projectName + templateDir);
        targetFile.mkdirs();
        System.out.println("generate directory: " + targetFile.getAbsolutePath());
    }

    private void generateFile(String templateDir) throws Exception {
        File targetFile = new File(this.projectPath + File.separator + projectName + capturePath(templateDir));
        OutputStream os = new FileOutputStream(targetFile);
        Template t = gt.getTemplate(templateDir);
        t.binding("projectName", this.projectName);
        t.binding("groupId", this.groupId);
        t.binding("version", this.version);
        t.binding("packagePath", this.packagePath);
        t.renderTo(os);
        os.close();

        System.out.println("generate file: " + targetFile.getAbsolutePath());
    }

    private String capturePackageName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new RuntimeException("project name is empty");
        }
        if (name.contains("-")) {
            String[] strings = name.split("-");
            name = strings[strings.length - 1];
        }
        return name.toLowerCase();
    }

    private Properties readConfig() {
        System.out.println("read code generate config file ...");
        Properties properties = new Properties();
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties");
            properties.load(inputStream);
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("read config file error", e);
        }
        return properties;
    }
}


