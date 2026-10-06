package makrovModel;

import javax.naming.InsufficientResourcesException;
import java.util.*;

/**
 * Multi-order Markov model
 * @author Axel Berndt
 */
public class MarkovModel<T> {
    private final ArrayList<List<T>> sequences = new ArrayList<>();                             // list of sequences with which the Markov model is trained
    private final HashMap<Integer, TransitionMatrix<T>> transitionMatrices = new HashMap<>();   // transition matrices are accessed by order

    /**
     * constructor, no transition matrices for any order is prepared
     */
    public MarkovModel() {
    }

    /**
     * constructor, prepares transition matrices for orders 0 through maxOrder
     * @param maxOrder
     */
    public MarkovModel(int maxOrder) {
        for (int order = 0; order <= maxOrder; ++order)
            this.trainOrder(order);
    }

    public void train(List<T> sequence) {
        if ((sequence == null) || sequence.isEmpty())
            return;

        this.sequences.add(sequence);

        for (Map.Entry<Integer, TransitionMatrix<T>> transitionMatrixEntry : this.transitionMatrices.entrySet())
            transitionMatrixEntry.getValue().train(sequence);
    }

    public void trainOrder(int order) {
        if (this.transitionMatrices.get(order) != null)
            return;

        TransitionMatrix<T> matrix = new TransitionMatrix<>(order);
        for (List<T> sequence : this.sequences)
            matrix.train(sequence);
        this.transitionMatrices.put(order, matrix);
    }

    public T getNext(List<T> pastEvents) {
        int order = (pastEvents == null) ? 0 : pastEvents.size();
        TransitionMatrix<T> matrix = this.transitionMatrices.get(order);
        if (matrix == null)     // if this order has not been trained yet
            return null;        // cancel

        return matrix.getNext(pastEvents);
    }

    public T getNextWithInverseProbabilities(List<T> pastEvents) {
        int order = (pastEvents == null) ? 0 : pastEvents.size();
        if (order == 0)
            return this.getNext(pastEvents);

        TransitionMatrix<T> matrix = this.transitionMatrices.get(order);
        if (matrix == null)     // if this order has not been trained yet
            return null;        // cancel

        return matrix.getNextWithInverseProbabilities(pastEvents);
    }

    /**
     * the initial sequence is randomly chosen from the sequences available
     * @param order
     * @return the initial sequence
     * @throws IllegalArgumentException
     * @throws InsufficientResourcesException
     */
    public List<T> getInitialSequence(int order) throws IllegalArgumentException, InsufficientResourcesException {
        TransitionMatrix<T> matrix = this.transitionMatrices.get(order);

        if (matrix == null)
            throw new IllegalArgumentException("The Markov model was not trained with order " + order + ".");

        if (matrix.isEmpty())
            throw new InsufficientResourcesException("Transition matrix for order " + order + " is empty. Train the Markov model with data!");

        // choose a random sequence and return its initial sequence of states
        List<T> sequence = this.sequences.get((new Random()).nextInt(this.sequences.size()));

        if (sequence.size() >= order)           // if the sequence is long enough
            return sequence.subList(0, order);

        // the sequence is not long enough, we have to expand it circularly
        ArrayList<T> clone = new ArrayList<>(sequence);
        for (int i = sequence.size(); i < order; ++i)
            clone.add(clone.get(i % sequence.size()));

        return clone;
    }

    public TransitionMatrix<T> getTransitionMatrix(int order) {
        return this.transitionMatrices.get(order);
    }

    public ArrayList<List<T>> getTrainingData() {
        return this.sequences;
    }

    /**
     * During the generation process we wish to switch to another order, i.e. a Markov model/transition matrix of another order.
     * Based on the series of past events, this method finds a closest matching state in the target model.
     * If events must be added to the series to achieve a state that exists in the target model, these will be returned.
     * After appending these events to the series a handover to the target model can be performed.
     * @param pastEvents an arbitrarily long list of past events
     * @param order the target order
     * @return a series of events to be appended to the series of past events, so handover to the target model can be performed
     * @throws IllegalArgumentException
     * @throws InsufficientResourcesException
     */
    public List<T> modulateState(List<T> pastEvents, int order) throws IllegalArgumentException, InsufficientResourcesException {
        TransitionMatrix<T> matrix = this.transitionMatrices.get(order);
        if (matrix == null)
            throw new IllegalArgumentException("The Markov model was not trained with order " + order + ".");

        // the trivial case
        if ((pastEvents == null) || pastEvents.isEmpty())           // no past events
            return this.getInitialSequence(order);                  // we simply start with an initial sequence

        if (order == 0)
            return new ArrayList<>();

        // if the series is long enough, so there is a chance that the current state matches with a state in the target model
        if ((pastEvents.size() >= order)
                && (this.getNext(pastEvents.subList(pastEvents.size() - order, pastEvents.size())) != null)) {
            return new ArrayList<>();
        }

        // take the last order-1 events from pastEvents and try to find a close match to the list of states in the target matrix
        ArrayList<List<T>> candidates = new ArrayList<>();
        for (List<T> lastFewEvents = pastEvents.subList(Math.max(0, pastEvents.size() - order + 1), pastEvents.size()); !lastFewEvents.isEmpty(); lastFewEvents.remove(0)) {
            for (List<T> key : this.transitionMatrices.get(order).keySet()) {       // search the matrix for a matching key
                if (lastFewEvents.equals(key.subList(0, lastFewEvents.size())))
                    candidates.add(key);                                                // found match, add it to candidates
            }
            if (!candidates.isEmpty()) {                                                                    // if candidates is no longer empty
                List<T> theChosenOne = candidates.get(new Random().nextInt(candidates.size()));             // randomly choose one of the candidates
                return new ArrayList<>(theChosenOne.subList(lastFewEvents.size(), theChosenOne.size()));    // add the missing last events from candidate to the list of events to be returned
            }
        }

        // we found no candidate; we have no events to return; randomly choose a key from the target matrix
        return (new ArrayList<>(this.transitionMatrices.get(order).keySet())).get((new Random()).nextInt(this.transitionMatrices.get(order).keySet().size()));
    }


    public boolean isEmpty() {
        for (HashMap.Entry<Integer, TransitionMatrix<T>> entry : this.transitionMatrices.entrySet()) {
            if (!entry.getValue().isEmpty())
                return false;
        }
        return true;
    }

    public void clear() {
        this.sequences.clear();
        this.transitionMatrices.clear();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("Markov model trained with ").append(this.sequences.size()).append(" sequences; contains transition matrices for orders\n");

        for (Map.Entry<Integer, TransitionMatrix<T>> entry : this.transitionMatrices.entrySet())
            out.append("   ").append(entry.getKey()).append(" with ").append(entry.getValue().size()).append(" entries\n");

        return out.toString();
    }
}
