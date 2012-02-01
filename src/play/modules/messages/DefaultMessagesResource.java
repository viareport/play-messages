package play.modules.messages;

import play.Logger;
import play.Play;
import play.libs.IO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Default messages resource uses the Play messages files.
 *
 * @author huljas
 */
public class DefaultMessagesResource extends MessagesResource {

    File targetDir;
    File targetControllerDir;

    public DefaultMessagesResource() {
        this.targetDir = new File(Play.configuration.getProperty("messages.targetDir", "conf"));
        this.targetControllerDir = new File(Play.configuration.getProperty("messages.targetControllerDir", "conf/Messages"));
    }

    @Override
    public List<String> loadKeepList() {
        try {
            File file = new File(this.targetDir, "messages.keep");
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            List<String> result = IOUtils.readLines(in, "UTF-8");
            IOUtils.closeQuietly(in);
            return result;
        } catch (FileNotFoundException e) {
            return new ArrayList<String>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> loadIgnoreList() {
        try {
            File file = new File(this.targetDir, "messages.ignore");
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            List<String> result = IOUtils.readLines(in, "UTF-8");
            IOUtils.closeQuietly(in);
            return result;
        } catch (FileNotFoundException e) {
            return new ArrayList<String>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> loadMessages(String language, String controller) {
        try {
            File file = this.getMessagesFile(language, controller);
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            Properties properties = IO.readUtf8Properties(in);
            IOUtils.closeQuietly(in);
            return new HashMap(properties);
        } catch (FileNotFoundException e) {
            Logger.warn("Message file not found: " + controller + " Language : " + language);
            return new HashMap<String,String>();
        }
    }

    @Override
    public void save(String language, String controller, String key, String value) {
        Map<String,String> messages = this.loadMessages(language, controller);
        messages.put(key, value);
        this.saveMessages(language, controller, messages);
    }

    @Override
    public void keep(String key) {
        List<String> keepList = this.loadKeepList();
        if (!keepList.contains(key)) {
            keepList.add(key);
            this.saveKeepList(keepList);
        }
    }

    @Override
    public void removeKeep(String key) {
        List<String> keepList = this.loadKeepList();
        if (keepList.contains(key)) {
            keepList.remove(key);
            this.saveKeepList(keepList);
        }
    }

    @Override
    public void removeAll(String language, String controller, List<String> keys) {
        Map<String, String> messages = this.loadMessages(language, controller);
        messages.keySet().removeAll(keys);
        this.saveMessages(language, controller, messages);
    }

    @Override
    public void ignoreAll(List<String> keys) {
        List<String> ignoreList = this.loadIgnoreList();
        ignoreList.removeAll(keys);
        ignoreList.addAll(keys);
        this.saveIgnoreList(ignoreList);
    }

    @Override
    public void unignoreAll(List<String> keys) {
        List<String> ignoreList = this.loadIgnoreList();
        ignoreList.removeAll(keys);
        this.saveIgnoreList(ignoreList);
    }

    protected void saveKeepList(List<String> list) {
        File file = new File(this.targetDir, "messages.keep");
        try {
            Collections.sort(list);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            IOUtils.writeLines(list, null, out, "UTF-8");
            IOUtils.closeQuietly(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void saveIgnoreList(List<String> list) {
        File file = new File(this.targetDir, "messages.ignore");
        try {
            Collections.sort(list);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            IOUtils.writeLines(list, null, out, "UTF-8");
            IOUtils.closeQuietly(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void saveMessages(String language, String controller, Map<String, String> messages) {
        try {
            File file = this.getMessagesFile(language, controller);
            Properties properties = new Properties();
            properties.putAll(messages);
            // This is ugly but the properties string formatting is so weird that I don't want to
            // start messing around with it.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer stringWriter = new OutputStreamWriter(baos, "UTF-8");
            properties.store(stringWriter, "");
            IOUtils.closeQuietly(stringWriter);
            InputStreamReader lineReader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8");
            String propertiesAsString = IOUtils.toString(lineReader);
            String[] lines = StringUtils.split(propertiesAsString, "\n");
            List<String> list = new ArrayList<String>();
            for (String line : lines) {
                if (line.trim().length() > 0) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    list.add(line);
                }
            }
            Collections.sort(list);
            BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            String content = StringUtils.join(list, "\n");
            content = new StringBuilder("# Saved by @messages on ").append(new Date()).append("\n").append(content).toString();
            IOUtils.write(content, fileWriter);
            IOUtils.closeQuietly(fileWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getMessagesFile(String language, String controller) {
        String filename = "";
        String directory = "";
        if (language.equals(DefaultMessagesResource.DEFAULT_LANGUAGE)) {
            filename = "messages";
            directory = this.targetDir.getPath();
        } else if(controller.equals(DefaultMessagesResource.DEFAULT_CONTROLLER)){
            filename = "messages." + language;
            directory = this.targetDir.getPath();
        } else {
            filename = "messages." + controller + "." + language;
            directory = this.targetControllerDir.getPath();
        }
        Logger.trace("File name %s", filename);
        return new File(directory, filename);
    }
}
