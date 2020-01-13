package moa.classifiers.core.driftdetection;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/**
 * Drift detection method based in EDDM using McDiarmid's bound
 *
 * @author José del Campo (jcampo@uma.es)
 * @version $Revision: 1 $
 */
public class EDDM_McDiarmid extends AbstractChangeDetector {

    /**
     *
     */

    public IntOption EDDM_WINDOW_SIZE = new IntOption(
            "windowSize",
            'w',
            "The size of sliding window.",
            100, 1, Integer.MAX_VALUE);

    public FloatOption EDDM_DELTA = new FloatOption(
            "delta",
            'd',
            "The value of delta. (1-delta) is the confidence.",
            0.05, 0.000001, 1.0);

    private static final long serialVersionUID = 140980267062162000L;


    private int num_instances; // count the number of instances processed
    private double numErrors; // count the number of errors (wrong prediction)

    private int pos_last_error;         // position of last error
    private int pos_prelast_error;      // position of previous error

    private double cte_epsilon;  // constant value to avoid some calculations
    private double epsilon;      // epsilon by using McDiarmid's bound
    private SlidingWindowMcDiarmid window; // store values of a sliding window

    private double max_mtbf;        // keeps best values for the stream
    private double min_epsilon;     // keeps best values for the stream
    private double max_lower_bound; // keeps lower bound in the best scenario


    public EDDM_McDiarmid() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        super.resetLearning();
        window = new SlidingWindowMcDiarmid(EDDM_WINDOW_SIZE.getValue());
        cte_epsilon = Math.log(2.0 / EDDM_DELTA.getValue()) * 0.5;
        epsilon = Double.MAX_VALUE;
        num_instances = 1;
        numErrors = 0;
        pos_last_error = 0;
        pos_prelast_error = 0;
        max_mtbf = -Double.MAX_VALUE;
        min_epsilon = Double.MAX_VALUE;
        max_lower_bound = -Double.MAX_VALUE;
        this.estimation = 0.0;
    }

    @Override
    public void input(double prediction) {
        double distance;

        // prediction must be 1 or 0
        // It monitors the error rate
//         System.out.println("predicion = " + prediction + " -- num_instances " + num_instances);
        if (this.isChangeDetected == true || this.isInitialized == false) {
            resetLearning();
            this.isInitialized = true;
        }
        this.isChangeDetected = false;

        num_instances++;

        // if a new error is detected
        if (prediction == 1.0) {
            // update variables in super (AbstractChangeDetector)
            this.isWarningZone = false;
            this.delay = 0;

            // update local variables
            numErrors += 1;
            pos_prelast_error = pos_last_error;
            pos_last_error = num_instances - 1;

            // calculate time between failures (tbf)
            distance = pos_last_error - pos_prelast_error;

            // add new distance in sliding window
            window.addElement(distance);

            // for debug purpose
//            System.out.println(" n = " + num_instances +
//                    " -- epsilon = " + String.format("%.3f", epsilon) +
//                    " -- window.getAverage() = " + String.format("%.3f", window.getAverage()) +
//                    " -- max_mtbf = " + String.format("%.3f", max_mtbf) +
//                    " -- min_epsilon = " + String.format("%.3f", min_epsilon) +
//                    " -- max_lower_bound = " + String.format("%.3f", max_lower_bound));

            if (window.isFull()) {
                // if new average value in the window is outside the estimated interval
                if (window.getAverage() < max_lower_bound) {
                    this.isChangeDetected = true;
//                    System.out.println("CHANGE at: num_instances = " + num_instances + " numErrors = " + numErrors);

                }

                // calculation of error with McDiarmid's bound
                epsilon = Math.sqrt(cte_epsilon * window.getSumSquared_C_i());

                // updating interval if it improves (is tighter) the last one stored
                if ((window.getAverage() - epsilon) > (max_mtbf - min_epsilon)) {
                    max_mtbf = window.getAverage();
                    min_epsilon = epsilon;
                    max_lower_bound = max_mtbf - min_epsilon;
                }
            }
        }
        // there is no error
//        else {
//             System.out.println("NO ERROR: num_instances = " + num_instances + " numErrors = " + numErrors);
//        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
                                     ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}