/*
 *  Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

// This Chebyshev Filter implementation has been ported from the BASIC 
// implementation outlined in Chapter 20 of The Scientist and Engineer's
// Guide to Signal Processing, which can be found at:
//
//     http://www.dspguide.com/ch20.htm

package ddf.minim.effects;

import ddf.minim.Minim;

/**
 * A Chebyshev filter is an IIR filter that uses a particular method to
 * calculate the coefficients of the filter. It is defined by whether it is a
 * low pass filter or a high pass filter and the number of poles it has. You
 * needn't worry about what a pole is, exactly, just know that more poles
 * usually makes for a better filter. An additional limitation is that the
 * number of poles must be even. See {@link #setPoles(int)} for more information
 * about poles. Another characteristic of Chebyshev filters is how much "ripple"
 * they allow in the pass band. The pass band is the range of frequencies that
 * the filter lets through. The "ripple" in the pass band can be seen as wavy
 * line in the frequency response of the filter. Lots of ripple is bad, but more
 * ripple gives a faster rolloff from the pass band to the stop band (the range
 * of frequencies blocked by the filter). Faster rolloff is good because it
 * means the cutoff is sharper. Ripple is expressed as a percentage, such as
 * 0.5% ripple.
 * 
 * @author Damien Di Fede
 * @see <a href="http://www.dspguide.com/ch20.htm">Chebyshev Filters</a>
 * 
 */
public class ChebFilter extends IIRFilter
{
  /** A constant used to indicate a low pass filter. */
  public static final int LP = 1;
  /** A constant used to indicate a high pass filter. */
  public static final int HP = 2;
  private static final float PI = (float) Math.PI;
  private int type, poles;
  private float ripple;
  private boolean canCalc;

  /**
   * Constructs a Chebyshev filter with a cutoff of the given frequency, of the given
   * type, with the give amount of ripple in the pass band, and with the given
   * number of poles, that will be used to filter audio of that was recorded at
   * the given sample rate.
   * 
   * @param frequency
   *          the cutoff frequency of the filter
   * @param type
   *          the type of filter, either ChebFilter.LP or ChebFilter.HP
   * @param ripple
   *          the percentage of ripple, such as 0.005
   * @param poles
   *          the number of poles, must be even and in the range [2, 20]
   * @param sampleRate
   *          the sample rate of audio that will be filtered
   */
  public ChebFilter(float frequency, int type, float ripple, int poles,
      float sampleRate)
  {
    super(frequency, sampleRate);
    canCalc = false;
    setType(type);
    setRipple(ripple);
    setPoles(poles);
    canCalc = true;
    calcCoeff();
    initArrays(2);
  }

  /**
   * Sets the type of the filter. Either ChebFilter.LP or ChebFilter.HP
   * 
   * @param t
   *          the type of the filter
   */
  public void setType(int t)
  {
    if ( t != LP && t != HP )
    {
      Minim.error("Invalid filter type, defaulting to low pass.");
      t = LP;
    }
    type = t;
    calcCoeff();
  }

  /**
   * Returns the type of the filter.
   */
  public int getType()
  {
    return type;
  }

  /**
   * Sets the ripple percentage of the filter.
   * 
   * @param r
   *          the ripple percentage
   */
  public void setRipple(float r)
  {
    ripple = r;
    calcCoeff();
  }

  /**
   * Returns the ripple percentage of the filter.
   * 
   * @return the ripple percentage
   */
  public float getRipple()
  {
    return ripple;
  }

  /**
   * Sets the number of poles used in the filter. The number of poles must be
   * even and between 2 and 20. This function will report an error if either of
   * those conditions are not met. However, it should also be mentioned that
   * depending on the current cutoff frequency of the filter, the number of
   * poles that will result in a <i>stable</i> filter, can be a few as 4. The
   * function does not report an error in the case of the number of requested
   * poles resulting in an unstable filter. For reference, here is a table of
   * the maximum number of poles possible according to cutoff frequency:
   * <p>
   * <table border="1" cellpadding="5">
   * <tr>
   * <td>Cutoff Frequency<br />
   * (expressed as a fraction of the sampling rate)</td>
   * <td>0.02</td>
   * <td>0.05</td>
   * <td>0.10</td>
   * <td>0.25</td>
   * <td>0.40</td>
   * <td>0.45</td>
   * <td>0.48</td>
   * </tr>
   * <tr>
   * <td>Maximum poles</td>
   * <td>4</td>
   * <td>6</td>
   * <td>10</td>
   * <td>20</td>
   * <td>10</td>
   * <td>6</td>
   * <td>4</td>
   * </tr>
   * </table>
   * 
   * @param p -
   *          the number of poles
   */
  public void setPoles(int p)
  {
    if (p < 2)
    {
      Minim
          .error("ChebFilter.setPoles: The number of poles must be at least 2.");
      return;
    }
    if (p % 2 != 0)
    {
      Minim.error("ChebFilter.setPoles: The number of poles must be even.");
      return;
    }
    if (p > 20)
    {
      Minim.error("ChebFilter.setPoles: The maximum number of poles is 20.");
    }
    poles = p;
    calcCoeff();
    initArrays(2);
  }

  /**
   * Returns the number of poles in the filter.
   * 
   * @return the number of poles
   */
  public int getPoles()
  {
    return poles;
  }

  protected synchronized void calcCoeff()
  {
    a = new float[0];
    b = new float[0];
    if (!canCalc) return;
    // where the poles will wind up
    float[] ca = new float[23];
    float[] cb = new float[23];
    // temporary arrays for working with ca and cb
    float[] ta = new float[23];
    float[] tb = new float[23];
    // arrays to hold the two-pole coefficients
    // used during the aggregation process
    float[] pa = new float[3];
    float[] pb = new float[2];
    // I don't know why this must be done
    ca[2] = 1;
    cb[2] = 1;
    // calculate two poles at a time
    for (int p = 1; p <= poles / 2; p++)
    {
      // calc pair p, put the results in pa and pb
      calcTwoPole(p, pa, pb);
      // copy ca and cb into ta and tb
      System.arraycopy(ca, 0, ta, 0, ta.length);
      System.arraycopy(cb, 0, tb, 0, tb.length);
      // add coefficients to the cascade
      for (int i = 2; i < 23; i++)
      {
        ca[i] = pa[0] * ta[i] + pa[1] * ta[i - 1] + pa[2] * ta[i - 2];
        cb[i] = tb[i] - pb[0] * tb[i - 1] - pb[1] * tb[i - 2];
      }
    }
    // final stage of combining coefficients
    cb[2] = 0;
    for (int i = 0; i < 21; i++)
    {
      ca[i] = ca[i + 2];
      cb[i] = -cb[i + 2];
    }
    // normalize the gain
    float sa = 0;
    float sb = 0;
    for (int i = 0; i < 21; i++)
    {
      if (type == LP)
      {
        sa += ca[i];
        sb += cb[i];
      }
      else
      {
        sa += ca[i] * (float) Math.pow(-1, i);
        sb += cb[i] * (float) Math.pow(-1, i);
      }
    }
    float gain = sa / (1 - sb);
    for (int i = 0; i < 21; i++)
    {
      ca[i] /= gain;
    }
    // initialize the coefficient arrays used by process()
    a = new float[poles + 1];
    b = new float[poles];
    // copy the values from ca and cb into a and b
    // in this implementation cb[0] = 0 and cb[1] is where
    // the b coefficients begin, so they are numbered the way
    // one normally numbers coefficients when talking about IIR filters
    // however, process() expects b[0] to be the coefficient B1
    // so we copy cb over to b starting at index 1
    System.arraycopy(ca, 0, a, 0, a.length);
    System.arraycopy(cb, 1, b, 0, b.length);
  }

  private void calcTwoPole(int p, float[] pa, float[] pb)
  {
    float np = (float) poles;
    float angle = PI / (np * 2) + (p - 1) * PI / np;
    float rp = -(float) Math.cos(angle);
    float ip = (float) Math.sin(angle);
    // warp from a circle to an ellipse
    if (ripple > 0)
    {
      float es = 1 / (float) Math
          .sqrt(Math.pow(100.0f / (100.0f - ripple), 2) - 1);
      float vx = (1 / np) * (float) Math.log(es + Math.sqrt(es * es + 1));
      float kx = (1 / np) * (float) Math.log(es + Math.sqrt(es * es - 1));
      kx = (float) (Math.exp(kx) + Math.exp(-kx)) / 2;
      rp *= ((Math.exp(vx) - Math.exp(-vx)) / 2) / kx;
      ip *= ((Math.exp(vx) + Math.exp(-vx)) / 2) / kx;
    }
    // s-domain to z-domain conversion
    float t = 2 * (float) Math.tan(0.5);
    float w = 2 * PI * (frequency()/sampleRate());
    float m = rp * rp + ip * ip;
    float d = 4 - 4 * rp * t + m * t * t;
    float x0 = (t * t) / d;
    float x1 = (2 * t * t) / d;
    float x2 = (t * t) / d;
    float y1 = (8 - 2 * m * t * t) / d;
    float y2 = (-4 - 4 * rp * t - m * t * t) / d;
    // LP to LP, or LP to HP transform
    float k;
    if (type == HP)
      k = -(float) Math.cos(w / 2 + 0.5f) / (float) Math.cos(w / 2 - 0.5f);
    else
      k = (float) Math.sin(0.5f - w / 2) / (float) Math.sin(0.5 + w / 2);
    d = 1 + y1 * k - y2 * k * k;
    pa[0] = (x0 - x1 * k + x2 * k * k) / d;
    pa[1] = (-2 * x0 * k + x1 + x1 * k * k - 2 * x2 * k) / d;
    pa[2] = (x0 * k * k - x1 * k + x2) / d;
    pb[0] = (2 * k + y1 + y1 * k * k - 2 * y2 * k) / d;
    pb[1] = (-(k * k) - y1 * k + y2) / d;
    if (type == HP)
    {
      pa[1] = -pa[1];
      pb[0] = -pb[0];
    }
  }
}
