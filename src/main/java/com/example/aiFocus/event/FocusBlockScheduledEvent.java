package com.example.aiFocus.event;

import com.example.aiFocus.entity.FocusBlock;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a focus block is scheduled.
 * This event can be used to trigger additional actions such as
 * sending reminders or notifications.
 */
public class FocusBlockScheduledEvent extends ApplicationEvent {

    private final FocusBlock focusBlock;

    /**
     * Constructs a new FocusBlockScheduledEvent.
     *
     * @param focusBlock the focus block that was scheduled
     */
    public FocusBlockScheduledEvent(FocusBlock focusBlock) {
        super(focusBlock);
        this.focusBlock = focusBlock;
    }

    /**
     * Gets the focus block associated with this event.
     *
     * @return the focus block
     */
    public FocusBlock getFocusBlock() {
        return focusBlock;
    }
}
