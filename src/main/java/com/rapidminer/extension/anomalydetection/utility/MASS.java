package com.rapidminer.extension.anomalydetection.utility;

import static java.lang.Math.sqrt;
import static org.bytedeco.fftw.global.fftw3.fftw_cleanup;
import static org.bytedeco.fftw.global.fftw3.fftw_cleanup_threads;
import static org.bytedeco.fftw.global.fftw3.fftw_destroy_plan;
import static org.bytedeco.fftw.global.fftw3.fftw_execute;
import static org.bytedeco.fftw.global.fftw3.fftw_free;
import static org.bytedeco.fftw.global.fftw3.fftw_malloc;
import static org.bytedeco.fftw.global.fftw3.fftw_plan_dft_1d;
import static org.bytedeco.fftw.presets.fftw3.free;


import org.apache.commons.math.complex.Complex;

import org.apache.commons.math.transform.FastFourierTransformer;
import org.bytedeco.fftw.global.fftw3;
import org.bytedeco.javacpp.DoublePointer;


import com.rapidminer.tools.math.FastFourierTransform;


/**
 * This implements MASS: Mueen's Algorithm for Similarity Search as given in
 * https://www.cs.unm.edu/~mueen/FastestSimilaritySearch.html
 * It's literally a translation of the C++ code given here: https://www.cs.unm.edu/~mueen/MASS.cpp
 */
public class MASS {

	public double[] findNN(double x[], double[] y, int n, int m) {
		double[] cx = new double[n + 1];
		double[] cx2 = new double[n + 1];
		double[] cy = new double[m + 1];
		double[] cy2 = new double[m + 1];

		x = zNorm(x, x.length);
		y = zNorm(y,y.length);

		cx[0] = cx2[0] = cy[0] = cy2[0] = 0.0;
		for (int i = 1; i <= n; i++) {
			cx[i] = cx[i - 1] + x[i - 1];
			cx2[i] = cx2[i - 1] + x[i - 1] * x[i - 1];
			if (i <= m) {
				cy[i] = cy[i - 1] + y[i - 1];
				cy2[i] = cy2[i - 1] + y[i - 1] * y[i - 1];
			}

		}


		double[] z = multiply(x, n, y, m);

		//y Stats

		double sumy = cy[m];
		double sumy2 = cy2[m];
		double meany = sumy / m;
		double sigmay = (sumy2 / m) - meany * meany;
		sigmay = sqrt(sigmay);

		double[] dist = new double[n - m + 1];
		//The Search
		for (int j = 0; j < n - m + 1; j = j + 1) {
			double sumxy = z[m - 1 + j];

			double sumx = cx[j + m] - cx[j];
			double sumx2 = cx2[j + m] - cx2[j];
			double meanx = sumx / m;
			double sigmax = (sumx2 / m) - meanx * meanx;
			sigmax = sqrt(sigmax);

			double c = (sumxy - m * meanx * meany) / (m * sigmax * sigmay);
			dist[j] = sqrt(2 * m * (1 - c));

		}

		return dist;
	}

	double[] multiply(double[] x, int n, double[] y, int m) {
		fftw3.fftw_make_planner_thread_safe();

//		fftw_complex * X, * Y, * Z , *XX, *YY, *ZZ;
//		fftw_plan p;

		//assuming n > m
		double[] z = new double[2 * n];
		DoublePointer X = fftw3.fftw_alloc_complex(2 * n); //(fftw_complex*) fftw_malloc(sizeof(fftw_complex) * 2 * n);
		DoublePointer Y = fftw3.fftw_alloc_complex(2 * n);
		DoublePointer XX = fftw3.fftw_alloc_complex(2 * n);
		DoublePointer YY = fftw3.fftw_alloc_complex(2 * n);
		DoublePointer Z = fftw3.fftw_alloc_complex(2 * n);
		DoublePointer ZZ = fftw3.fftw_alloc_complex(2 * n);


		for (int i = 0; i < 2 * n; i++) {
			X.put(i + n, 0);
			Y.put(i + n, 0);//iaginary part is always zero

			if (i < n) {
				X.put(i, x[i]);

			} else {
				X.put(i, 0);
			}

			if (i < m) {
				Y.put(i, y[m - i - 1]); //reversing y
			} else {
				Y.put(i, 0);
			}

		}


		fftw3.fftw_plan p = fftw_plan_dft_1d(2 * n, X, XX, fftw3.FFTW_FORWARD, fftw3.FFTW_ESTIMATE);
		fftw_execute(p);

		p = fftw_plan_dft_1d(2 * n, Y, YY, fftw3.FFTW_FORWARD, fftw3.FFTW_ESTIMATE);
		fftw_execute(p);

		for (int i = 0; i < 2 * n; i++) {
			int i_plus_n = i + n;
			//ZZ[i][0] = XX[i][0] * YY[i][0] - XX[i][1] * YY[i][1];
			ZZ.put(i, XX.get(i) * YY.get(i) - XX.get(i_plus_n) * YY.get(i_plus_n));

			//ZZ[i][1] = XX[i][1] * YY[i][0] + XX[i][0] * YY[i][1];
			ZZ.put(i_plus_n, XX.get(i_plus_n) * YY.get(i) + XX.get(i) * YY.get(i_plus_n));
		}

		p = fftw_plan_dft_1d(2 * n, ZZ, Z, fftw3.FFTW_BACKWARD, fftw3.FFTW_ESTIMATE);
		fftw_execute(p);
		p.deallocate();

				for (int i = 0; i < 2 * n; i++) {
			z[i] = Z.get(i) / (2 * n);
		}


		fftw_free(X);
		fftw_free(Y);
		fftw_free(XX);
		fftw_free(YY);
		fftw_free(Z);
		fftw_free(ZZ);
		fftw_malloc(X.address());
		fftw_malloc(Y.address());
		fftw_malloc(XX.address());
		fftw_malloc(YY.address());
		fftw_malloc(Z.address());
		fftw_malloc(ZZ.address());
		fftw_cleanup_threads();
		fftw_cleanup();
		fftw_cleanup_threads();
		X.deallocate();
		Y.deallocate();
		XX.deallocate();
		YY.deallocate();
		Z.deallocate();
		ZZ.deallocate();


		return z;
	}

	private static double[] zNorm(double[] x, int n) {

		double[] y = new double[x.length];
		double ex = 0, ex2 = 0;
		for (int i = 0; i < n; i++) {
			ex += x[i];
			ex2 += x[i] * x[i];
		}
		double mean = ex / n;
		double std = ex2 / n;
		std = sqrt(std - mean * mean);
		for (int i = 0; i < n; i++) {
			y[i] = (x[i] - mean) / std;
		}
		return y;

	}
}
