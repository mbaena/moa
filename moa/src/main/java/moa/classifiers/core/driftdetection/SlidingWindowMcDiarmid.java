package moa.classifiers.core.driftdetection;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

public class SlidingWindowMcDiarmid implements Serializable {

    private static final long serialVersionUID = -12022182323355496L;

    private static int LIMIT_WINDOW = 100;

    private int limit;
    private long n_elements;
    private double sum;
    private double average;
    private double max;
    private double min;
    private double half_range;
    private Deque<Long> max_index_deque;
    private Deque<Long> min_index_deque;
    private LinkedList<Double> window;
    private Deque<Double> window_c_i;
    private double sumSquared_c_i;

    public SlidingWindowMcDiarmid(int limit) {
        this.limit = limit;

        n_elements = 0;
        sum = 0.0;
        average = 0.0;
        max = -Double.MAX_VALUE;
        min = Double.MAX_VALUE;
        half_range = 0.0;
        max_index_deque = new ArrayDeque<>();
        min_index_deque = new ArrayDeque<>();

        window = new LinkedList<>();

        window_c_i = new ArrayDeque<>();
        sumSquared_c_i = 0.0;
    }

    public SlidingWindowMcDiarmid() {
        this(LIMIT_WINDOW);
    }


    public void addElement(double element) {
        boolean changeMaxMinSize = false;
        double prevMax = max, prevMin = min;
        int prevSize = this.window.size();
        int size;

        n_elements++;

        double out = 0.0;
        if (window.size() == limit) {
            out = window.pop();
        }
        window.add(element);
        size = this.window.size();

        // update MAX
        if (!max_index_deque.isEmpty() && max_index_deque.peekFirst() <= n_elements - limit) {
            max_index_deque.pollFirst();
        }
        while (!max_index_deque.isEmpty() && (window.get((int) (max_index_deque.peekLast() - n_elements + window.size() - 1)) <= element)) {
            max_index_deque.pollLast();
        }
        max_index_deque.offerLast(n_elements);
        max = window.get((int) (max_index_deque.peekFirst() - n_elements + window.size() - 1));

        // update MIN
        if (!min_index_deque.isEmpty() && min_index_deque.peekFirst() <= n_elements - limit) {
            min_index_deque.pollFirst();
        }
        while (!min_index_deque.isEmpty() && (window.get((int) (min_index_deque.peekLast() - n_elements + window.size() - 1)) >= element)) {
            min_index_deque.pollLast();
        }
        min_index_deque.offerLast(n_elements);
        min = window.get((int) (min_index_deque.peekFirst() - n_elements + window.size() - 1));

        // update the average
        sum = sum - out + element;
        average = sum / window.size();

        // if maximum or minimum change
        changeMaxMinSize = (min != prevMin) || (max != prevMax) || (prevSize != size);

        // update c_i list and sumSquared_c_i
        double aux_c_i;

        if (changeMaxMinSize) {
            half_range = (max + min) / 2.0;
            sumSquared_c_i = 0.0;
            window_c_i = new ArrayDeque<>();

            for (int i = 0; i < size; i++) {
                double aux_x_i = window.get(i);
                if (aux_x_i < half_range) {
                    aux_c_i = (max - aux_x_i) / size;
                } else {
                    aux_c_i = (aux_x_i - min) / size;
                }
                window_c_i.add(aux_c_i);
                sumSquared_c_i = sumSquared_c_i + (aux_c_i * aux_c_i);
            }
        }
        else
        {
            if (element < half_range) {
                aux_c_i = (max - element) / size;
            } else {
                aux_c_i = (element - min) / size;
            }

            double out_c_i = 0.0;
            if (window_c_i.size() == limit) {
                out_c_i = window_c_i.pop();
            }
            window_c_i.add(aux_c_i);

            sumSquared_c_i = sumSquared_c_i - (out_c_i * out_c_i) + (aux_c_i * aux_c_i);
        }
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

    Deque<Double> getC_i()  { return window_c_i;  }

    double getSumSquared_C_i() { return sumSquared_c_i; }


    @Override
    public String toString() {
        String output = "";
//        output = output + "DATA = " + window.toString() + "\n";

        if (window.size() > 0) {

//            output = output + "INCREMENTAL -- " +
//                    "Average = " + getAverage() +
//                    " max = " + getMax() +
//                    " min = " + getMin() + "\n" +
//                    //"INCREMENTAL -- " +
//                    //" sumSquared_C_i = " + getSumSquared_C_i() +
//                    "\n";
            output = output + "O(n) -------- " +
                    "AVERAGE = " + String.format("%.4f",getAverage()) +
                    " MAX = " + String.format("%.4f",max) +
                    " MIN = " + String.format("%.4f",min) + "\n" +
                    "O(n) -------- " +
                    "LIST C_I = " + windowToString(getC_i()) +
                    " SUMSQUARED_C_I = " + String.format("%.4f",getSumSquared_C_i())
            //+ "\n"
            ;
        }

        return output;
    }


    String windowToString(Deque <Double> list)
    {
        LinkedList <Double> aux_list = new LinkedList<>(list);
        String result = "[";
        for (int i = 0; i < list.size() - 1; i++) {
            result = result + String.format("%.3f", aux_list.get(i)) + "; ";
        }
        if (list.size() > 0) {
            result = result + String.format("%.3f", aux_list.get(list.size() - 1));
        }
        result = result + "]";
        return result;
    }

    boolean isFull()
    {
        return window.size() == this.limit;
    }
}
