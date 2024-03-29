package ddf.minim.ugens;

/**
 * Balance is for controlling the left/right channel balance of a stereo signal.
 * This is different from Pan because rather than moving the signal around it
 * simply attenuates the existing audio. <p> A balance of 0 will make no change
 * to the incoming audio. Negative balance will decrease the volume of the
 * <em>right</em> channel and positive balance will decrease the volume of the
 * <em>left</em> channel. This is meant to mirror how a balance knob on a
 * typical stereo operates.
 *
 * @author Anderson Mills
 *
 */
public class Balance extends UGen {

    /**
     * The audio input is where audio comes in to be balanced. You won't need to
     * patch to this directly, patching to the balance UGen itself will achieve
     * the same thing.
     */
    public UGenInput audio;
    /**
     * The balance control should be driven by UGens that generate values in the
     * range [-1, 1].
     */
    public UGenInput balance;
    private float balanceVal;

    /**
     * Construct a Balance with a value of 0 (no change).
     *
     */
    public Balance() {
        this(0.0f);
    }

    /**
     * Construct a balance with a particular value.
     *
     * @param balanceVal a value in the range [-1, 1]
     */
    public Balance(float balanceVal) {
        super();
        // jam3: These can't be instantiated until the uGenInputs ArrayList
        //       in the super UGen has been constructed
        //audio = new UGenInput(InputType.AUDIO);
        audio = new UGenInput(InputType.AUDIO);
        balance = new UGenInput(InputType.CONTROL);
        this.balanceVal = balanceVal;
    }

    /**
     * Set the balance value directly.
     *
     * @param balanceVal a value in the range [-1, 1]
     */
    public void setBalance(float balanceVal) {
        this.balanceVal = balanceVal;
    }

    @Override
    protected void uGenerate(float[] channels) {
        for (int i = 0; i < channels.length; i++) {
            float tmp = audio.getLastValues()[i];
            if (balance.isPatched()) {
                balanceVal = balance.getLastValues()[0];
            }
            channels[i] = tmp * (float) Math.min(1.0f, Math.max(0.0f, 1.0f + Math.pow(-1.0f, i) * balanceVal));
        }
    }
}