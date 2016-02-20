package com.github.fzakaria.waterflow.swf;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.google.common.collect.AbstractIterator;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class DecisionTaskIterator extends AbstractIterator<HistoryEvent> {

    private String nextPageToken;
    private Iterator<HistoryEvent> currentPage = Collections.emptyIterator();
    private boolean isLastPage = false;
    private final AmazonSimpleWorkflow swf;
    private final PollForDecisionTaskRequest request;

    public DecisionTaskIterator(AmazonSimpleWorkflow swf, PollForDecisionTaskRequest request,
                                DecisionTask startingResponse) {
        this.swf = swf;
        this.request = request;
        this.nextPageToken = startingResponse.getNextPageToken();
        this.currentPage = Optional.ofNullable(startingResponse.getEvents())
                .map(e -> e.iterator()).orElse( Collections.emptyIterator());
        this.isLastPage = this.nextPageToken == null;
    }

    @Override
    protected HistoryEvent computeNext() {
        if (!currentPage.hasNext() && !isLastPage) {
            DecisionTask decisionTask = nextDecisionTask(nextPageToken);
            nextPageToken = decisionTask.getNextPageToken();
            isLastPage = this.nextPageToken == null;
            currentPage = Optional.ofNullable(decisionTask.getEvents())
                    .map(e -> e.iterator()).orElse(Collections.emptyIterator());
        }

        if (currentPage.hasNext()) {
            return currentPage.next();
        }else {
            endOfData();
            return null;
        }
    }

    protected DecisionTask nextDecisionTask(String nextPageToken) {
        return swf.pollForDecisionTask(request.withNextPageToken(nextPageToken));
    }
}

