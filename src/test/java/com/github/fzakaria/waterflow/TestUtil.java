package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.JsonUnmarshallerContextImpl;
import com.amazonaws.transform.Unmarshaller;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.github.fzakaria.waterflow.event.Event;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TestUtil {


    /**
     * Read a file on the classpath into a string.
     *
     * @param fileName name of file
     *
     * @return file contents as a string
     */
    public static String readFile(String fileName) {
        try {
            URL resource = TestUtil.class.getClassLoader().getResource(fileName);
            if (resource == null) {
                throw new FileNotFoundException(format("Could not find the file on classpath: %s", fileName));
            }
            return Resources.toString(resource, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(format("Error reading file \"%s\"", fileName), e);
        }
    }


    /**
     * Parse workflow history from a json-formatted string into a list of {@link Event} sorted in descending event id order.
     * <p/>
     * Note: json format is same as native format used by Amazon SWF responses.
     *
     * @param json json to parse
     */
    public static List<Event> parseActionEvents(String json) {
        List<HistoryEvent> historyEvents = Lists.newArrayList();
        historyEvents.addAll(unmarshalDecisionTask(json).getEvents());
        return historyEvents.stream().map(he -> new Event(he, historyEvents)).sorted().collect(Collectors.toList());
    }

    /**
     * Load workflow history converted to a list of {@link Event} sorted in descending event id order
     * from a json-formatted data file.
     *
     * @param fileName name of the json data file.
     *
     * @return list of history events
     */
    public static List<Event> loadActionEvents(String fileName) {
        return parseActionEvents(readFile(fileName));
    }

    /**
     * Use SWF API to unmarshal a json document into a {@link DecisionTask}.
     * Note: json is expected to be in the native format used by SWF
     */
    public static DecisionTask unmarshalDecisionTask(String json) {
        try {
            Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller();
            JsonParser parser = new JsonFactory().createParser(json);
            return unmarshaller.unmarshall(new JsonUnmarshallerContextImpl(parser));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
