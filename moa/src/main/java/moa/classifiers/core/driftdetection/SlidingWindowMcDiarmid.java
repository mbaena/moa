package moa.classifiers.core.driftdetection;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

public class SlidingWindowMcDiarmid implements Serializable {

	private static final long serialVersionUID = -1110271182325556496L;

	private static int LIMIT_WINDOW = 30;

    private int limit;
    private long n_elements;
    private double sum;
    private double average;
    private double max;
    private double min;
    private LinkedList<Double> window;
    private Deque<Double> window_ci;
    private Deque<Long> max_index_deque;
    private Deque<Long> min_index_deque;
    private double avg_tbf_change_i_max;
    private double avg_tbf_change_i_min;
    private double c_i;
    private double sumSquared_C_i;

    public SlidingWindowMcDiarmid(int limit) {
        this.limit = limit;
        n_elements = 0;

        window = new LinkedList<>();
        sum = 0.0;
        average = 0.0;
        max_index_deque = new ArrayDeque<>();
        max = -Double.MAX_VALUE;
        min_index_deque = new ArrayDeque<>();
        min = Double.MAX_VALUE;

        avg_tbf_change_i_max = 0.0;
        avg_tbf_change_i_min = 0.0;
        window_ci = new ArrayDeque<>();
        c_i = 0.0;
        sumSquared_C_i = 0.0;

    }

    public SlidingWindowMcDiarmid() {
        this(LIMIT_WINDOW);
    }


    public void addElement(double element) {
    	n_elements++;
    	
    	double out = 0.0;
    	double out_c_i = 0.0;
        if (window.size() == limit) {
            out =  window.pop();
            out_c_i = window_ci.pop();
        }
        
        window.add(element);
        
        // update MAX
		if (!max_index_deque.isEmpty() && max_index_deque.peekFirst() <= n_elements - limit) {
			max_index_deque.pollFirst();
		}
		while (!max_index_deque.isEmpty() && (window.get((int) (max_index_deque.peekLast()-n_elements+window.size()-1)) <= element)) {
			max_index_deque.pollLast();
		}
		max_index_deque.offerLast(n_elements);
        max = window.get((int) (max_index_deque.peekFirst()-n_elements+window.size()-1));
    	            
        // update MIN
		if (!min_index_deque.isEmpty() && min_index_deque.peekFirst() <= n_elements - limit) {
			min_index_deque.pollFirst();
		}
		while (!min_index_deque.isEmpty() && (window.get((int) (min_index_deque.peekLast()-n_elements+window.size()-1)) >= element)) {
			min_index_deque.pollLast();
		}
		min_index_deque.offerLast(n_elements);
        min = window.get((int) (min_index_deque.peekFirst()-n_elements+window.size()-1));
        
        double partial_sum = sum - out;
        
        // update change_max
        avg_tbf_change_i_max = (partial_sum + max)/window.size();
        // update change_min
        avg_tbf_change_i_min = (partial_sum + min)/window.size();

        // update SUM
        sum = partial_sum + element;

        // update AVERAGE
        average = sum / window.size();
        
        
        // UPDATE c_i
        if ((avg_tbf_change_i_max - average) > (average - avg_tbf_change_i_min)) {  //   (max-element) > (element-min)
            c_i = avg_tbf_change_i_max - average;
        } else {
            c_i = average - avg_tbf_change_i_min;
        }
        window_ci.add(c_i);

        sumSquared_C_i += (c_i*c_i) - (out_c_i*out_c_i);
            
    }

    double getAverage() {
        return average;
    }

    double getMax() {
        return max;
    }

    double getMin() {
        return min;
    }


    double getSumSquared_C_iBATCH()
    {
    	return sumSquared_C_i;
    }


    @Override
    public String toString() {
        String output = "";
        output = output + "DATA = " + window.toString() + "\n";

        if (window.size() > 0) {

            output = output + "INCREMENTAL -- " +
                    "Average = " + getAverage() +
                    " max = " + getMax() +
                    " min = " + getMin() + "\n" +
                    //"INCREMENTAL -- " +
                    //" sumSquared_C_i = " + getSumSquared_C_i() +
                    "\n";
            output = output + "BATCH -------- " +
                    "AVERAGE = " + average +
                    " MAX = " + max +
                    " MIN = " + min + "\n" +
                    "BATCH -------- " +
                    "LIST C_I = " + window_ci +
                    " SUMSQUARED_C_I = " + getSumSquared_C_iBATCH() + "\n";
        }

        return output;
    }
    
    boolean isFull()
    {
        return window.size() == this.limit;
    }
}
