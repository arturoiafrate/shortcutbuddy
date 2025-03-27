package it.arturoiafrate.shortcutbuddy.model.manager;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public abstract class AbstractManager {

    private void createDefaultFile(File shortcutsFile, String filePath) {
        try {
            FileUtils.copyURLToFile(Objects.requireNonNull(this.getClass().getResource(filePath)), shortcutsFile);
        } catch (IOException e) {
            System.out.println("Error while creating default file "+ filePath);
            throw new RuntimeException(e);
        }
    }

    private String getFilePath(String fileName) throws IOException {
        String userHome = System.getProperty("user.home");
        Path myAppDir = Paths.get(userHome, ".shortcutbuddy");
        if (!Files.exists(myAppDir)) {
            Files.createDirectory(myAppDir);
        }
        return Paths.get(myAppDir.toString(), fileName).toString();
    }

    protected <T> T loadFromFile(String fileName, Type type) {
        try {
            File file = new File(getFilePath(fileName));
            if (!file.exists()) {
                createDefaultFile(file, "/default/" + fileName);
            }
            String json = FileUtils.readFileToString(file, "UTF-8");
            return new Gson().fromJson(json, type);
        } catch (IOException e) {
            System.out.println("Error while loading file " + fileName);
            throw new RuntimeException(e);
        }
    }


}
