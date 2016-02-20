package com.github.fzakaria.waterflow.swf;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.google.common.collect.Lists;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.github.fzakaria.waterflow.TestUtil.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

@RunWith(JMockit.class)
public class DecisionTaskIteratorTest {

    @Test
    public void basicTest() {
        DecisionTask decisionTask = unmarshalDecisionTask(readFile("fixtures/simple_workflow.json"));
        DecisionTaskIterator decisionTaskIterator = new DecisionTaskIterator(null, null, decisionTask);
        List<HistoryEvent> events = Lists.newArrayList(decisionTaskIterator);
        assertThat(events, is(decisionTask.getEvents()));
    }

    @Test
    public void multiplePages(@Mocked AmazonSimpleWorkflow swf) {
        PollForDecisionTaskRequest initialRequest = new PollForDecisionTaskRequest();
        List<HistoryEvent> events = loadHistoryEvents("fixtures/simple_workflow.json");
        int numberOfPages = 4;
        int sizePerPage = (int) Math.ceil( (double) events.size() / numberOfPages);
        List<List<HistoryEvent>> pages = Lists.partition(events, sizePerPage);
        assertThat("partitioned incorrectly", pages, hasSize(numberOfPages));
        DecisionTask initialDecisionTask = new DecisionTask().withTaskToken("xyz")
                .withNextPageToken("1").withEvents(pages.get(0));
        DecisionTaskIterator decisionTaskIterator = new DecisionTaskIterator(swf, initialRequest, initialDecisionTask);
        new Expectations() {{
            swf.pollForDecisionTask(initialRequest);times=(numberOfPages-1);
            returns(new DecisionTask().withTaskToken("abc").withNextPageToken("2").withEvents(pages.get(1)),
                    new DecisionTask().withTaskToken("def").withNextPageToken("3").withEvents(pages.get(2)),
                    new DecisionTask().withTaskToken("ghi").withEvents(pages.get(3)));
        }};

        assertThat(Lists.newArrayList(decisionTaskIterator), is(events));
    }
}
