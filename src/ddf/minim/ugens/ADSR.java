package ddf.minim.ugens;

import ddf.minim.AudioOutput;
import ddf.minim.Minim;

/**
 * A UGen that plays input audio through a standard ADSR envelope based on time
 * from noteOn and noteOff
 *
 * @author Anderson Mills
 *
 */
public class ADSR extends UGen {

    /**
     * The default input is "audio."<br/> You won't need to patch to this
     * directly, since simply patching to the ADSR itself will achieve the same
     * result.
     */
    public UGenInput audio;
    // amplitude before the ADSR hits
    private float beforeAmplitude;
    // amplitude after the release of the ADSR
    private float afterAmplitude;
    // the max amplitude of the envelope
    private float maxAmplitude;
    // the current amplitude
    private float amplitude;
    // the time of the attack
    private float attackTime;
    // the time of the decay
    private float decayTime;
    // the level of the sustain
    private float sustainLevel;
    // the time of the release
    private float releaseTime;
    // the current size of the step
    private float timeStepSize;
    // the time from noteOn
    private float timeFromOn;
    // the time from noteOff
    private float timeFromOff;
    // the envelope has received noteOn
    private boolean isTurnedOn;
    // the envelope has received noteOff
    private boolean isTurnedOff;
    // unpatch the note after it's finished
    private boolean unpatchAfterRelease;
    private AudioOutput output;

    /**
     * Constructor for an ADSR envelope. Maximum amplitude is set to 1.0. Attack
     * and decay times are set to 1 sec. Sustain level is set to 0.0. Release
     * time is set to 1 sec. Amplitude before and after the envelope is set to
     * 0.
     */
    public ADSR() {
        this(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f);
    }

    /**
     * Constructor for an ADSR envelope with maximum amplitude. Attack and decay
     * times are set to 1 sec. Sustain level is set to 0.0. Release time is set
     * to 1 sec. Amplitude before and after the envelope is set to 0.
     */
    public ADSR(float maxAmp) {
        this(maxAmp, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f);
    }

    /**
     * Constructor for an ADSR envelope with maximum amplitude, attack Time.
     * Decay time is set to 1 sec. Sustain level is set to 0.0. Release time is
     * set to 1 sec. Amplitude before and after the envelope is set to 0.
     */
    public ADSR(float maxAmp, float attTime) {
        this(maxAmp, attTime, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f);
    }

    /**
     * Constructor for an ADSR envelope with maximum amplitude, attack Time, and
     * decay time. Sustain level is set to 0.0. Release time is set to 1 sec.
     * Amplitude before and after the envelope is set to 0.
     */
    public ADSR(float maxAmp, float attTime, float decTime) {
        this(maxAmp, attTime, decTime, 0.0f, 1.0f, 0.0f, 0.0f);
    }

    /**
     * Constructor for an ADSR envelope with maximum amplitude, attack Time,
     * decay time, and sustain level. Release time is set to 1 sec. Amplitude
     * before and after the envelope is set to 0.
     */
    public ADSR(float maxAmp, float attTime, float decTime, float susLvl) {
        this(maxAmp, attTime, decTime, susLvl, susLvl, 1.0f, 0.0f);
    }

    /**
     * Constructor for an ADSR envelope with maximum amplitude, attack Time,
     * decay time, sustain level, and release time. Amplitude before and after
     * the envelope is set to 0.
     */
    public ADSR(float maxAmp, float attTime, float decTime, float susLvl, float relTime) {
        this(maxAmp, attTime, decTime, susLvl, relTime, 0.0f, 0.0f);
    }

    /**
     * Constr uctor for an ADSR envelope with maximum amplitude, attack Time,
     * decay time, sustain level, release time, an amplitude before the
     * envelope. Amplitude after the envelope is set to 0.
     */
    public ADSR(float maxAmp, float attTime, float decTime, float susLvl, float relTime, float befAmp) {
        this(maxAmp, attTime, decTime, susLvl, relTime, befAmp, 0.0f);
    }

    /**
     * Constructor for an ADSR envelope with maximum amplitude, attack Time,
     * decay time, sustain level, release time, an amplitude before the
     * envelope, and an amplitude after the envelope.
     */
    public ADSR(float maxAmp, float attTime, float decTime, float susLvl, float relTime, float befAmp, float aftAmp) {
        super();
        audio = new UGenInput(InputType.AUDIO);
        maxAmplitude = maxAmp;
        attackTime = attTime;
        decayTime = decTime;
        sustainLevel = susLvl;
        releaseTime = relTime;
        beforeAmplitude = befAmp;
        afterAmplitude = aftAmp;
        amplitude = beforeAmplitude;
        isTurnedOn = false;
        isTurnedOff = false;
        timeFromOn = -1.0f;
        timeFromOff = -1.0f;
        unpatchAfterRelease = false;
    }

    /**
     * Permits the changing of the ADSR parameters.
     */
    public void setParameters(float maxAmp, float attTime, float decTime, float susLvl, float relTime, float befAmp, float aftAmp) {
        maxAmplitude = maxAmp;
        attackTime = attTime;
        decayTime = decTime;
        sustainLevel = susLvl;
        releaseTime = relTime;
        beforeAmplitude = befAmp;
        afterAmplitude = aftAmp;
    }

    /**
     * Speficies that the ADSR envelope should begin.
     */
    public void noteOn() {
        timeFromOn = 0f;
        isTurnedOn = true;
    }

    /**
     * Specifies that the ADSR envelope should start the release time.
     */
    public void noteOff() {
        timeFromOff = 0f;
        isTurnedOff = true;
    }

    /**
     * Use this method to notify the ADSR that the sample rate has changed.
     */
    @Override
    protected void sampleRateChanged() {
        timeStepSize = 1 / sampleRate();
    }

    /**
     * Tell the ADSR that it should unpatch itself from the output after the
     * release time.
     *
     * @param output the output this should unpatch itself from
     */
    public void unpatchAfterRelease(AudioOutput output) {
        unpatchAfterRelease = true;
        this.output = output;
    }

    @Override
    protected void uGenerate(float[] channels) {
        // before the envelope, just output the beforeAmplitude*audio
        if (!isTurnedOn) {
            for (int i = 0; i < channels.length; i++) {
                channels[i] = beforeAmplitude * audio.getLastValues()[i];
            }
        } // after the envelope, just output the afterAmplitude*audio
        else if (timeFromOff > releaseTime) {
            for (int i = 0; i < channels.length; i++) {
                channels[i] = afterAmplitude * audio.getLastValues()[i];
            }
            if (unpatchAfterRelease) {
                unpatch(output);
                Minim.debug(" unpatching ADSR ");
            }
        } // inside the envelope
        else {
            if ((isTurnedOn) && (!isTurnedOff)) {
                // ATTACK
                if (timeFromOn <= attackTime) {
                    // use time remaining until maxAmplitude to change amplitude
                    float timeRemain = (attackTime - timeFromOn);
                    amplitude += (maxAmplitude - amplitude) * timeStepSize / timeRemain;
                } // DECAY
                else if ((timeFromOn > attackTime) && (timeFromOn <= (attackTime + decayTime))) {
                    // use time remaining until sustain to change to sustain level
                    float timeRemain = (attackTime + decayTime - timeFromOn);
                    amplitude += (sustainLevel * maxAmplitude - amplitude) * timeStepSize / timeRemain;
                } // SUSTAIN
                else if (timeFromOn > (attackTime + decayTime)) {
                    // hold the sustain level
                    amplitude = sustainLevel * maxAmplitude;
                }
                timeFromOn += timeStepSize;
            } // RELEASE
            else //isTurnedOn and isTurnedOFF and timeFromOff < releaseTime
            {
                // use remaining time to get to afterAmplitude
                float timeRemain = (releaseTime - timeFromOff);
                amplitude += (afterAmplitude - amplitude) * timeStepSize / timeRemain;
                timeFromOff += timeStepSize;
            }
            // finally multiply the input audio to generate the output
            for (int i = 0; i < channels.length; i++) {
                channels[i] = amplitude * audio.getLastValues()[i];
            }
        }
    }
}
