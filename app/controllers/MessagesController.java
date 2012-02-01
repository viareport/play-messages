package controllers;

import play.Play;
import play.modules.messages.MessagesResource;
import play.modules.messages.MessagesUtil;
import play.modules.messages.SourceKeys;
import play.mvc.Before;
import play.mvc.Controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author huljas
 */
public class MessagesController extends Controller {

    @Before
    public static void disableInProduction() {
        if (Play.mode == Play.Mode.PROD) {
            error(404, "Page not found");
        }
    }

    public static void index(String language, String defaultLanguage, String controller) {
    	String[] controllerClasses = MessagesUtil.getApplicationControllers();
    	
    	if (StringUtils.isBlank(defaultLanguage)) {
            if (Play.langs.size() == 0) {
                error(500, "ERROR: Required application.langs property is not set!");
            }
            defaultLanguage = MessagesResource.DEFAULT_LANGUAGE;
        }
        if (StringUtils.isBlank(language)) {
            language = MessagesResource.DEFAULT_LANGUAGE;
        }
        
        if (StringUtils.isBlank(controller)) {
            controller = MessagesResource.DEFAULT_CONTROLLER;
        }
        
        	      
        MessagesResource messagesResource = MessagesResource.instance();
        
        Map<String,String> values = messagesResource.loadMessages(language, controller);
        Map<String,String> defaultValues = messagesResource.loadMessages(defaultLanguage, controller);
        if(controller.equalsIgnoreCase(MessagesResource.DEFAULT_CONTROLLER)) {
        	for(String controllerName : controllerClasses) {
        		values.putAll(messagesResource.loadMessages(language, controllerName.toLowerCase()));
        		defaultValues.putAll(messagesResource.loadMessages(defaultLanguage, controllerName.toLowerCase()));
        	}
        }
        
        List<String> keepList = messagesResource.loadKeepList();
        List<String> ignoreList = messagesResource.loadIgnoreList();

        SourceKeys sources = SourceKeys.lookUp(controller);

        Collection<String> newKeys = MessagesUtil.getNewKeys(sources, values);
        Collection<String> obsoleteKeys = MessagesUtil.getObsoleteKeys(sources, values);
        Collection<String> existingKeys = MessagesUtil.getExistingKeys(sources, values);
        
        if(controller.equalsIgnoreCase(MessagesResource.DEFAULT_CONTROLLER)) {
        	existingKeys.addAll(keepList);
        } else {
        	newKeys.removeAll(messagesResource.loadMessages(language, MessagesResource.DEFAULT_CONTROLLER).keySet());
        }
        obsoleteKeys.removeAll(keepList);
        newKeys.removeAll(ignoreList);

        List<String> langs = new ArrayList<String>(Play.langs);
        langs.add("default");

        render(language, defaultLanguage, controller, values, defaultValues, sources, newKeys, existingKeys, obsoleteKeys, keepList, ignoreList, langs, controllerClasses);
    }

    public static void save(String language, String controller, String key, String value, boolean keep) {
        if (!StringUtils.isBlank(value) && !StringUtils.isBlank(key)) {
            MessagesResource messagesResource = MessagesResource.instance();
            messagesResource.save(language, controller, key, value);
            if (keep) {
                messagesResource.keep(key);
            } else {
                messagesResource.removeKeep(key);
            }
        }
        render(value);
    }

    public static void applyChanges(String language, String defaultLanguage, String controller, MessagesAction action, List<String> keys) {
        if (action == MessagesAction.REMOVE) {
            MessagesResource messagesResource = MessagesResource.instance();
            messagesResource.removeAll(language, controller, keys);
        } else if (action == MessagesAction.IGNORE) {
            MessagesResource messagesResource = MessagesResource.instance();
            messagesResource.ignoreAll(keys);
        } else if (action == MessagesAction.UNIGNORE) {
            MessagesResource messagesResource = MessagesResource.instance();
            messagesResource.unignoreAll(keys);
        }
        index(language, defaultLanguage, controller);
    }

    public static void addKey(String language, String defaultLanguage, String controller, String key) {
        MessagesResource messagesResource = MessagesResource.instance();
        Map<String,String> values = messagesResource.loadMessages(language, controller);
        Map<String,String> defaultValues = messagesResource.loadMessages(defaultLanguage, controller);
        List<String> keepList = new ArrayList<String>();
        keepList.add(key);
        render("_row.html", language, defaultLanguage, controller, values, defaultValues, key, keepList);
    }

    public static void sources(String key) {
        SourceKeys sources = SourceKeys.lookUp(null);
        render(key, sources);
    }

    private static List<String> removeDuplicates(List<String> list) {
        if (list == null) {
            return Collections.EMPTY_LIST;
        }
        return new ArrayList<String>(new HashSet<String>(list));
    }
}
