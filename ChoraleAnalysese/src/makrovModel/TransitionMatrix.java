package makrovModel;

import meico.supplementary.RandomNumberProvider;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * transition matrices are represented as Hashmap of (state = sequence of past events; Hashmap of (next events; their probability))
 * @author Axel Berndt
 */
public class TransitionMatrix<T> extends HashMap<List<T>, HashMap<T, Double>> {
    public final int order;

    public TransitionMatrix(int order) {
        super();
        this.order = order;

        if (this.order == 0)
            this.put(null, new HashMap<>());
    }

    public void train(List<T> sequence) {
        if ((sequence == null) || (sequence.size() <= this.order))
            return;

        if (this.order == 0) {
            for (T state : sequence)
                this.put(null, state);
            return;
        }

        // expand sequence for circularity
        ArrayList<T> clone = new ArrayList<>(sequence);
        for (int i = 0; i < this.order; ++i)
            clone.add(clone.get(i));

        // do a sliding window over the sequence and add its succession to the matrix
        CircularFifoQueue<T> pastEvents = new CircularFifoQueue<>(this.order);
        for (int i = 0; i < clone.size(); ++i) {
            if (!pastEvents.isAtFullCapacity()) {
                pastEvents.add(clone.get(i));
                continue;
            }

            this.put(new ArrayList<>(pastEvents), clone.get(i));
//            this.put((List<T>) Arrays.asList(pastEvents.toArray()), clone.get(i));
            pastEvents.add(clone.get(i));
        }
    }

    private HashMap<T, Double> put(List<T> sequenceOfPastEvents, T nextEvent) {
        if ((this.order == 0) && ((sequenceOfPastEvents == null) || sequenceOfPastEvents.isEmpty())) {
            HashMap<T, Double> transitionProbabilities = this.get(null);
            Double prob = transitionProbabilities.get(nextEvent);
            if (prob == null)
                prob = 0.0;
            transitionProbabilities.put(nextEvent, prob + 1.0);
            return super.put(null, transitionProbabilities);
        }

        if ((sequenceOfPastEvents == null) || (sequenceOfPastEvents.size() != this.order)) {
            System.err.println("The size of the sequence of past events must equal the order of the Markov chain.");
            return null;
        }

        HashMap<T, Double> transitionProbabilities = this.get(sequenceOfPastEvents);
        if (transitionProbabilities == null) {
            transitionProbabilities = new HashMap<>();
            transitionProbabilities.put(nextEvent, 1.0);
            return super.put(sequenceOfPastEvents, transitionProbabilities);
        }

        Double prop = transitionProbabilities.get(nextEvent);
        if (prop == null)
            prop = 0.0;
        transitionProbabilities.put(nextEvent, prop + 1.0);
        return super.put(sequenceOfPastEvents, transitionProbabilities);
    }

    public T getNext(List<T> pastEvents) {
        HashMap<T, Double> probabilities = this.get(pastEvents);

        if (probabilities == null)
            return null;

        double sum = 0.0;
        for (Entry<T, Double> entry : probabilities.entrySet())
            sum += entry.getValue();

        double randomNumber = RandomNumberProvider.createRandomNumberProvider_uniformDistribution(0.0, sum).getValue(0);
        double cumulative = 0.0;
        for (Entry<T, Double> entry : probabilities.entrySet()) {
            cumulative += entry.getValue();
            if (randomNumber <= cumulative) {
                return entry.getKey();
            }
        }

        return null;
    }

    public T getNextWithInverseProbabilities(List<T> pastEvents) {
        HashMap<T, Double> probabilities = this.get(pastEvents);
        HashMap<T, Double> inverseProbabilities = new HashMap<>();          // same content as probabilities but with inverse values

        if (probabilities == null)
            return null;

        double sum = 0.0;
        for (Entry<T, Double> entry : probabilities.entrySet()) {
            double inverseProbability = 1.0 / entry.getValue();             // invert probability
            inverseProbabilities.put(entry.getKey(), inverseProbability);
            sum += inverseProbability;
        }

        double randomNumber = RandomNumberProvider.createRandomNumberProvider_uniformDistribution(0.0, sum).getValue(0);
        double cumulative = 0.0;
        for (Entry<T, Double> entry : inverseProbabilities.entrySet()) {
            cumulative += entry.getValue();
            if (randomNumber <= cumulative) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * returns a list of all features that this transition matrix contains
     * @return
     */
    public ArrayList<T> getFeatures() {
        ArrayList<T> out = new ArrayList<>();

        for (HashMap<T, Double> value : this.values()) {
            for (T key : value.keySet()) {
                if (!out.contains(key))
                    out.add(key);
            }
        }

        return out;
    }
}
