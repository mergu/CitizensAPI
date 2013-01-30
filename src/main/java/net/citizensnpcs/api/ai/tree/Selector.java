package net.citizensnpcs.api.ai.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;


import com.google.common.base.Function;

/**
 * A selector of sub-goals, that chooses a single {@link Behavior} to execute
 * from a list. The default selection function is a random selection.
 */
public class Selector extends Composite {
    private Behavior executing;
    private boolean retryChildren = false;
    private final Function<List<Behavior>, Behavior> selectionFunction;

    private Selector(Function<List<Behavior>, Behavior> selectionFunction, boolean retryChildren,
            Collection<Behavior> behaviors) {
        super(behaviors);
        this.selectionFunction = selectionFunction;
        this.retryChildren = retryChildren;
    }

    protected Behavior getNextBehavior() {
        return selectionFunction.apply(getBehaviors());
    }

    @Override
    public void reset() {
        if (executing != null)
            executing.reset();
        executing = null;
    }

    @Override
    public BehaviorStatus run() {
        if (executing == null) {
            executing = getNextBehavior();
        }
        BehaviorStatus status = executing.run();
        if (status == BehaviorStatus.FAILURE) {
            if (retryChildren) {
                executing.reset();
                executing = getNextBehavior();
                return BehaviorStatus.RUNNING;
            }
        }
        return status;
    }

    public static class Builder {
        private final Collection<Behavior> behaviors;
        private boolean retryChildren;
        private Function<List<Behavior>, Behavior> selectionFunction = RANDOM_SELECTION;

        private Builder(Collection<Behavior> behaviors) {
            this.behaviors = behaviors;
        }

        public Selector build() {
            return new Selector(selectionFunction, retryChildren, behaviors);
        }

        /**
         * Sets whether to retry child {@link Behavior}s when they return
         * {@link BehaviorStatus#FAILURE}.
         * 
         * @param retry
         *            Whether to retry children (default: false)
         */
        public Builder retryChildren(boolean retry) {
            retryChildren = retry;
            return this;
        }

        /**
         * Sets the {@link Function} that selects a {@link Behavior} to execute
         * from a list of behaviors, such as a random selection or a priority
         * selection. See {@link Selectors} for some helper methods.
         * 
         * @param function
         *            The selection function
         */
        public Builder selectionFunction(Function<List<Behavior>, Behavior> function) {
            selectionFunction = function;
            return this;
        }
    }

    private static final Random RANDOM = new Random();

    private static final Function<List<Behavior>, Behavior> RANDOM_SELECTION = new Function<List<Behavior>, Behavior>() {
        @Override
        public Behavior apply(@Nullable List<Behavior> behaviors) {
            return behaviors.get(RANDOM.nextInt(behaviors.size()));
        }
    };

    public static Builder selecting(Behavior... behaviors) {
        return selecting(Arrays.asList(behaviors));
    }

    public static Builder selecting(Collection<Behavior> behaviors) {
        return new Builder(behaviors);
    }
}