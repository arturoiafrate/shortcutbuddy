package it.arturoiafrate.shortcutbuddy.model.manager;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractManager {

    private void createDefaultFile(File shortcutsFile, String filePath) {
        try {
            FileUtils.copyURLToFile(Objects.requireNonNull(this.getClass().getResource(filePath)), shortcutsFile);
        } catch (IOException e) {
            log.error("Error while creating default file {}", filePath, e);
            throw new RuntimeException(e);
        }
    }

    protected static String getFilePath(String fileName) throws IOException {
        String userHome = System.getProperty("user.home");
        Path myAppDir = Paths.get(userHome, ".shortcutbuddy");
        if (!Files.exists(myAppDir)) {
            Files.createDirectory(myAppDir);
        }
        return Paths.get(myAppDir.toString(), fileName).toString();
    }

    protected <T> T loadIfFileExists(String fileName, Type type){
        try {
            File file = new File(getFilePath(fileName));
            if (!file.exists()) {
                return null;
            }
            String json = FileUtils.readFileToString(file, "UTF-8");
            return new Gson().fromJson(json, type);
        } catch (IOException e) {
            log.error("Error while loading file {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    protected <T> T loadFromFile(String fileName, Type type, boolean getfromResource) {
        log.info("Loading file {} from resources: {}", fileName, getfromResource);
        File file;
        try {
            if(getfromResource) {
                URL resource = this.getClass().getResource(fileName);
                if (resource == null) {
                    log.error("Resource not found: {}", fileName);
                    throw new RuntimeException("Resource not found: " + fileName);
                }
                if("jar".equals(resource.getProtocol())){
                    try (InputStream inputStream = this.getClass().getResourceAsStream(fileName)) {
                        if (inputStream == null) {
                            log.error("Resource not found in JAR: {}", fileName);
                            throw new RuntimeException("Resource not found in JAR: " + fileName);
                        }
                        file = File.createTempFile("temp", null);
                        file.deleteOnExit();
                        try (OutputStream outputStream = new FileOutputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                } else {
                    file = new File(resource.toURI());
                }
            } else {
                file = new File(getFilePath(fileName));
            }
            if (!file.exists() && !getfromResource) {
                createDefaultFile(file, "/default/" + fileName);
            }
            String json = FileUtils.readFileToString(file, "UTF-8");
            try{
                copyImages();
            } catch (Exception e){
                log.error("Error while copying images", e);
            }
            return new Gson().fromJson(json, type);
        } catch (IOException | URISyntaxException e) {
            log.error("Error while loading file {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    public String getAppImagePath(String appName) {
        String userHome = System.getProperty("user.home");
        Path myAppDir = Paths.get(userHome, ".shortcutbuddy/appimages");
        return Paths.get(myAppDir.toString(), appName+".png").toString();
    }

    private void copyImages() throws IOException, URISyntaxException {
        String resourceDir = "/images/apps";
        String userHome = System.getProperty("user.home");
        Path destinationDir = Paths.get(userHome, ".shortcutbuddy", "appimages");

        log.info("Starting files copy from ({}) to {}", resourceDir, destinationDir);
        if (!Files.exists(destinationDir)) {
            try {
                Files.createDirectories(destinationDir);
                log.info("Target folder created: {}", destinationDir);
            } catch (IOException e) {
                log.error("Error while creating target directory {}", destinationDir, e);
                throw e;
            }
        }

        URI uri = getClass().getResource(resourceDir).toURI();
        log.debug("URI resource: {}", uri);

        Path sourcePathBase = null;
        FileSystem jarFileSystem = null;
        try {
            if ("jar".equals(uri.getScheme())) {
                try {
                    jarFileSystem = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
                sourcePathBase = jarFileSystem.getPath(resourceDir);
            } else {
                sourcePathBase = Paths.get(uri);
            }
            if (sourcePathBase == null || !Files.exists(sourcePathBase)) {
                log.error("Cannot resolve or find resource path: {}", resourceDir);
                return;
            }
            log.info("Copying files from {}...", sourcePathBase);
            try (Stream<Path> stream = Files.walk(sourcePathBase, 1)) {
                Path finalSourcePathBase = sourcePathBase;
                stream.filter(path -> !path.equals(finalSourcePathBase))
                        .filter(Files::isRegularFile)
                        .forEach(sourceFile -> {
                            try {
                                Path fileName = sourceFile.getFileName();
                                Path destinationFile = destinationDir.resolve(fileName.toString());
                                log.debug("Copy {} -> {}", sourceFile, destinationFile);
                                Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                log.error("Cannot copy {}", sourceFile, e);
                            }
                        });
            }
            log.info("Image copying completed.");

        } finally {
            if (jarFileSystem != null && jarFileSystem != FileSystems.getDefault() && jarFileSystem.isOpen()) {
                try {
                    jarFileSystem.close();
                } catch (IOException e) {
                    log.warn("Error closing jar file system", e);
                }
            }
        }
    }
}
